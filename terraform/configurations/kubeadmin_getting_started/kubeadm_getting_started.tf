variable "kubernetes_version" {
  description = "Kubernetes version to use"
  default     = "v1.26.3"
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
  default     = "admin"
}

variable "hostname" {
  description = "Hostname"
}

module "cluster" {
  source = "../../modules/cluster_creation/kubeadm/"
  # Please specific the cluster host address (i.e. api server address)
  cluster_host = ""
}

provider "kubernetes" {
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

provider "kubectl" {
  load_config_file       = false
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

provider "helm" {
  kubernetes {
    host                   = module.cluster.cluster_host
    client_certificate     = module.cluster.cluster_client_certificate
    client_key             = module.cluster.cluster_client_key
    cluster_ca_certificate = module.cluster.cluster_ca_certificate
  }
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
  cert_manager_cluster_issuer  = "theia-cloud-selfsigned-issuer"
  cert_manager_common_name     = var.hostname
  hostname                     = var.hostname
  keycloak_admin_password      = var.keycloak_admin_password
  postgresql_enabled           = true
  postgres_postgres_password   = "admin"
  postgres_password            = "admin"
  # Change it to your storage class
  postgresql_storageClass      = "longhorn"
  postgresql_volumePermissions = true
  service_type                 = "ClusterIP"
  cloudProvider                = "K8S"
}

provider "keycloak" {
  client_id = "admin-cli"
  username  = "admin"
  password  = var.keycloak_admin_password
  url       = "https://${var.hostname}/keycloak"
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.helm]

  hostname                        = var.hostname
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "https://${var.hostname}/*"
}
