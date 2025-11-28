variable "hostname" {
  description = "Hostname for Keycloak ingress"
  type        = string
}

variable "keycloak_admin_username" {
  description = "Keycloak admin username"
  type        = string
  default     = "admin"
}

variable "keycloak_admin_password" {
  description = "Keycloak admin password"
  type        = string
  sensitive   = true
}

variable "postgres_enabled" {
  description = "Whether to deploy PostgreSQL database"
  type        = bool
  default     = true
}

variable "postgres_database" {
  description = "PostgreSQL database name"
  type        = string
  default     = "keycloak"
}

variable "postgres_username" {
  description = "PostgreSQL username"
  type        = string
  default     = "keycloak"
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "postgres_storage_class" {
  description = "Storage class for PostgreSQL PVC"
  type        = string
  default     = ""
}

variable "postgres_storage_size" {
  description = "Storage size for PostgreSQL PVC"
  type        = string
  default     = "10Gi"
}

variable "postgres_volume_permissions" {
  description = "Enable init container that changes the owner and group of the persistent volume"
  type        = bool
  default     = false
}

variable "postgres_image" {
  description = "PostgreSQL Docker image. See Keycloak database compatibility at https://www.keycloak.org/server/db#_supported_databases"
  type        = string
  default     = "postgres:17"
}

variable "keycloak_namespace" {
  description = "Kubernetes namespace for Keycloak"
  type        = string
  default     = "keycloak"
}

variable "keycloak_version" {
  description = "Keycloak operator version (tag from keycloak-k8s-resources repository)"
  type        = string
  default     = "26.4.5"
}

variable "keycloak_http_relative_path" {
  description = "HTTP relative path for Keycloak"
  type        = string
  default     = "/keycloak"
}

variable "keycloak_replicas" {
  description = "Number of Keycloak replicas"
  type        = number
  default     = 1
}

variable "keycloak_resource_requests_cpu" {
  description = "CPU resource requests for Keycloak"
  type        = string
  default     = "500m"
}

variable "keycloak_resource_requests_memory" {
  description = "Memory resource requests for Keycloak"
  type        = string
  default     = "1Gi"
}

variable "keycloak_resource_limits_cpu" {
  description = "CPU resource limits for Keycloak"
  type        = string
  default     = "1"
}

variable "keycloak_resource_limits_memory" {
  description = "Memory resource limits for Keycloak"
  type        = string
  default     = "2Gi"
}

variable "ingress_enabled" {
  description = "Whether to create Kubernetes Ingress"
  type        = bool
  default     = true
}

variable "ingress_class_name" {
  description = "Ingress class name"
  type        = string
  default     = "nginx"
}

variable "ingress_tls_enabled" {
  description = "Whether to enable TLS for ingress"
  type        = bool
  default     = true
}

variable "ingress_cert_manager_cluster_issuer" {
  description = "Cert-manager cluster issuer for TLS certificate"
  type        = string

  validation {
    condition     = length(regexall("^(letsencrypt-prod|theia-cloud-selfsigned-issuer|keycloak-selfsigned-issuer)$", var.ingress_cert_manager_cluster_issuer)) > 0
    error_message = "ERROR: Valid values are \"letsencrypt-prod\", \"theia-cloud-selfsigned-issuer\", and \"keycloak-selfsigned-issuer\"!"
  }
}

variable "ingress_cert_manager_common_name" {
  description = "The common name for the certificate"
  default     = ""
}

variable "ingress_annotations" {
  description = "Additional annotations for ingress"
  type        = map(string)
  default     = {}
}

variable "ingress_tls_secret_name" {
  description = "Name of TLS secret (auto-generated if not specified)"
  type        = string
  default     = ""
}

variable "cloud_provider" {
  description = "Cloud provider type"
  type        = string
  default     = "K8S"
  validation {
    condition     = contains(["MINIKUBE", "GKE", "K8S"], var.cloud_provider)
    error_message = "Valid values are: MINIKUBE, GKE, K8S"
  }
}

variable "install_cert_manager" {
  description = "Whether to install cert-manager"
  type        = bool
  default     = true
}

variable "cert_manager_version" {
  description = "Version of cert-manager to install"
  type        = string
  default     = "v1.17.4"
}

variable "cert_manager_namespace" {
  description = "Namespace for cert-manager installation"
  type        = string
  default     = "cert-manager"
}

variable "install_selfsigned_issuer" {
  description = "Whether to install an additional self-signed ClusterIssuer for Keycloak"
  type        = bool
  default     = false
}

variable "cert_manager_issuer_email" {
  description = "Email address used to create certificates (required for letsencrypt-prod issuer)"
  type        = string
  default     = ""
}
