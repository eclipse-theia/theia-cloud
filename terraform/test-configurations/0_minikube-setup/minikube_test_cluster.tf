variable "kubernetes_version" {
  description = "Kubernetes version to use"
  default     = "v1.26.3"
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
  default     = "tester@theia-cloud.io"
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
  default     = "admin"
}

provider "minikube" {
  kubernetes_version = var.kubernetes_version
}

module "cluster" {
  source = "../../modules/cluster_creation/minikube/"

  # adjust values below
  cluster_name = "minikube"
  cpus         = 4
  disk_size    = "51200mb"
  memory       = "8192mb"
  driver       = "kvm2"
}

provider "kubernetes" {
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

resource "kubernetes_persistent_volume" "minikube" {

  depends_on = [module.cluster]

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
    host                   = module.cluster.cluster_host
    client_certificate     = module.cluster.cluster_client_certificate
    client_key             = module.cluster.cluster_client_key
    cluster_ca_certificate = module.cluster.cluster_ca_certificate
  }
}

provider "kubectl" {
  load_config_file       = false
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

module "host" {
  depends_on = [module.cluster]

  source = "matti/urlparse/external"
  url    = module.cluster.cluster_host
}

module "helm" {
  source = "../../modules/helm"

  depends_on = [module.host]

  install_ingress_controller   = false
  cert_manager_issuer_email    = var.cert_manager_issuer_email
  cert_manager_cluster_issuer  = "keycloak-selfsigned-issuer"
  cert_manager_common_name     = "${module.host.host}.nip.io"
  hostname                     = "${module.host.host}.nip.io"
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
  url                      = "https://${module.host.host}.nip.io/keycloak"
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.helm]

  hostname                        = "${module.host.host}.nip.io"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "*"
}

# Configure user foo as admin by adding it to the admin group
resource "keycloak_group_memberships" "admin_group_memberships" {
  realm_id = module.keycloak.realm.id
  group_id = module.keycloak.admin_group.id
  members = [
    module.keycloak.test_users.foo.username
  ]
}
