terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 6.47.0"
    }
  }

  required_version = ">= 1.12.2"
}
