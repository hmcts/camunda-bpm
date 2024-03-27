variable "common_tags" {
  type = map(string)
}

variable "raw_product" {
  default = "camunda"
}

variable "product" {}

variable "component" {}

variable "subscription" {}

variable "aks_subscription_id" {}

variable "env" {}

variable "location" {
  default = "UK South"
}

variable "tenant_id" {
  description = "The Tenant ID of the Azure Active Directory"
}

variable "jenkins_AAD_objectId" {
  description = "This is the ID of the Application you wish to give access to the Key Vault via the access policy"
}

variable "pgsql_storage_mb" {
  description = "Storage MB for Postgresql DB"
  default     = "65536"
}

variable "pgsql_sku" {
  description = "SKU for Postgresql DB"
  default     = "GP_Standard_D2s_v3"
}

variable "pgsql_server_configuration" {
  description = "pgsql server configuration options"
  type = object({
    shared_buffers  = optional(string, "786432")
    max_wal_size    = optional(string, "4096")
  })
  default = {}
}

variable "pgsql_version" {
  description = "Version number of pgsql instance"
  type = string
  default = "14"
}

variable "pgsql_create_mode" {
  description = "The creation mode which can be used to restore or replicate existing servers. Possible values are Default, PointInTimeRestore, Replica and Update."
  type = string
  default = "Default"
}