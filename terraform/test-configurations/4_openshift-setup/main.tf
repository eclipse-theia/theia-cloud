variable "openshift_server" {
  description = "OpenShift API server URL (e.g. https://api.crc.testing:6443)"
  default     = "https://api.crc.testing:6443"
}

variable "openshift_token" {
  description = "OpenShift login token (get via: oc whoami -t)"
  type        = string
  sensitive   = true
}

variable "apps_domain" {
  description = "OpenShift apps domain for Routes (e.g. apps-crc.testing)"
  default     = "apps-crc.testing"
}

variable "keycloak_admin_password" {
  description = "Keycloak Admin Password"
  sensitive   = true
  default     = "admin"
}

variable "postgres_password" {
  description = "Keycloak Postgres DB Password"
  sensitive   = true
  default     = "admin"
}

provider "kubernetes" {
  host     = var.openshift_server
  token    = var.openshift_token
  insecure = true # CRC uses self-signed certs
}

provider "helm" {
  kubernetes = {
    host     = var.openshift_server
    token    = var.openshift_token
    insecure = true
  }
}

provider "kubectl" {
  load_config_file = false
  host             = var.openshift_server
  token            = var.openshift_token
  insecure         = true
}

# cert-manager is required by the theia-cloud-crds chart for the conversion webhook certificate
resource "helm_release" "cert-manager" {
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

module "cluster_prerequisites" {
  source = "../../modules/cluster-prerequisites"

  depends_on = [helm_release.cert-manager]

  hostname                            = "keycloak.${var.apps_domain}"
  keycloak_admin_password             = var.keycloak_admin_password
  postgres_password                   = var.postgres_password
  install_cert_manager                = false
  install_selfsigned_issuer           = false
  install_ingress_controller          = false
  ingress_enabled                     = false
  ingress_cert_manager_cluster_issuer = "keycloak-selfsigned-issuer"
  ingress_cert_manager_common_name    = "keycloak.${var.apps_domain}"
  keycloak_http_relative_path         = "/keycloak/"
  cloud_provider                      = "OPENSHIFT"
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = var.keycloak_admin_password
  url                      = module.cluster_prerequisites.keycloak_url
  tls_insecure_skip_verify = true # CRC uses self-signed certs
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [module.cluster_prerequisites]

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
