output "host" {
  description = "The OpenShift API server URL."
  value       = var.openshift_server
}

output "token" {
  description = "The OpenShift API token."
  value       = var.openshift_token
  sensitive   = true
}

output "hostname" {
  description = "Base hostname for Theia Cloud routes."
  value       = var.apps_domain
}
