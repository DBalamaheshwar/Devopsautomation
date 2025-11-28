output "instance_id" {
  value = aws_instance.ec2.id
}

output "public_ip" {
  value = aws_instance.ec2.public_ip
}

output "public_dns" {
  value = aws_instance.ec2.public_dns
}

output "security_group_id" {
  value = aws_security_group.ec2_sg.id
}
