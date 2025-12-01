resource "google_service_account" "vm_sa" {
  account_id   = "vm-service-account"
  display_name = "VM Service Account"
}



resource "google_project_iam_member" "sa_storage_admin" {
  project = var.project_id  # This is where the project ID is used
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.vm_sa.email}"
}


resource "google_compute_instance" "vm" {
  name         = "terraform-gcp-vm"
  machine_type = var.machine_type
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-11"
    }
  }

  network_interface {
    network    = var.network_self_link
    subnetwork = var.subnet_self_link
    access_config {}
  }

  metadata = {
    startup-script = <<EOF
#! /bin/bash

# Install google logging agent
curl -sSO https://dl.google.com/cloudagents/add-google-cloud-ops-agent-repo.sh
sudo bash add-google-cloud-ops-agent-repo.sh --also-install

echo "Startup script executed at: $(date)" >> /var/log/startup.log
EOF
  }

  service_account {
    email  = google_service_account.vm_sa.email
    scopes = ["cloud-platform"]
  }

  labels = var.labels
}

