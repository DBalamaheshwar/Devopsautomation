resource "google_storage_bucket" "bucket" {
  name                        = var.bucket_name
  location                    = "US"
  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = 30
    }
  }

  logging {
    log_bucket        = google_storage_bucket.log_bucket.name
    log_object_prefix = "access_logs"
  }

  labels = var.labels
}

resource "google_storage_bucket" "log_bucket" {
  name          = "${var.bucket_name}-logs"
  location      = "US"
  force_destroy = true

  labels = var.labels
}

resource "google_storage_bucket" "bucket2" {  # Renamed to "bucket2"
  name     = "my-bucket-1"
  location = "US"
}

resource "google_storage_bucket" "bucket3" {  # Renamed to "bucket3"
  name     = "my-bucket-2"
  location = "EU"
}

