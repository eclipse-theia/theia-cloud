variable "kubernetes_version" {
  description = "Kubernetes version to use"
  default     = "v1.34.0"
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

provider "kubernetes" {
  host                   = module.cluster.cluster_host
  client_certificate     = module.cluster.cluster_client_certificate
  client_key             = module.cluster.cluster_client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}
