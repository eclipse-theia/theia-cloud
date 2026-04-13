variable "openshift_server" {
  description = "OpenShift API server URL (e.g. https://api.crc.testing:6443)"
  default     = "https://api.crc.testing:6443"
}

variable "openshift_token" {
  description = "OpenShift login token (get via: oc whoami -t)"
  type        = string
  sensitive   = true
}

variable "apps_domain" {
  description = "OpenShift apps domain for Routes (e.g. apps-crc.testing)"
  default     = "apps-crc.testing"
}

provider "kubernetes" {
  host     = var.openshift_server
  token    = var.openshift_token
  insecure = true # CRC uses self-signed certs
}
