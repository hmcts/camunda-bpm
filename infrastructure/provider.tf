provider "azurerm" {
  alias = "postgres_network"
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
      version = "2.37.2"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.53.0"
    }
  }

}
