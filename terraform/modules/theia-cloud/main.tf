locals {
  # base_keycloak: use provided URL or build from hostname
  base_keycloak = var.keycloak_url != "" ? var.keycloak_url : "https://${var.hostname}/keycloak"
  # normalized_keycloak_url: ensure a single trailing slash as required by the Theia Cloud Helm chart.
  normalized_keycloak_url = endswith(local.base_keycloak, "/") ? local.base_keycloak : "${local.base_keycloak}/"
}

resource "helm_release" "theia-cloud-base" {
  count            = var.install_theia_cloud_base ? 1 : 0
  name             = "theia-cloud-base"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud-base"
  version          = "1.1.2"
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
  version          = "1.1.2"
  namespace        = "theia-cloud"
  create_namespace = true
}

resource "helm_release" "theia-cloud" {
  count            = var.install_theia_cloud ? 1 : 0
  depends_on       = [helm_release.theia-cloud-crds]
  name             = "theia-cloud"
  repository       = "https://eclipse-theia.github.io/theia-cloud-helm"
  chart            = "theia-cloud"
  version          = "1.1.3"
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
      value = local.normalized_keycloak_url
    },
    {
      name  = "operator.cloudProvider"
      value = var.cloudProvider
    }
  ]
}
