output "try_now" {
  description = "Try Now URL."
  value       = "https://${local.hostname}/trynow/"
}

output "keycloak" {
  description = "Keycloak Admin URL."
  value       = "${module.cluster_prerequisites.keycloak_url}/"
}
