variable "cluster_name" {
  description = "Minikube Cluster Name"
}

variable "cpus" {
  description = "Number of CPUs used by the Node."
}

variable "disk_size" {
  description = "Disk size of the Node."
}

variable "memory" {
  description = "Memory available for the Node."
}

variable "driver" {
  description = "Minikube driver"
}

variable "container_runtime" {
  description = "The container runtime"
  default     = "docker"
}

variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "nginx"
}

locals {
  nginx_config_script = <<-EOT
    kubectl config use-context ${var.cluster_name}
    kubectl -n ingress-nginx patch configmap ingress-nginx-controller \
      --type merge \
      -p '{"data":{"allow-snippet-annotations":"true","annotations-risk-level":"Critical"}}'
    kubectl -n ingress-nginx rollout restart deployment ingress-nginx-controller
    kubectl -n ingress-nginx rollout status deployment ingress-nginx-controller --timeout=90s
  EOT

  skip_config_script = "echo 'Skipping additional configuration for ${var.ingress_controller_type} ingress on Minikube'"
}

resource "minikube_cluster" "cluster" {
  driver            = var.driver
  cluster_name      = var.cluster_name
  cpus              = var.cpus
  disk_size         = var.disk_size
  memory            = var.memory
  container_runtime = var.container_runtime

  addons = concat([
    "dashboard",
    "default-storageclass",
    "metrics-server"
  ], var.ingress_controller_type == "nginx" ? ["ingress"] : [])

  provisioner "local-exec" {
    command = var.ingress_controller_type == "nginx" ? local.nginx_config_script : local.skip_config_script
  }
}
