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
  description = "The hostname for all installed services"
}

variable "keycloak_url" {
  description = "The base URL of the Keycloak instance used for authentication. If not provided, it will be constructed from the 'hostname' variable assuming keycloak is hosted at relative path /keycloak/."
  default     = ""
}

variable "cloudProvider" {
  description = "The cloud provider to use"
  default     = "K8S"
}

variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
}
