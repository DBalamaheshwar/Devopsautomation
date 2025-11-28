terraform {
  backend "gcs" {
    bucket = "my-terraform-state-bucket"
    prefix = "env/dev"
  }
}
