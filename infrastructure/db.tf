module "database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "hmcts-${var.product}"
  location           = var.location
  env                = var.env
  postgresql_user    = "camundaadmin"
  database_name      = "camunda"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_4"
  sku_tier           = "GeneralPurpose"
  storage_mb         = "179200"
  common_tags        = var.common_tags
  subscription       = var.subscription
  sku_capacity       = 4
}

resource "azurerm_key_vault_secret" "postgres-user" {
  name         = "bpm-POSTGRES-USER"
  value        = module.database.user_name
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-password" {
  name         = "bpm-POSTGRES-PASS"
  value        = module.database.postgresql_password
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-host" {
  name         = "bpm-POSTGRES-HOST"
  value        = module.database.host_name
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-port" {
  name         = "bpm-POSTGRES-PORT"
  value        = module.database.postgresql_listen_port
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-database" {
  name         = "bpm-POSTGRES-DATABASE"
  value        = module.database.postgresql_database
  key_vault_id = module.vault.key_vault_id
}
