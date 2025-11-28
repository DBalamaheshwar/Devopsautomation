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

  labels = var.labels
}

resource "google_storage_bucket" "log_bucket" {
  name          = "${var.bucket_name}-logs"
  location      = "US"
  force_destroy = true

  labels = var.labels
}


resource "google_storage_bucket" "bucket" {
  name                        = var.bucket_name
  location                    = "US"
  uniform_bucket_level_access = true

  logging {
    log_bucket        = google_storage_bucket.log_bucket.name
    log_object_prefix = "access_logs"
  }

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

  labels = var.labels
}
