module "vault" {
  source                       = "git@github.com:hmcts/cnp-module-key-vault?ref=DTSPO-31965/remove-jenkins-ptl-access"
  name                         = local.vault_name
  product                      = var.product
  env                          = var.env
  tenant_id                    = var.tenant_id
  object_id                    = var.jenkins_AAD_objectId
  jenkins_object_id            = data.azurerm_user_assigned_identity.jenkins.principal_id
  grant_preview_jenkins_access = var.env == "aat"
  resource_group_name          = azurerm_resource_group.rg.name
  product_group_object_id      = "e7ea2042-4ced-45dd-8ae3-e051c6551789"
  create_managed_identity      = true
  common_tags                  = var.common_tags
}
