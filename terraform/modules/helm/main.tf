variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "nginx"

  validation {
    condition     = contains(["nginx", "haproxy"], var.ingress_controller_type)
    error_message = "Valid values are 'nginx' or 'haproxy'."
  }
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

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}

variable "hostname" {
  description = "The hostname for all installed services"
}

variable "cloudProvider" {
  description = "The cloud provider to use"
  default     = "K8S"
}

# Note: cert-manager and nginx-ingress must be installed via cluster-prerequisites module first
  count            = var.install_ingress_controller && var.ingress_controller_type == "nginx" ? 1 : 0
resource "helm_release" "haproxy-ingress-controller" {
  count            = var.install_ingress_controller && var.ingress_controller_type == "haproxy" ? 1 : 0
  name             = "haproxy-ingress"
  repository       = "https://haproxy-ingress.github.io/charts"
  chart            = "haproxy-ingress"
  version          = "0.15.1"
  namespace        = "ingress-haproxy"
  create_namespace = true

  set = [
    {
      name  = "controller.ingressClassResource.enabled"
      value = true
    },
    {
      name  = "controller.service.loadBalancerIP"
      value = var.loadBalancerIP
    }
  ]
}

resource "helm_release" "theia-cloud-base" {
  count            = var.install_theia_cloud_base ? 1 : 0
  depends_on       = [helm_release.cert-manager, helm_release.nginx-ingress-controller, helm_release.haproxy-ingress-controller] # we need to install cert issuers
  name             = "theia-cloud-base"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud-base"
  version          = "1.2.0"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "issuer.email"
      value = var.cert_manager_issuer_email
    }
  ]
}

resource "helm_release" "theia-cloud-crds" {
  count            = var.install_theia_cloud_crds ? 1 : 0
  depends_on       = [helm_release.theia-cloud-base]
  name             = "theia-cloud-crds"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud-crds"
  version          = "1.2.0"
  namespace        = "theia-cloud"
  create_namespace = true
}

resource "kubectl_manifest" "selfsigned_issuer" {
  count      = var.install_selfsigned_issuer ? 1 : 0
  depends_on = [helm_release.cert-manager, helm_release.nginx-ingress-controller, helm_release.haproxy-ingress-controller] # we need to install cert issuers
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

# TODO remove keycloak here
resource "helm_release" "keycloak" {
  depends_on       = [helm_release.theia-cloud-base, kubectl_manifest.selfsigned_issuer, helm_release.nginx-ingress-controller, helm_release.haproxy-ingress-controller] # we need an existing issuer
  name             = "keycloak"
  repository       = "https://charts.bitnami.com/bitnami"
  chart            = "keycloak"
  version          = "15.1.8"
  namespace        = "keycloak"
  create_namespace = true

  values = [
    "${templatefile("${path.module}/keycloak.yaml", { cluster-issuer = var.cert_manager_cluster_issuer, common-name = var.cert_manager_common_name, ingress-class = var.ingress_controller_type == "haproxy" ? "haproxy" : "nginx" })}"
  ]

  set = [
    {
      name  = "postgresql.enabled"
      value = var.postgresql_enabled
    },
    {
      name  = "ingress.hostname"
      value = var.hostname
    },
    {
      name  = "global.storageClass"
      value = var.postgresql_storageClass
    },
    {
      name  = "service.type"
      value = var.service_type
    },
    {
      name  = "postgresql.volumePermissions.enabled"
      value = var.postgresql_volumePermissions
    }
  ]
  set_sensitive = [
    {
      name  = "auth.adminPassword"
      value = var.keycloak_admin_password
    },
    {
      name  = "postgresql.auth.postgresPassword"
      value = var.postgres_postgres_password
    },
    {
      name  = "postgresql.auth.password"
      value = var.postgres_password
    }
  ]

  # We expect that kubectl context was configured by a previous module.
  # After keycloak was set up with tls enabled, we use the created tls secret as the default ssl-secret of the ingress-controller.
  # Below command connects to the cluster in the local environment and patches the ingress-controller accordingly.
  # Theia Cloud is then installed with path based hosts reusing the same certificate.
  # Sleep 5 seconds at the end as there might be a brief delay between the ingress controller reporting available and it actually being ready to serve traffic
  provisioner "local-exec" {
    command = var.ingress_controller_type == "nginx" ? "kubectl patch deploy ingress-nginx-controller --type=${local.local_exec_quotes}json${local.local_exec_quotes} -n ingress-nginx -p ${local.local_exec_quotes}${local.jsonpatch}${local.local_exec_quotes} && kubectl -n ingress-nginx wait --for condition=available deploy/ingress-nginx-controller --timeout=90s && kubectl wait certificate -n keycloak ${var.hostname}-tls --for condition=Ready --timeout=90s && sleep 5" : "kubectl patch deploy haproxy-ingress --type=${local.local_exec_quotes}json${local.local_exec_quotes} -n ingress-haproxy -p ${local.local_exec_quotes}${local.jsonpatch}${local.local_exec_quotes} && kubectl -n ingress-haproxy wait --for condition=available deploy/haproxy-ingress --timeout=90s && kubectl wait certificate -n keycloak ${var.hostname}-tls --for condition=Ready --timeout=90s && sleep 5"
  }
}

resource "helm_release" "theia-cloud" {
  count            = var.install_theia_cloud ? 1 : 0
  depends_on       = [helm_release.theia-cloud-crds]
  name             = "theia-cloud"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud"
  version          = "1.2.0"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/theia-cloud.yaml")}"
  ]

  set = [
    {
      name  = "hosts.configuration.baseHost"
      value = var.hostname
    },
    {
      name  = "keycloak.authUrl"
      value = "https://${var.hostname}/keycloak/"
    },
    {
      name  = "operator.cloudProvider"
      value = var.cloudProvider
    },
    {
      name  = "ingress.controller"
      value = var.ingress_controller_type
    }
  ]
}
