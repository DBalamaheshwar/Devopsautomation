resource "aws_s3_bucket" "my_bucket" {
  bucket = "my-terraform-demo-bucket-12345"

  tags = {
    Name = "Terraform-S3"
  }
}

#Enable Versioning
resource "aws_s3_bucket_versioning" "bucket_versioning" {
  bucket = aws_s3_bucket.my_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

#Block Public Access
resource "aws_s3_bucket_public_access_block" "block_public_access" {
  bucket = aws_s3_bucket.my_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
