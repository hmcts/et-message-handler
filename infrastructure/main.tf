provider "azurerm" {
  features {}
}

locals {
  tags = merge(var.common_tags,
    map(
      "environment", var.env,
      "managedBy", var.team_name,
      "Team Contact", var.team_contact,
      "lastUpdated", timestamp()
    )
  )
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location
  tags     = local.tags
}

data "azurerm_user_assigned_identity" "et-identity" {
  name                = "${var.product}-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.product}-${var.component}-POSTGRES-HOST"
  value        = module.db.host_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.product}-${var.component}-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.product}-${var.component}-POSTGRES-DATABASE"
  value        = module.db.postgresql_database
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.product}-${var.component}-POSTGRES-USER"
  value        = module.db.user_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.product}-${var.component}-POSTGRES-PASS"
  value        = module.db.postgresql_password
  key_vault_id = module.key-vault.key_vault_id
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "et_msg_handler_s2s_key" {
  name         = "microservicekey-et-msg-handler"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "et_msg_handler_s2s_secret" {
  name         = "et-msg-handler-s2s-secret"
  value        = data.azurerm_key_vault_secret.et_msg_handler_s2s_key.value
  key_vault_id = module.key-vault.key_vault_id
}
