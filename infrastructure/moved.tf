moved {
  from = module.key-vault
  to   = module.key-vault[0]
}

moved {
  from = azurerm_resource_group.rg
  to   = azurerm_resource_group.rg[0]
}
