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

variable "postgres_postgres_password" {
  description = "Keycloak Postgres DB Postgres (Admin) Password"
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

resource "helm_release" "keycloak" {
  depends_on       = [helm_release.cert-manager]
  name             = "keycloak"
  repository       = "https://charts.bitnami.com/bitnami"
  chart            = "keycloak"
  version          = "15.1.8"
  namespace        = "keycloak"
  create_namespace = true

  values = [
    file("${path.module}/keycloak-values.yaml")
  ]

  set = [
    {
      name  = "ingress.hostname"
      value = "keycloak.${var.apps_domain}"
    }
  ]
  set_sensitive = [
    {
      name  = "auth.adminPassword"
      value = var.keycloak_admin_password
    },
    {
      name  = "postgresql.auth.postgresPassword"
      value = var.postgres_postgres_password
    },
    {
      name  = "postgresql.auth.password"
      value = var.postgres_password
    }
  ]
}

resource "kubectl_manifest" "keycloak_route" {
  depends_on = [helm_release.keycloak]
  yaml_body  = templatefile("${path.module}/keycloak-route.yaml", {
    hostname = "keycloak.${var.apps_domain}"
  })
}

# Wait for Keycloak to be fully ready before configuring the realm.
# The Route may exist before Keycloak pods are ready to serve traffic.
resource "null_resource" "wait_for_keycloak" {
  depends_on = [kubectl_manifest.keycloak_route]
  provisioner "local-exec" {
    command = "until curl -sf -o /dev/null -k https://keycloak.${var.apps_domain}/realms/master; do echo 'Waiting for Keycloak...'; sleep 5; done"
  }
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = var.keycloak_admin_password
  url                      = "https://keycloak.${var.apps_domain}/"
  tls_insecure_skip_verify = true # CRC uses self-signed certs
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source     = "../../modules/keycloak"
  depends_on = [null_resource.wait_for_keycloak]

  hostname                        = "keycloak.${var.apps_domain}"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "*"
}

resource "keycloak_group_memberships" "admin_group_memberships" {
  realm_id = module.keycloak.realm.id
  group_id = module.keycloak.admin_group.id
  members  = [
    module.keycloak.test_users.foo.username
  ]
}
