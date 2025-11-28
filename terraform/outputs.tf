##############################
# EC2 Instance Outputs
##############################

output "ec2_instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.my_ec2.id
}

output "ec2_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.my_ec2.public_ip
}

output "ec2_public_dns" {
  description = "Public DNS of the EC2 instance"
  value       = aws_instance.my_ec2.public_dns
}

output "ec2_arn" {
  description = "ARN of the EC2 instance"
  value       = aws_instance.my_ec2.arn
}


##############################
# IAM Outputs
##############################

output "iam_role_name" {
  description = "IAM role name attached to EC2"
  value       = aws_iam_role.ec2_role.name
}

output "iam_instance_profile" {
  description = "IAM instance profile name"
  value       = aws_iam_instance_profile.ec2_profile.name
}


##############################
# Security Group Outputs
##############################

output "security_group_id" {
  description = "Security group ID for the EC2 instance"
  value       = aws_security_group.ec2_sg.id
}


##############################
# S3 Bucket Outputs
##############################

output "s3_bucket_name" {
  description = "Name of the created S3 bucket"
  value       = aws_s3_bucket.my_bucket.bucket
}

output "s3_bucket_arn" {
  description = "ARN of the created S3 bucket"
  value       = aws_s3_bucket.my_bucket.arn
}

output "s3_bucket_domain_name" {
  description = "Domain name (URL endpoint) of the S3 bucket"
  value       = aws_s3_bucket.my_bucket.bucket_domain_name
}
