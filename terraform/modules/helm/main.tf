variable "install_ingress_controller" {
  description = "Whether to install the nginx ingress controller"
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}

variable "cert_manager_cluster_issuer" {
  type = string

  validation {
    condition     = length(regexall("^(letsencrypt-prod|theia-cloud-selfsigned-issuer)$", var.cert_manager_cluster_issuer)) > 0
    error_message = "ERROR: Valid types are \"letsencrypt-prod\" and \"theia-cloud-selfsigned-issuer\"!"
  }
}


variable "cert_manager_common_name" {
  description = "The common name for the certificate"
  default     = ""
}

variable "hostname" {
  description = "The hostname for all installed services"
}

variable "service_type" {
  description = "Kubernetes service type"
  default     = "LoadBalancer"

}

variable "postgresql_storageClass" {
  description = "StorageClass for Persistent Volume(s)"
  default     = ""
}

variable "postgresql_volumePermissions" {
  description = "Enable init container that changes the owner and group of the persistent volume"
  default     = false
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
}

variable "postgresql_enabled" {
  description = "Whether to enable postgreswl"
  default     = true
}

variable "postgres_postgres_password" {
  description = "Keycloak Postgres DB Postgres (Admin) Password"
  sensitive   = true
}

variable "postgres_password" {
  description = "Keycloak Postgres DB Password"
  sensitive   = true
}

variable "loadBalancerIP" {
  description = "External IP for the nginx ingress controller"
  default     = ""
}

# variable "backend_bucket_name" {
#   description = "The bucket name for the remote state storage"
# }

resource "helm_release" "cert-manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.11.0"
  namespace        = "cert-manager"
  create_namespace = true

  set {
    name  = "installCRDs"
    value = "true"
  }
}

resource "helm_release" "nginx-ingress-controller" {
  count            = var.install_ingress_controller ? 1 : 0
  name             = "nginx-ingress-controller"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = "4.5.2"
  namespace        = "ingress-nginx"
  create_namespace = true

  set {
    name  = "fullnameOverride"
    value = "ingress-nginx"
  }

  set {
    name  = "controller.service.loadBalancerIP"
    value = var.loadBalancerIP
  }
}

resource "helm_release" "theia-cloud-base" {
  depends_on       = [helm_release.cert-manager, helm_release.nginx-ingress-controller] # we need to install cert issuers
  name             = "theia-cloud-base"
  repository       = "https://github.eclipsesource.com/theia-cloud-helm"
  chart            = "theia-cloud-base"
  version          = "0.8.0-MS9v1"
  namespace        = "theiacloud"
  create_namespace = true

  set {
    name  = "issuer.email"
    value = var.cert_manager_issuer_email
  }
}

resource "helm_release" "keycloak" {
  depends_on       = [helm_release.theia-cloud-base] # we use the cert issuer from theia cloud base
  name             = "keycloak"
  repository       = "https://charts.bitnami.com/bitnami"
  chart            = "keycloak"
  version          = "13.3.0"
  namespace        = "keycloak"
  create_namespace = true

  values = [
    "${templatefile("${path.module}/keycloak.yaml", { cluster-issuer = var.cert_manager_cluster_issuer, common-name = var.cert_manager_common_name })}"
  ]

  set {
    name  = "postgresql.enabled"
    value = var.postgresql_enabled
  }
  set {
    name  = "ingress.hostname"
    value = var.hostname
  }
  set {
    name  = "global.storageClass"
    value = var.postgresql_storageClass
  }
  set {
    name  = "service.type"
    value = var.service_type
  }
  set {
    name  = "postgresql.volumePermissions.enabled"
    value = var.postgresql_volumePermissions
  }
  set_sensitive {
    name  = "auth.adminPassword"
    value = var.keycloak_admin_password
  }
  set_sensitive {
    name  = "postgresql.auth.postgresPassword"
    value = var.postgres_postgres_password
  }
  set_sensitive {
    name  = "postgresql.auth.password"
    value = var.postgres_password
  }

  # We expect that kubectl context was configured by a previous module.
  # After keycloak was set up with tls enabled, we use the created tls secret as the default ssl-secret of the nginx-ingress-controller. 
  # Below command connects to the cluster in the local environment and patches the ingress-controller accordingly. 
  # Theia Cloud is then installed with path based hosts reusing the same certificate. 
  provisioner "local-exec" {
    command = "kubectl patch deploy ingress-nginx-controller --type='json' -n ingress-nginx -p '[{ \"op\": \"add\", \"path\": \"/spec/template/spec/containers/0/args/-\", \"value\": \"--default-ssl-certificate=keycloak/${var.hostname}-tls\" }]' && kubectl wait pods -n ingress-nginx -l app.kubernetes.io/component=controller --for condition=Ready --timeout=90s && kubectl wait certificate -n keycloak ${var.hostname}-tls --for condition=Ready --timeout=90s"
  }
}

resource "helm_release" "theia-cloud" {
  depends_on       = [helm_release.keycloak] # wait for keycloak to make the default cert available
  name             = "theia-cloud"
  repository       = "https://github.eclipsesource.com/theia-cloud-helm"
  chart            = "theia-cloud"
  version          = "0.8.0-MS9v1"
  namespace        = "theiacloud"
  create_namespace = true

  values = [
    "${file("${path.module}/theia-cloud.yaml")}"
  ]

  set {
    name  = "hosts.paths.baseHost"
    value = var.hostname
  }

  set {
    name  = "keycloak.authUrl"
    value = "https://${var.hostname}/keycloak/"
  }
}
