resource "aws_iam_role" "ec2_role" {
  name = "ec2-basic-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = {
        Service = "ec2.amazonaws.com"
      },
      Action = "sts:AssumeRole"
    }]
  })
}

#EC2 Instance

resource "aws_instance" "my_ec2" {
  ami           = "ami-0c02fb55956c7d316" # Replace with latest Amazon Linux 2 AMI
  instance_type = "t2.micro"

  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name
  security_groups      = [aws_security_group.ec2_sg.name]

  key_name = "my-key" # Replace with your key pair

  tags = {
    Name = "Terraform-EC2"
  }
}
