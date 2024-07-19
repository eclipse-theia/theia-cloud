terraform {
  required_providers {
    minikube = {
      source  = "scott-the-programmer/minikube"
      version = "0.2.3"
    }
  }

  required_version = ">= 1.4.0"
}
