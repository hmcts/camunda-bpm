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

variable "aks_infra_subscription_id" {}

variable "mgmt_subscription_id" {
  default = "8999dec3-0104-4a27-94ee-6588559729d1"
}

variable "dataNodesAreMasterEligible" {
  type    = "string"
  default = "Yes"
}

variable "vmDataNodeCount" {
  description = "number of data nodes"
  type        = "string"
  default     = "1"
}

variable "vmSizeAllNodes" {
  description = "vm size for all the cluster nodes"
  type        = "string"
  default     = "Standard_D2_v2"
}

variable "storageAccountType" {
  description = "disk storage account type"
  type        = "string"
  default     = "Standard"
}

variable "vmDataDiskCount" {
  description = "number of data node's disks"
  type        = "string"
  default     = "1"
}

variable "esAdditionalYaml" {
  description = "Additional configuration for Elasticsearch yaml configuration file. Each line must be separated by a \n"
  type        = "string"
  default     = "action.auto_create_index: .security*,.monitoring*,.watches,.triggered_watches,.watcher-history*,.logstash_dead_letter,.ml*\nxpack.monitoring.collection.enabled: true\nscript.allowed_types: inline\nscript.allowed_contexts: template, ingest\n"
}

variable "kibanaAdditionalYaml" {
  description = "Additional configuration for Kibana yaml configuration file. Each line must be separated by a \n"
  type        = "string"
  default     = "console.enabled: false\n"
}

variable "dynatrace_instance" {}

variable "dynatrace_hostgroup" {}
