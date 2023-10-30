variable "product" {
  default = "et"
}

variable "component" {
  default = "msg-handler"
}

variable "location" {
  default = "UK South"
}

variable "location_db" {
  default = "UK South"
}

variable "env" {
}

variable "subscription" {
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "deployment_namespace" {
  default = ""
}

variable "common_tags" {
  type = map(string)
}

variable "team_name" {
  description = "Team name"
  default     = "DTS Employment Tribunals"
}

variable "team_contact" {
  description = "Team contact"
  default     = "#et-tech"
}

variable "aks_subscription_id" {}
