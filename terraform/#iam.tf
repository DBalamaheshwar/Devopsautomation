resource "aws_iam_policy" "ec2_policy" {
  name        = "ec2-basic-policy"
  description = "Policy for EC2 instance to access S3"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Action = [
        "s3:ListBucket",
        "s3:GetObject"
      ],
      Resource = "*"
    }]
  })
}


resource "aws_iam_role_policy_attachment" "ec2_role_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.ec2_policy.arn
}



resource "aws_iam_instance_profile" "ec2_profile" {
  name = "ec2-basic-instance-profile"
  role = aws_iam_role.ec2_role.name
}


resource "aws_security_group" "ec2_sg" {
  name        = "ec2-basic-sg"
  description = "Allow SSH and HTTP"
  vpc_id      = "vpc-12345678" # replace with your VPC ID

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

