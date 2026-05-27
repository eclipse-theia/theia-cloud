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

# Manually install cert-manager to have its CRDs available for the theia-cloud-base module, which installs the cluster issuer for let's encrypt.
resource "helm_release" "cert_manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.17.4"
  namespace        = "cert-manager"
  create_namespace = true

  set = [
    {
      name  = "installCRDs"
      value = "true"
    }
  ]
}

# Install theia-cloud-base first because we re-use the "letsencrypt-prod" ClusterIssuer created by that module for the Keycloak ingress installed in the cluster prerequisites module.
module "theia-cloud-base" {
  source = "../../modules/theia-cloud"

  depends_on = [helm_release.cert_manager]

  install_theia_cloud_crds  = false
  install_theia_cloud       = false
  hostname                  = "${google_compute_address.host_ip.address}.sslip.io"
  cert_manager_issuer_email = var.cert_manager_issuer_email
}

module "cluster_prerequisites" {
  source = "../../modules/cluster-prerequisites"

  depends_on                          = [module.theia-cloud-base]
  hostname                            = "${google_compute_address.host_ip.address}.sslip.io"
  keycloak_admin_password             = var.keycloak_admin_password
  postgres_password                   = var.postgres_password
  install_cert_manager                = false
  install_ingress_controller          = true
  install_selfsigned_issuer           = false
  cert_manager_issuer_email           = var.cert_manager_issuer_email
  ingress_controller_type             = var.ingress_controller_type
  ingress_class_name                  = var.ingress_controller_type
  ingress_cert_manager_cluster_issuer = "letsencrypt-prod"
  load_balancer_ip                    = google_compute_address.host_ip.address
}

module "theia-cloud" {
  source = "../../modules/theia-cloud"

  depends_on = [module.cluster_prerequisites]

  install_theia_cloud_base  = false
  hostname                  = "${google_compute_address.host_ip.address}.sslip.io"
  ingress_controller_type   = var.ingress_controller_type
  cert_manager_issuer_email = var.cert_manager_issuer_email
  cloud_provider            = "K8S"
  keycloak_url              = module.cluster_prerequisites.keycloak_url
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

  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "https://${google_compute_address.host_ip.address}.sslip.io/*"
}
