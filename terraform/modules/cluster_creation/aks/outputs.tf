output "cluster_host" {
  description = "The hostname of the AKS Kubernetes master."
  value       = azurerm_kubernetes_cluster.aks.kube_config[0].host
}

output "cluster_client_certificate" {
  description = "Client certificate used for authentication to the AKS Kubernetes master."
  value       = base64decode(azurerm_kubernetes_cluster.aks.kube_config[0].client_certificate)
   sensitive   = true
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication to the AKS Kubernetes master."
  value       = base64decode(azurerm_kubernetes_cluster.aks.kube_config[0].cluster_ca_certificate)
   sensitive   = true
}

output "cluster_client_key" {
  description = "Client key used for authentication to the AKS Kubernetes master."
  value       = base64decode(azurerm_kubernetes_cluster.aks.kube_config[0].client_key)
   sensitive   = true
}

output "loadbalancer_ip" {
  description = "The IP Address of the first public IP in the AKS resource group"
  value       = length(data.azurerm_public_ips.aks_pips.public_ips) > 0 ? data.azurerm_public_ips.aks_pips.public_ips[0].ip_address : "No IPs found"
}
