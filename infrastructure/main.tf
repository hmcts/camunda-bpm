terraform {
  backend "azurerm" {}
}

provider "azurerm" {
  version = "=2.20.0"
  features {}
}

locals {
  vault_name = "${var.product}-${var.env}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.env}"
  location = var.location

  tags = var.common_tags
}
