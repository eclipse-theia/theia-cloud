output "host" {
  description = "The hostname of Kubernetes master."
  value       = module.cluster.cluster_host
}

output "client_certificate" {
  description = "PEM-encoded client certificate for TLS authentication."
  value       = module.cluster.cluster_client_certificate
  sensitive   = true
}

output "client_key" {
  description = "PEM-encoded client certificate key for TLS authentication."
  value       = module.cluster.cluster_client_key
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication."
  value       = module.cluster.cluster_ca_certificate
  sensitive   = true
}

output "hostname" {
  description = "nip.io hostname that can be used for ingresses with this cluster."
  value       = "${module.host.host}.nip.io"
}

output "keycloak" {
  description = "Keycloak"
  value       = "${module.host.host}.nip.io/keycloak"
}
