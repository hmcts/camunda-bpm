module "database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "hmcts-${var.product}"
  location           = var.location
  env                = var.env
  postgresql_user    = "camundaadmin"
  database_name      = "camunda"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  storage_mb         = "51200"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

resource "azurerm_key_vault_secret" "postgres-user" {
  name         = "camunda-postgres-user"
  value        = module.database.user_name
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-password" {
  name         = "camunda-postgres-password"
  value        = module.database.postgresql_password
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-host" {
  name         = "camunda-postgres-host"
  value        = module.database.host_name
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-port" {
  name         = "camunda-postgres-port"
  value        = module.database.postgresql_listen_port
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-database" {
  name         = "camunda-postgres-database"
  value        = module.database.postgresql_database
  key_vault_id = module.vault.key_vault_id
}
