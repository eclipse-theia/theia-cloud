terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.0.0"
    }
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.19.0"
    }
    http = {
      source  = "hashicorp/http"
      version = ">= 3.0.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.0.0"
    }
  }
  required_version = ">= 1.12.2"
}
