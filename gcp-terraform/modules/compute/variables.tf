variable "project_id" {}
variable "region" {}
variable "zone" {}
variable "machine_type" {}
variable "network_self_link" {}
variable "subnet_self_link" {}
variable "startup_script" {}
variable "labels" {
  type = map(string)
}
