variable "common_tags" {
  type = map
}
variable "product" {}

variable "component" {}

variable "subscription" {}

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

variable "aks_subscription_id" {}

variable "mgmt_subscription_id" {}

variable "vmDataNodeCount" {
  description = "number of data nodes"
  type        = number
  default     = 1
}

variable "vmSizeAllNodes" {
  description = "vm size for all the cluster nodes"
  default     = "Standard_D2s_v3"
}

variable "storageAccountType" {
  description = "disk storage account type"
  default     = "Standard"
}

variable "dynatrace_instance" {}

variable "dynatrace_hostgroup" {}
