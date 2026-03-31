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
