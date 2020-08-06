variable "common_tags" {
  type = "map"
}

variable "product" {}

variable "component" {}

variable "subscription" {}

variable "env" {}

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
