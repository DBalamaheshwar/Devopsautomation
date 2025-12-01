
variable "project_name" {
  description = "Project name for tagging"
  type        = string
}

variable "region" {
  description = "The AWS region where resources will be deployed."
  type        = string
  default     = "us-east-1"  # You can set a default region here, or leave it out if it will be provided at runtime
}

variable "environment" {
  description = "Environment (dev, qa, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID for the EC2 security group"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "ami_id" {
  description = "AMI ID for EC2"
  type        = string
}

variable "bucket_name" {
  description = "Name of S3 bucket"
  type        = string
}
