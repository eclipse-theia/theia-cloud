variable "install_ingress_controller" {
  description = "Whether to install the nginx ingress controller"
}

variable "install_theia_cloud_base" {
  description = "Whether to install theia cloud base"
  default     = true
}

variable "install_theia_cloud_crds" {
  description = "Whether to install theia cloud crds"
  default     = true
}

variable "install_theia_cloud" {
  description = "Whether to install theia cloud"
  default     = true
}

variable "install_selfsigned_issuer" {
  description = "Whether to install an additional self signed issuer"
  default     = false
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}

variable "cert_manager_cluster_issuer" {
  type = string

  validation {
    condition     = length(regexall("^(letsencrypt-prod|theia-cloud-selfsigned-issuer|keycloak-selfsigned-issuer)$", var.cert_manager_cluster_issuer)) > 0
    error_message = "ERROR: Valid values are \"letsencrypt-prod\", \"theia-cloud-selfsigned-issuer\", and \"keycloak-selfsigned-issuer\"!"
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

variable "cloudProvider" {
  description = "The cloud provider to use"
  default     = "K8S"
}

resource "helm_release" "cert-manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.16.2"
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
  version          = "4.11.5"
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

  set {
    name  = "controller.allowSnippetAnnotations"
    value = true
  }
}

resource "helm_release" "theia-cloud-base" {
  count            = var.install_theia_cloud_base ? 1 : 0
  depends_on       = [helm_release.cert-manager, helm_release.nginx-ingress-controller] # we need to install cert issuers
  name             = "theia-cloud-base"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud-base"
  version          = "1.0.0"
  namespace        = "theia-cloud"
  create_namespace = true

  set {
    name  = "issuer.email"
    value = var.cert_manager_issuer_email
  }
}

resource "helm_release" "theia-cloud-crds" {
  count            = var.install_theia_cloud_crds ? 1 : 0
  depends_on       = [helm_release.theia-cloud-base]
  name             = "theia-cloud-crds"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud-crds"
  version          = "1.0.0"
  namespace        = "theia-cloud"
  create_namespace = true
}

resource "kubectl_manifest" "selfsigned_issuer" {
  count      = var.install_selfsigned_issuer ? 1 : 0
  depends_on = [helm_release.cert-manager, helm_release.nginx-ingress-controller] # we need to install cert issuers
  yaml_body  = file("${path.module}/clusterissuer-selfsigned.yaml")
}

locals {
  # local_exec_quotes is a helper function to deal with different handling of
  # quotes between linux and windows. On linux, it will output "'". On windows,
  # it will output "".
  local_exec_quotes = startswith(abspath(path.module), "/") ? "'" : ""
  jsonpatch = jsonencode([{
    "op"    = "add",
    "path"  = "/spec/template/spec/containers/0/args/-",
    "value" = "--default-ssl-certificate=keycloak/${var.hostname}-tls"
  }])
}

resource "helm_release" "keycloak" {
  depends_on       = [helm_release.theia-cloud-base, kubectl_manifest.selfsigned_issuer] # we need an existing issuer
  name             = "keycloak"
  repository       = "https://charts.bitnami.com/bitnami"
  chart            = "keycloak"
  version          = "15.1.8"
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
    command = "kubectl patch deploy ingress-nginx-controller --type=${local.local_exec_quotes}json${local.local_exec_quotes} -n ingress-nginx -p ${local.local_exec_quotes}${local.jsonpatch}${local.local_exec_quotes} && kubectl wait pods -n ingress-nginx -l app.kubernetes.io/component=controller --for condition=Ready --timeout=90s && kubectl wait certificate -n keycloak ${var.hostname}-tls --for condition=Ready --timeout=90s"
  }
}

resource "helm_release" "theia-cloud" {
  count            = var.install_theia_cloud ? 1 : 0
  depends_on       = [helm_release.keycloak, helm_release.theia-cloud-crds] # wait for keycloak to make the default cert available
  name             = "theia-cloud"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud"
  version          = "1.0.0"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/theia-cloud.yaml")}"
  ]

  set {
    name  = "hosts.configuration.baseHost"
    value = var.hostname
  }

  set {
    name  = "keycloak.authUrl"
    value = "https://${var.hostname}/keycloak/"
  }

  set {
    name  = "operator.cloudProvider"
    value = var.cloudProvider
  }
}
