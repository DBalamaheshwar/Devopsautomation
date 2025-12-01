provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

locals {
  common_labels = {
    project     = var.project_name
    environment = var.environment
    managed_by  = "terraform"
  }
}

module "network" {
  source = "./modules/network"

  project_id = var.project_id
  region     = var.region
  labels     = local.common_labels
}

module "compute" {
  source = "./modules/compute"

  project_id        = var.project_id
  region            = var.region
  zone              = var.zone
  machine_type      = var.vm_machine_type
  network_self_link = module.network.network_self_link
  subnet_self_link  = module.network.subnet_self_link
  startup_script    = var.vm_startup_script
  labels            = local.common_labels
}

module "storage" {
  source      = "./modules/storage"
  bucket_name = var.bucket_name
  labels      = local.common_labels
}


resource "google_logging_project_sink" "project_logs" {
  name        = "project-log-sink"
  destination = "storage.googleapis.com/${var.project_name}-logs"
  filter      = "severity>=ERROR"

  unique_writer_identity = true
}

# Example of insecure setting (will trigger high severity issues)
resource "google_compute_firewall" "allow_all" {
  name    = "allow-all-traffic"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }

  source_ranges = ["0.0.0.0/0"]  # This exposes the firewall to the entire internet (high severity)
}

resource "google_storage_bucket" "msy_bucket" {
  name     = "my-unsafe-sbucket"
  location = "US"
}  # Missing encryption (medium severity)




