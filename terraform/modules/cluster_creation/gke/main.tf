variable "project_id" {
  description = "The GCE project id"
}
variable "location" {
  description = "The zone of the created cluster"
}
variable "cluster_name" {
  default     = "gke-theia-cloud-getting-started"
  description = "The name of the created cluster"
}

variable "primary_node_pool_name" {
  default     = "default-pool"
  description = "The name of the primary node pool"
}

variable "primary_node_pool_machine" {
  default     = "e2-standard-2"
  description = "Machine Type of the primary node pool"
}

variable "primary_node_pool_initial_nodes" {
  default     = 1
  description = "Initial number of nodes for the primary node pool"
}

variable "primary_node_pool_max_nodes" {
  default     = 2
  description = "Maximum number of nodes for the primary node pool"
}

resource "google_container_cluster" "primary" {
  name                     = var.cluster_name
  location                 = var.location
  remove_default_node_pool = true
  initial_node_count       = 1
  deletion_protection      = false
}

resource "google_container_node_pool" "primary_nodes" {
  name               = var.primary_node_pool_name
  location           = var.location
  cluster            = var.cluster_name
  initial_node_count = var.primary_node_pool_initial_nodes
  depends_on         = [google_container_cluster.primary]

  autoscaling {
    max_node_count = var.primary_node_pool_max_nodes
  }

  node_config {
    preemptible  = false
    machine_type = var.primary_node_pool_machine
    metadata = {
      disable-legacy-endpoints = "true"
    }
  }

  provisioner "local-exec" {
    command = "gcloud container clusters get-credentials ${var.cluster_name} --zone ${var.location} --project ${var.project_id}"
  }
}

data "google_client_config" "default" {
  depends_on = [google_container_cluster.primary, google_container_node_pool.primary_nodes]
}
