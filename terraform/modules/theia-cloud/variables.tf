variable "install_theia_cloud_base" {
  description = "Whether to install theia cloud base"
  default     = true
}

variable "install_theia_cloud_crds" {
  description = "Whether to install theia cloud crds"
  default     = true
}

variable "install_theia_cloud" {
  description = "Whether to install theia cloud"
  default     = true
}

variable "hostname" {
  description = "The hostname for the deployment"
}

variable "ingress_controller_type" {
  description = "Type of ingress controller to use (nginx or haproxy)"
  type        = string
  default     = "nginx"

  validation {
    condition     = contains(["nginx", "haproxy"], var.ingress_controller_type)
    error_message = "Valid values are 'nginx' or 'haproxy'."
  }
}

variable "keycloak_url" {
  description = "The base URL of the Keycloak instance used for authentication. If not provided, it will be constructed from the 'hostname' variable assuming keycloak is hosted at relative path /keycloak/."
  default     = ""
}

variable "cloud_provider" {
  description = "Cloud provider type"
  type        = string
  default     = "K8S"
  validation {
    condition     = contains(["MINIKUBE", "K8S"], var.cloud_provider)
    error_message = "Valid values are: MINIKUBE, K8S"
  }
}

variable "cert_manager_issuer_email" {
  description = "Email address used to create certificates."
  type        = string
  default     = ""
}
