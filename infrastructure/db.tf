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
  pgsql_storage_mb     = var.pgsql_storage_mb
  pgsql_sku            = var.pgsql_sku

  common_tags          = var.common_tags
  admin_user_object_id = var.jenkins_AAD_objectId
  pgsql_databases = [
    {
      name : "camunda"
    }
  ]
  // server_configuration values set based on SKU (CPU/RAM) and Max Connections
  pgsql_server_configuration = [
    {
      name  = "shared_buffers"
      value = "2097152"
    },
    {
      name  = "work_mem"
      value = "7489"
    },
    {
      name  = "maintenance_work_mem"
      value = "512000"
    },
    {
      name  = "effective_cache_size"
      value = "3932160"
    },
    {
      name  = "max_parallel_workers"
      value = "0"
    },
    {
      name  = "max_parallel_workers_per_gather"
      value = "0"
    },
    {
      name  = "random_page_cost"
      value = "1.1"
    },
    {
      name  = "wal_buffers"
      value = "16384"
    },
    {
      name  = "min_wal_size"
      value = "1024"
    },
    {
      name  = "max_wal_size"
      value = "4096"
    },
    {
      name  = "effective_io_concurrency"
      value = "200"
    },
    {
      name  = "backslash_quote"
      value = "on"
    },
    {
      name  = "azure.extensions"
      value = "PG_BUFFERCACHE,PG_STAT_STATEMENTS,PLPGSQL"
    }
  ]
  pgsql_firewall_rules = []
  pgsql_version        = "14"
}

resource "azurerm_key_vault_secret" "postgres-user" {
  name         = "bpm-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-password" {
  name         = "bpm-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-host" {
  name         = "bpm-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-port" {
  name         = "bpm-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = module.vault.key_vault_id
}

resource "azurerm_key_vault_secret" "postgres-database" {
  name         = "bpm-POSTGRES-DATABASE"
  value        = "camunda"
  key_vault_id = module.vault.key_vault_id
}