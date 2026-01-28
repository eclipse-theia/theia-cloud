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

variable "postgres_password" {
  description = "Keycloak Postgres DB Password"
  sensitive   = true
}

variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "haproxy"
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
  name       = "theia-cloud-ingress-ip"
}

provider "kubernetes" {
  host                   = module.cluster.cluster_host
  token                  = module.cluster.cluster_token
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
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

module "cluster_prerequisites" {
  source = "../../modules/cluster-prerequisites"

  hostname                            = "${google_compute_address.host_ip.address}.sslip.io"
  keycloak_admin_password             = var.keycloak_admin_password
  postgres_password                   = var.postgres_password
  install_cert_manager                = true
  install_ingress_controller          = true
  install_selfsigned_issuer           = false
  cert_manager_issuer_email           = var.cert_manager_issuer_email
  ingress_cert_manager_cluster_issuer = "letsencrypt-prod"
  load_balancer_ip                    = google_compute_address.host_ip.address
}

module "helm" {
  source = "../../modules/helm"

  depends_on = [module.cluster_prerequisites]
  ingress_controller_type     = var.ingress_controller_type
  cert_manager_issuer_email = var.cert_manager_issuer_email
  cert_manager_cluster_issuer = "letsencrypt-prod"
  cert_manager_common_name    = "${google_compute_address.host_ip.address}.sslip.io"
  hostname                  = "${google_compute_address.host_ip.address}.sslip.io"
  loadBalancerIP            = google_compute_address.host_ip.address
}

provider "keycloak" {
  client_id      = "admin-cli"
  username       = "admin"
  password       = var.keycloak_admin_password
  url            = module.cluster_prerequisites.keycloak_url
  initial_login  = false
  client_timeout = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.cluster_prerequisites]

  hostname                        = "${google_compute_address.host_ip.address}.sslip.io"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "https://${google_compute_address.host_ip.address}.sslip.io/*"
}
