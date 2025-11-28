variable "kubernetes_version" {
  description = "Kubernetes version to use"
  default     = "v1.26.3"
}

variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "haproxy" # "nginx"
}

provider "minikube" {
  kubernetes_version = var.kubernetes_version
}

module "cluster" {
  source = "../../modules/cluster_creation/minikube/"

  # adjust values below
  cluster_name            = "minikube"
  cpus                    = 4
  disk_size               = "51200mb"
  memory                  = "8192mb"
  driver                  = "kvm2"
  ingress_controller_type = var.ingress_controller_type
}
