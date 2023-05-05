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
  numeric = true
}

resource "azurerm_key_vault_secret" "camunda-admin-password" {
  name         = "camunda-admin-password"
  value        = random_string.password.result
  key_vault_id = module.vault.key_vault_id
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}


data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-camunda-bpm"
}

# Copy camunda-bpm s2s secret from s2s key vault to camunda key vault
resource "azurerm_key_vault_secret" "camunda_bpm_s2s_secret" {
  name         = "s2s-secret-camunda-bpm"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = module.vault.key_vault_id
}


# Application insight
resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.component}-appinsights-${var.env}"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"

  tags = var.common_tags

  lifecycle {
    ignore_changes = [
      # Ignore changes to appinsights as otherwise upgrading to the Azure provider 2.x
      # destroys and re-creates this appinsights instance
      application_type,
    ]
  }
}

resource "azurerm_key_vault_secret" "app_insights_key" {
  name         = "AppInsightsInstrumentationKey"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.vault.key_vault_id
}