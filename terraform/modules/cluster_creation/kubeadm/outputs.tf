output "cluster_host" {
  description = "The hostname of Kubernetes master."
  value       = var.cluster_host
}

output "cluster_client_certificate" {
  description = "PEM-encoded client certificate for TLS authentication."
  value = file(var.cluster_client_certificate_file)
}

output "cluster_client_key" {
  description = "PEM-encoded client certificate key for TLS authentication."
  value = file(var.cluster_client_key_file)
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication."
  value = file(var.cluster_ca_certificate_file)
}
