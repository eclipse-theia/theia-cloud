locals {
  next_steps_haproxy = <<-EOT
    Minikube cluster created successfully!

    NEXT STEPS FOR HAPROXY:
    1. Open a new terminal and run: minikube tunnel
    2. Keep the tunnel running
    3. In this terminal, cd to ../1_theiacloud-and-dependencies
    4. Run: terraform init && terraform apply
  EOT

  next_steps_nginx = <<-EOT
    Minikube cluster created successfully!

    NEXT STEPS FOR NGINX:
    1. cd to ../1_theiacloud-and-dependencies
    2. Run: terraform init && terraform apply
  EOT
}

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

output "ingress_controller_type" {
  description = "Type of ingress controller configured for this cluster."
  value       = var.ingress_controller_type
}

output "next_steps" {
  description = "Instructions for the next step."
  value       = var.ingress_controller_type == "haproxy" ? local.next_steps_haproxy : local.next_steps_nginx
}
