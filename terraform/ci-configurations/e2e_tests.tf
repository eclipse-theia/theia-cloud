variable "ingress_ip" {
  description = "The IP of the running Minikube Cluster"
  type        = string
}

variable "use_paths" {
  description = "Whether to use Theia Cloud with paths or subdomains"
  type        = bool
}

variable "use_ephemeral_storage" {
  description = "Whether to use ephemeral storage"
  type        = bool
}

variable "enable_keycloak" {
  description = "Whether to enable keycloak"
  type        = bool
}

variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "nginx"
}

variable "eager_start" {
  description = "Whether to enable eager start for sessions"
  type        = bool
  default     = false
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes = {
    config_path = "~/.kube/config"
  }
}

resource "kubernetes_persistent_volume_v1" "minikube" {
  metadata {
    name = "minikube-volume"
  }
  spec {
    storage_class_name = "manual"
    capacity = {
      storage = "16Gi"
    }
    access_modes = ["ReadWriteOnce"]
    persistent_volume_source {
      host_path {
        path = "/data/theiacloud"
      }
    }
  }
}

module "helm" {
  source = "../modules/helm"

  depends_on = [kubernetes_persistent_volume_v1.minikube]

  install_ingress_controller   = false
  ingress_controller_type      = var.ingress_controller_type
  install_theia_cloud_base     = false
  install_theia_cloud_crds     = false
  install_theia_cloud          = false
  install_selfsigned_issuer    = true
  cert_manager_issuer_email    = "jdoe@theia-cloud.io"
  cert_manager_cluster_issuer  = "keycloak-selfsigned-issuer"
  cert_manager_common_name     = "${var.ingress_ip}.nip.io"
  hostname                     = "${var.ingress_ip}.nip.io"
  service_type                 = "ClusterIP"
  postgresql_storageClass      = "manual"
  postgresql_volumePermissions = true
  keycloak_admin_password      = "admin"
  postgresql_enabled           = true
  postgres_postgres_password   = "admin"
  postgres_password            = "admin"
  loadBalancerIP               = ""
  cloudProvider                = "MINIKUBE"
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = "admin"
  url                      = "https://${var.ingress_ip}.nip.io/keycloak"
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../modules/keycloak"

  depends_on = [module.helm]

  hostname                        = "${var.ingress_ip}.nip.io"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "*"
}

resource "helm_release" "theia-cloud-crds" {
  depends_on = [module.keycloak]

  name             = "theia-cloud-crds"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud-crds"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "conversion.image"
      value = "theiacloud/theia-cloud-conversion-webhook:minikube-ci-e2e"
    }
  ]
}

resource "helm_release" "theia-cloud-base" {
  depends_on = [module.keycloak]

  name             = "theia-cloud-base"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud-base"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "issuer.email"
      value = "jdoe@theia-cloud.io"
    }
  ]
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-crds, helm_release.theia-cloud-base]

  name             = "theia-cloud"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    file("${path.module}/../values/valuesE2ECI-base.yaml"),
    file("${path.module}/../values/valuesE2ECI-minikube.yaml"),
  ]

  set = [{
    name  = "hosts.usePaths"
    value = var.use_paths
    },
    {
      name  = "hosts.configuration.baseHost"
      value = "${var.ingress_ip}.nip.io"
    },
    {
      name  = "landingPage.ephemeralStorage"
      value = var.use_ephemeral_storage
    },
    {
      name  = "keycloak.enable"
      value = var.enable_keycloak
    },
    {
      name  = "keycloak.authUrl"
      value = "https://${var.ingress_ip}.nip.io/keycloak/"
    },
    {
      name  = "ingress.controller"
      value = var.ingress_controller_type
    },
    {
      name  = "operator.eagerStart"
      value = var.eager_start
    }
  ]
}

module "appdefinitions" {
  source = "../modules/theia-cloud-ci-appdefinitions"

  depends_on = [helm_release.theia-cloud]

  eager_start = var.eager_start
}

