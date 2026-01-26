variable "kubernetes_version" {
  description = "Kubernetes version to use"
  default     = "v1.33.6"
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

module "cluster_prerequisites" {
  source = "../../modules/cluster-prerequisites"

  depends_on = [kubernetes_persistent_volume.minikube]

  hostname                            = "${module.host.host}.nip.io"
  keycloak_admin_password             = var.keycloak_admin_password
  postgres_password                   = "admin"
  install_cert_manager                = true
  install_selfsigned_issuer           = true
  cert_manager_issuer_email           = var.cert_manager_issuer_email
  ingress_cert_manager_cluster_issuer = "keycloak-selfsigned-issuer"
  ingress_cert_manager_common_name    = "${module.host.host}.nip.io"
  postgres_storage_class              = "manual"
  postgres_volume_permissions         = true
  cloud_provider                      = "MINIKUBE"
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = var.keycloak_admin_password
  url                      = module.cluster_prerequisites.keycloak_url
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.cluster_prerequisites]

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
