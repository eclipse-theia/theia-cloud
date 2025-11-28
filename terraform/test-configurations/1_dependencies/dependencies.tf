variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
  default     = "tester@theia-cloud.io"
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
  default     = "admin"
}

data "terraform_remote_state" "minikube" {
  backend = "local"

  config = {
    path = "${path.module}/../0_minikube-setup/terraform.tfstate"
  }
}

provider "kubernetes" {
  host                   = data.terraform_remote_state.minikube.outputs.host
  client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
  client_key             = data.terraform_remote_state.minikube.outputs.client_key
  cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
}

resource "kubernetes_persistent_volume" "minikube" {
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
        path = "/data/theia-cloud"
      }
    }
  }
}

provider "helm" {
  kubernetes = {
    host                   = data.terraform_remote_state.minikube.outputs.host
    client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
    client_key             = data.terraform_remote_state.minikube.outputs.client_key
    cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
  }
}

provider "kubectl" {
  load_config_file       = false
  host                   = data.terraform_remote_state.minikube.outputs.host
  client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
  client_key             = data.terraform_remote_state.minikube.outputs.client_key
  cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
}

module "host" {
  source = "matti/urlparse/external"
  url    = data.terraform_remote_state.minikube.outputs.host
}

resource "helm_release" "haproxy-ingress-controller" {
  count            = data.terraform_remote_state.minikube.outputs.ingress_controller_type == "haproxy" ? 1 : 0
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
    }
  ]
}

data "kubernetes_service" "haproxy_ingress" {
  count = data.terraform_remote_state.minikube.outputs.ingress_controller_type == "haproxy" ? 1 : 0

  depends_on = [helm_release.haproxy-ingress-controller]

  metadata {
    name      = "haproxy-ingress"
    namespace = "ingress-haproxy"
  }
}

locals {
  effective_host = data.terraform_remote_state.minikube.outputs.ingress_controller_type == "haproxy" ? data.kubernetes_service.haproxy_ingress[0].status[0].load_balancer[0].ingress[0].ip : module.host.host
  hostname       = "${local.effective_host}.nip.io"
}

module "helm" {
  source = "../../modules/helm"

  depends_on = [module.host, helm_release.haproxy-ingress-controller]

  install_ingress_controller   = false
  ingress_controller_type      = data.terraform_remote_state.minikube.outputs.ingress_controller_type
  cert_manager_issuer_email    = var.cert_manager_issuer_email
  cert_manager_cluster_issuer  = "keycloak-selfsigned-issuer"
  cert_manager_common_name     = local.hostname
  hostname                     = local.hostname
  keycloak_admin_password      = var.keycloak_admin_password
  postgresql_enabled           = true
  postgres_postgres_password   = "admin"
  postgres_password            = "admin"
  postgresql_storageClass      = "manual"
  postgresql_volumePermissions = true
  service_type                 = "ClusterIP"
  cloudProvider                = "MINIKUBE"
  install_selfsigned_issuer    = true
  install_theia_cloud_base     = false
  install_theia_cloud_crds     = false
  install_theia_cloud          = false
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = var.keycloak_admin_password
  url                      = "https://${local.hostname}/keycloak"
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.helm]

  hostname                        = local.hostname
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "*"
}

resource "keycloak_group_memberships" "admin_group_memberships" {
  realm_id = module.keycloak.realm.id
  group_id = module.keycloak.admin_group.id
  members = [
    module.keycloak.test_users.foo.username
  ]
}
