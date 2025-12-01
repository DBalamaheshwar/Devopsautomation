resource "google_compute_network" "vpc" {
  name                    = "project-vpc"
  auto_create_subnetworks  = false
  routing_mode             = "REGIONAL"
  project                 = var.project_id
}

# First subnet with log configuration
resource "google_compute_subnetwork" "subnet" {
  name          = "project-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc.self_link

  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 0.5
    metadata             = "INCLUDE_ALL_METADATA"
  }

}

# Second subnet with different IP range
resource "google_compute_subnetwork" "subnet_1" {  # Renamed to "subnet_1"
  name          = "subnet-1"
  network       = google_compute_network.vpc.self_link  # Corrected reference
  ip_cidr_range = "10.0.1.0/24"
  region        = var.region
}

# Third subnet with a different IP range
resource "google_compute_subnetwork" "subnet_2" {  # Renamed to "subnet_2"
  name          = "subnet-2"
  network       = google_compute_network.vpc.self_link  # Corrected reference
  ip_cidr_range = "10.0.2.0/24"
  region        = var.region
}

# Allow SSH rule for VPC
resource "google_compute_firewall" "allow_ssh" {
  name    = "allow-ssh"
  network = google_compute_network.vpc.self_link

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
}

# Allow HTTP rule for VPC
resource "google_compute_firewall" "allow_http" {
  name    = "allow-http"
  network = google_compute_network.vpc.self_link

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = ["0.0.0.0/0"]
}

