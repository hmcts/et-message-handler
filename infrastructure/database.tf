data "azurerm_subnet" "postgres" {
  name                 = "core-infra-subnet-0-${var.env}"
  resource_group_name  = "core-infra-${var.env}"
  virtual_network_name = "core-infra-vnet-${var.env}"
}

module "db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=postgresql_tf"
  product            = var.product
  component          = var.component
  location           = var.location_db
  env                = var.env
  postgresql_user    = "et_msg_handler"
  database_name      = "et_msg_handler"
  postgresql_version = "11"
  subnet_id          = data.azurerm_subnet.postgres.id
  common_tags        = local.tags
  subscription       = var.subscription
}
