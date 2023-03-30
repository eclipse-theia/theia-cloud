terraform {
  required_providers {
    minikube = {
      source  = "scott-the-programmer/minikube"
      version = "0.2.3"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.9.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.18.1"
    }
    keycloak = {
      source  = "mrparkers/keycloak"
      version = ">= 4.2.0"
    }
  }

  required_version = ">= 1.4.0"
}
