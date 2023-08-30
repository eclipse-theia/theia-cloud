terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.9.0"
    }
  }

  required_version = ">= 1.4.0"
}
