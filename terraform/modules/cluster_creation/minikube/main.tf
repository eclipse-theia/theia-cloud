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

resource "minikube_cluster" "cluster" {
  driver       = var.driver
  cluster_name = var.cluster_name
  cpus         = var.cpus
  disk_size    = var.disk_size
  memory       = var.memory
  addons = [
    "dashboard",
    "default-storageclass",
    "ingress",
    "metrics-server"
  ]

  provisioner "local-exec" {
    command = "kubectl config use-context ${var.cluster_name}"
  }
}
