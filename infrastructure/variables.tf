variable "common_tags" {
  type = map
}

variable "raw_product" {
  default = "camunda"
}

variable "product" {}

variable "component" {}

variable "subscription" {}

variable "env" {
  type = string
}

variable "location" {
  default = "UK South"
}

variable "tenant_id" {
  type        = string
  description = "The Tenant ID of the Azure Active Directory"
}

variable "jenkins_AAD_objectId" {
  type        = string
  description = "This is the ID of the Application you wish to give access to the Key Vault via the access policy"
}

variable "aks_infra_subscription_id" {}

variable "mgmt_subscription_id" {}

variable "dataNodesAreMasterEligible" {
  type    = string
  default = "Yes"
}

variable "vmDataNodeCount" {
  description = "number of data nodes"
  type        = string
  default     = "1"
}

variable "vmSizeAllNodes" {
  description = "vm size for all the cluster nodes"
  type        = string
  default     = "Standard_D2_v2"
}

variable "storageAccountType" {
  description = "disk storage account type"
  type        = string
  default     = "Standard"
}

variable "vmDataDiskCount" {
  description = "number of data node's disks"
  type        = string
  default     = "1"
}

variable "dynatrace_instance" {}

variable "dynatrace_hostgroup" {}
