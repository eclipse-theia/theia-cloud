terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.57.0"
    }
  }

  required_version = ">= 1.4.0"
}
