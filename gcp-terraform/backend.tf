terraform {
  backend "gcs" {
    bucket = "my-terraform-state-bala-bucket"  # Replace this with your actual bucket name
    prefix = "terraform/state"
  }
}

