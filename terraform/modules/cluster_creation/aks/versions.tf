terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.68.0"
    }
  }

  required_version = ">= 1.4.0"
}