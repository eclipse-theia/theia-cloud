output "host" {
  description = "The hostname of Kubernetes master."
  value       = data.terraform_remote_state.minikube.outputs.host
}

output "client_certificate" {
  description = "PEM-encoded client certificate for TLS authentication."
  value       = data.terraform_remote_state.minikube.outputs.client_certificate
  sensitive   = true
}

output "client_key" {
  description = "PEM-encoded client certificate key for TLS authentication."
  value       = data.terraform_remote_state.minikube.outputs.client_key
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "PEM-encoded root certificates bundle for TLS authentication."
  value       = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
  sensitive   = true
}

output "hostname" {
  description = "nip.io hostname that can be used for ingresses with this cluster."
  value       = local.hostname
}

output "keycloak" {
  description = "Keycloak"
  value       = "${local.hostname}/keycloak"
}

output "ingress_controller_type" {
  description = "Type of ingress controller in use."
  value       = data.terraform_remote_state.minikube.outputs.ingress_controller_type
}
