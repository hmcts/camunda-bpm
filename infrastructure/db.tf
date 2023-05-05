module "database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "hmcts-${var.product}"
  location           = var.location
  env                = var.env
  postgresql_user    = "camundaadmin"
  database_name      = "camunda"
  postgresql_version = "11"
  sku_name           = var.sku_name
  sku_tier           = "GeneralPurpose"
  storage_mb         = var.storage_mb
  common_tags        = var.common_tags
  subscription       = var.subscription
  sku_capacity       = var.sku_capacity
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