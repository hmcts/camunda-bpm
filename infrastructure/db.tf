module "postgresql_flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                  = var.env
  product              = var.product
  name                 = "hmcts-${var.product}-v14-flexible"
  component            = var.component
  business_area        = "CFT"
  location             = var.location
  pgsql_admin_username = "camundaadmin"
  pgsql_storage_mb      = var.pgsql_storage_mb

  common_tags          = var.common_tags
  admin_user_object_id = var.jenkins_AAD_objectId
  pgsql_databases = [
    {
      name : "camunda"
    }
  ]
  pgsql_firewall_rules = []
  pgsql_version = "14"
}
resource "azurerm_key_vault_secret" "postgres-user-v14" {
  name         = "bpm-v14-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-password-v14" {
  name         = "bpm-v14-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-host-v14" {
  name         = "bpm-v14-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-port-v14" {
  name         = "bpm-v14-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-database-v14" {
  name         = "bpm-v14-POSTGRES-DATABASE"
  value        = "camunda"
  key_vault_id = module.vault.key_vault_id
}