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

resource "random_string" "password" {
  length  = 16
  special = true
  upper   = true
  lower   = true
  number  = true
}

resource "azurerm_key_vault_secret" "camunda-admin-password" {
  name         = "camunda-admin-password"
  value        = random_string.password.result
  key_vault_id = module.vault.key_vault_id
}