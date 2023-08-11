# Variables
variable "resource_group_name" {
  description = "The Azure Resource Group name"
}

variable "location" {
  description = "The location of the created AKS cluster"
}

variable "cluster_name" {
  default     = "aks-theia-cloud-getting-started"
  description = "The name of the created AKS cluster"
}

variable "node_pool_name" {
  default     = "defaultpool"
  description = "The name of the AKS node pool"
}

variable "node_pool_vm_size" {
  default     = "Standard_DS2_v2"
  description = "VM size of the AKS node pool"
}

variable "node_pool_min_nodes" {
  default     = 1
  description = "Minimum number of nodes for the AKS node pool"
}

variable "node_pool_max_nodes" {
  default     = 2
  description = "Maximum number of nodes for the AKS node pool"
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "aks" {
  name                = var.cluster_name
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = "aks-${var.cluster_name}"

  default_node_pool {
    name                = var.node_pool_name
    node_count          = var.node_pool_min_nodes
    vm_size             = var.node_pool_vm_size
    enable_auto_scaling = true
    min_count           = var.node_pool_min_nodes
    max_count           = var.node_pool_max_nodes
  }

  identity {
    type = "SystemAssigned"
  }
}

# To use kubectl commands
provider "kubernetes" {
  host                   = azurerm_kubernetes_cluster.aks.kube_config.0.host
  client_certificate     = base64decode(azurerm_kubernetes_cluster.aks.kube_config.0.client_certificate)
  client_key             = base64decode(azurerm_kubernetes_cluster.aks.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.aks.kube_config.0.cluster_ca_certificate)
}


data "azurerm_public_ips" "aks_pips" {
  resource_group_name = "mc_theiacloud_test_rg_aks-theia-cloud-getting-started_northeurope"
}


