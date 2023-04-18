provider "azurerm" {
  features {}
}

terraform {
  backend "azurerm" {}

  required_providers {
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "1.6.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.52.0"
    }
  }

}
