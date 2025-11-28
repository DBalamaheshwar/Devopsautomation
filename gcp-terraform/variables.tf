variable "project_id" {
  type        = string
  description = "GCP project ID"
}

variable "region" {
  type        = string
  default     = "us-central1"
}

variable "zone" {
  type        = string
  default     = "us-central1-a"
}

variable "project_name" {
  type        = string
  description = "Project name for tags"
}

variable "environment" {
  type        = string
}

variable "vm_machine_type" {
  type        = string
  default     = "e2-micro"
}

variable "bucket_name" {
  type = string
}

variable "vm_startup_script" {
  type        = string
  description = "Startup script for VM instance"
}
