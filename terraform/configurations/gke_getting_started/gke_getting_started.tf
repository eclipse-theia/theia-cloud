variable "project_id" {
  description = "The GCE project id"
}

variable "location" {
  description = "The zone of the created cluster"
  default     = "europe-west3-c"
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
}

variable "postgres_postgres_password" {
  description = "Keycloak Postgres DB Postgres (Admin) Password"
  sensitive   = true
}

variable "postgres_password" {
  description = "Keycloak Postgres DB Password"
  sensitive   = true
}

provider "google" {
  project = var.project_id
  zone    = var.location
}

module "cluster" {
  source = "../../modules/cluster_creation/gke/"

  # adjust values below
  project_id = var.project_id
  location   = var.location
}

resource "google_compute_address" "host_ip" {
  depends_on = [module.cluster]
  name       = "theia-cloud-nginx-ip"
}

provider "helm" {
  kubernetes = {
    host                   = module.cluster.cluster_host
    token                  = module.cluster.cluster_token
    cluster_ca_certificate = module.cluster.cluster_ca_certificate
  }
}

provider "kubectl" {
  load_config_file       = false
  host                   = module.cluster.cluster_host
  token                  = module.cluster.cluster_token
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

module "helm" {
  source = "../../modules/helm"

  install_ingress_controller  = true
  cert_manager_issuer_email   = var.cert_manager_issuer_email
  cert_manager_cluster_issuer = "letsencrypt-prod"
  cert_manager_common_name    = "${google_compute_address.host_ip.address}.sslip.io"
  hostname                    = "${google_compute_address.host_ip.address}.sslip.io"
  keycloak_admin_password     = var.keycloak_admin_password
  postgresql_enabled          = true
  postgres_postgres_password  = var.postgres_postgres_password
  postgres_password           = var.postgres_password
  loadBalancerIP              = google_compute_address.host_ip.address
}

provider "keycloak" {
  client_id      = "admin-cli"
  username       = "admin"
  password       = var.keycloak_admin_password
  url            = "https://${google_compute_address.host_ip.address}.sslip.io/keycloak"
  initial_login  = false
  client_timeout = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.helm]

  hostname                        = "${google_compute_address.host_ip.address}.sslip.io"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "https://${google_compute_address.host_ip.address}.sslip.io/*"
}
