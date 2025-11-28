provider "aws" {
  region = var.region
}

# Auto-generated tags
locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

module "ec2" {
  source        = "./modules/ec2"
  ami_id        = var.ami_id
  instance_type = var.instance_type
  vpc_id        = var.vpc_id
  tags          = local.common_tags
}

module "s3" {
  source      = "./modules/s3"
  bucket_name = var.bucket_name
  tags        = local.common_tags
}
