module "vault" {
  source                     = "git@github.com:hmcts/cnp-module-key-vault?ref=azurermv2"
  name                       = local.vault_name
  product                    = var.product
  env                        = var.env
  tenant_id                  = var.tenant_id
  object_id                  = var.jenkins_AAD_objectId
  resource_group_name        = azurerm_resource_group.rg.name
  product_group_object_id    = "300e771f-856c-45cc-b899-40d78281e9c1"
  create_managed_identity    = true
  common_tags                = var.common_tags
}
