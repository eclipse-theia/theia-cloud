output "cluster_host" {
  description = "The hostname of Kubernetes master."
  value       = "https://${google_container_cluster.primary.endpoint}"
}

output "cluster_token" {
  description = "Token to authenticate an service account"
  value       = data.google_client_config.default.access_token
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication."
  value       = base64decode(google_container_cluster.primary.master_auth[0].cluster_ca_certificate)
  sensitive   = true
}

