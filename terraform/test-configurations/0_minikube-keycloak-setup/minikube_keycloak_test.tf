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

provider "helm" {
  kubernetes = {
    host                   = module.cluster.cluster_host
    client_certificate     = module.cluster.cluster_client_certificate
    client_key             = module.cluster.cluster_client_key
    cluster_ca_certificate = module.cluster.cluster_ca_certificate
  }
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

provider "kubectl" {
  load_config_file       = false
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

provider "http" {
}

module "host" {
  depends_on = [module.cluster]

  source = "matti/urlparse/external"
  url    = module.cluster.cluster_host
}

# resource "helm_release" "nginx_ingress_controller" {
#   depends_on       = [module.cluster]
#   name             = "nginx-ingress-controller"
#   repository       = "https://kubernetes.github.io/ingress-nginx"
#   chart            = "ingress-nginx"
#   version          = "4.13.0"
#   namespace        = "ingress-nginx"
#   create_namespace = true

#   set {
#     name  = "fullnameOverride"
#     value = "ingress-nginx"
#   }

#   set {
#     name  = "controller.allowSnippetAnnotations"
#     value = true
#   }

#   set {
#     name  = "controller.admissionWebhooks.enabled"
#     value = false
#   }

#   set {
#     name  = "controller.config.enable-snippet"
#     value = "true"
#   }
# }

module "keycloak_setup" {
  source = "../../modules/keycloak-setup"

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

# Output the Keycloak URL for debugging
output "debug_keycloak_url" {
  value = module.keycloak_setup.keycloak_url
}

# Wait for Keycloak to be fully ready before configuring provider
resource "time_sleep" "wait_for_keycloak" {
  depends_on = [module.keycloak_setup]
  
  create_duration = "30s"
}

# Test Keycloak availability before proceeding
resource "terraform_data" "verify_keycloak_auth" {
  depends_on = [time_sleep.wait_for_keycloak]
  
  provisioner "local-exec" {
    command = <<-EOT
      echo "Testing Keycloak authentication..."
      echo "Attempting to retrieve admin credentials from Keycloak..."
      
      # Check if Keycloak created an initial admin secret
      kubectl get secret -n keycloak keycloak-initial-admin 2>/dev/null && \
        echo "Found keycloak-initial-admin secret" || \
        echo "No keycloak-initial-admin secret found"
      
      # Try authentication with provided credentials
      sleep 10
      RESPONSE=$(curl -k -s -X POST \
        "https://${module.host.host}.nip.io/keycloak/realms/master/protocol/openid-connect/token" \
        -d "client_id=admin-cli" \
        -d "username=admin" \
        -d "password=${var.keycloak_admin_password}" \
        -d "grant_type=password")
      
      echo "Keycloak response: $RESPONSE"
      
      if echo "$RESPONSE" | grep -q "access_token"; then
        echo "SUCCESS: Keycloak authentication successful"
      else
        echo "WARNING: Keycloak authentication test failed. Response: $RESPONSE"
        echo "This might be expected on first run. Terraform will retry..."
      fi
    EOT
  }
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = var.keycloak_admin_password
  url                      = module.keycloak_setup.keycloak_url
  tls_insecure_skip_verify = true
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../../modules/keycloak"

  depends_on = [
    terraform_data.verify_keycloak_auth
  ]

  hostname                        = "${module.host.host}.nip.io"
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
