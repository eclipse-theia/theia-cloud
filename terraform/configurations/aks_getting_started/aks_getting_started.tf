# Variables
variable "subscription_id" {
  description = "Azure Subscription ID"
}

variable "location" {
  description = "The Azure region for resources"
}

variable "resource_group_name" {
  description = "The Azure resource group to deploy into"
}

variable "cert_manager_issuer_email" {
  description = "Email address used to create certificates."
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

# Providers
provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}

resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.location
}

module "cluster" {
  source              = "../../modules/cluster_creation/aks/"
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
}

# Helm provider configuration
provider "helm" {
  kubernetes {
    host                   = module.cluster.cluster_host
    client_certificate     = module.cluster.cluster_client_certificate
    client_key             = module.cluster.cluster_client_key
    cluster_ca_certificate = module.cluster.cluster_ca_certificate
  }
}

 # Kubectl provider configuration
 provider "kubectl" {
   load_config_file       = false
   host                   = module.cluster.cluster_host
   client_certificate     = module.cluster.cluster_client_certificate
   client_key             = module.cluster.cluster_client_key
   cluster_ca_certificate = module.cluster.cluster_ca_certificate
 }

 module "helm" {
   source = "../../modules/helm"

   install_ingress_controller  = true
   cert_manager_issuer_email   = var.cert_manager_issuer_email
   cert_manager_cluster_issuer = "letsencrypt-prod"
   cert_manager_common_name    = "${module.cluster.loadbalancer_ip}.sslip.io"
   hostname                    = "${module.cluster.loadbalancer_ip}.sslip.io"
   keycloak_admin_password     = var.keycloak_admin_password
   postgresql_enabled          = true
   postgres_postgres_password  = var.postgres_postgres_password
   postgres_password           = var.postgres_password
   loadBalancerIP              = module.cluster.loadbalancer_ip
 }

 provider "keycloak" {
   client_id     = "admin-cli"
   username      = "admin"
   password      = var.keycloak_admin_password
   url           = "https://${module.cluster.loadbalancer_ip}.sslip.io/keycloak"
   initial_login = false
 }

 module "keycloak" {
   source = "../../modules/keycloak"

   depends_on = [module.helm]

   hostname                        = "${module.cluster.loadbalancer_ip}.sslip.io"
   keycloak_test_user_foo_password = "foo"
   keycloak_test_user_bar_password = "bar"
   valid_redirect_uri              = "https://${module.cluster.loadbalancer_ip}.sslip.io/*"
 }
