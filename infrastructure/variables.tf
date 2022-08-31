variable "common_tags" {
  type = "map"
}

variable "raw_product" {
  default = "camunda"
}

variable "product" {}

variable "component" {}

variable "subscription" {}

variable "env" {
  type = "string"
}

variable "location" {
  default = "UK South"
}

variable "tenant_id" {
  type        = "string"
  description = "The Tenant ID of the Azure Active Directory"
}

variable "jenkins_AAD_objectId" {
  type        = "string"
  description = "This is the ID of the Application you wish to give access to the Key Vault via the access policy"
}

variable "storage_mb" {
  type        = "string"
  description = "Storage MB for Postgresql DB"
  default = "179200"
}

variable "sku_capacity" {
  type        = "string"
  description = "SKU for Postgresql DB"
  default = "4"
}