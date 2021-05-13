provider "azurerm" {
  alias                      = "aks-infra"
  subscription_id            = var.aks_subscription_id
  skip_provider_registration = true
  features {}
}

provider "azurerm" {
  alias           = "mgmt"
  subscription_id = var.mgmt_subscription_id
  features {}
}

module "elastic" {
  source                        = "git@github.com:hmcts/cnp-module-elk.git?ref=7.11.1"
  product                       = var.product
  location                      = var.location
  env                           = var.env
  subscription                  = var.subscription
  common_tags                   = var.common_tags
  dataNodesAreMasterEligible    = true
  vmDataNodeCount               = var.vmDataNodeCount
  vmSizeAllNodes                = var.vmSizeAllNodes
  storageAccountType            = var.storageAccountType
  vmDataDiskCount               = 1
  ssh_elastic_search_public_key = data.azurerm_key_vault_secret.camunda_elastic_search_public_key.value
  providers = {
    azurerm           = azurerm
    azurerm.mgmt      = azurerm.mgmt
    azurerm.aks-infra = azurerm.aks-infra
  }
  logAnalyticsId      = data.azurerm_log_analytics_workspace.log_analytics.workspace_id
  logAnalyticsKey     = data.azurerm_log_analytics_workspace.log_analytics.primary_shared_key
  dynatrace_instance  = var.dynatrace_instance
  dynatrace_hostgroup = var.dynatrace_hostgroup
  dynatrace_token     = data.azurerm_key_vault_secret.dynatrace_token.value
  enable_logstash     = false
  enable_kibana       = false
  alerts_email        = data.azurerm_key_vault_secret.alerts_email.value
}

locals {
  // Vault name
  vaultName = "${var.product}-${var.env}"
}

data "azurerm_log_analytics_workspace" "log_analytics" {
  name                = "hmcts-${var.subscription}"
  resource_group_name = "oms-automation"
}

data "azurerm_key_vault_secret" "camunda_elastic_search_public_key" {
  name         = "${var.product}-ELASTIC-SEARCH-PUB-KEY"
  key_vault_id = module.vault.key_vault_id
}

data "azurerm_key_vault_secret" "dynatrace_token" {
  name         = "dynatrace-token"
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "elastic_search_url_key_setting" {
  name         = "${var.product}-ELASTIC-SEARCH-URL"
  value        = module.elastic.loadbalancerManual
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "elastic_search_pwd_key_setting" {
  name         = "${var.product}-ELASTIC-SEARCH-PASSWORD"
  value        = module.elastic.elasticsearch_admin_password
  key_vault_id = module.vault.key_vault_id
}

data "azurerm_key_vault_secret" "alerts_email" {
  name         = "alerts-email"
  key_vault_id = module.vault.key_vault_id
}
