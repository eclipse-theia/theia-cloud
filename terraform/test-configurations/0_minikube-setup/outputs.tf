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
  value       = <<-EOT
    Minikube cluster has been created successfully!
    
    IMPORTANT: Before proceeding to step 1_dependencies, you must start minikube tunnel:
    
      minikube tunnel
    
    Keep this running in a separate terminal. The tunnel is required for the ingress
    controller to obtain an external IP and function properly.
    
    Once the tunnel is running, proceed to the 1_dependencies directory and run:
    
      terraform init
      terraform apply
  EOT
}
