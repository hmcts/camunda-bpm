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