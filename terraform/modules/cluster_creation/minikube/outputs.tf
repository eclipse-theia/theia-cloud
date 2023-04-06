output "cluster_host" {
  description = "The hostname of Kubernetes master."
  value       = minikube_cluster.cluster.host
}

output "cluster_client_certificate" {
  description = "PEM-encoded client certificate for TLS authentication."
  value       = minikube_cluster.cluster.client_certificate
}

output "cluster_client_key" {
  description = "PEM-encoded client certificate key for TLS authentication."
  value       = minikube_cluster.cluster.client_key
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication."
  value       = minikube_cluster.cluster.cluster_ca_certificate
}
