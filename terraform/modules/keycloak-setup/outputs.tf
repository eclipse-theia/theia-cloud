output "namespace" {
  description = "Keycloak namespace"
  value       = kubernetes_namespace.keycloak.metadata[0].name
}

output "keycloak_url" {
  description = "Full URL to access Keycloak"
  value       = var.ingress_enabled ? "https://${var.hostname}${var.keycloak_http_relative_path}" : "http://${var.hostname}:8080${var.keycloak_http_relative_path}"
}

output "admin_username" {
  description = "Keycloak admin username"
  value       = var.keycloak_admin_username
}

output "postgres_service_name" {
  description = "PostgreSQL service name"
  value       = var.postgres_enabled ? kubernetes_service.postgres[0].metadata[0].name : null
}

output "keycloak_service_name" {
  description = "Keycloak service name (created by operator)"
  value       = "keycloak-service"
}

output "tls_secret_name" {
  description = "TLS certificate secret name"
  value       = var.ingress_tls_enabled ? local.tls_secret_name : null
}
