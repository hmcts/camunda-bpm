terraform {
  backend "azurerm" {}
}

provider "azurerm" {
  version = "=2.29.0"
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

data "azurerm_key_vault" "key_vault" {
  name                = "${var.raw_product}-${var.env}"
  resource_group_name = "${var.raw_product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}


data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name      = "microservicekey-camunda-bpm"
}

# Copy camunda-bpm s2s secret from s2s key vault to camunda key vault
resource "azurerm_key_vault_secret" "camunda_bpm_s2s_secret" {
  name         = "s2s-secret-camunda-bpm"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.key_vault.id
}
