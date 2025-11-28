project_id     = "my-gcp-project-123"
region         = "us-central1"
zone           = "us-central1-a"
project_name   = "terraform-gcp-project"
environment    = "dev"

vm_machine_type = "e2-small"
bucket_name     = "my-gcp-demo-bucket-123"

vm_startup_script = <<EOF
#!/bin/bash
echo "Hello from Terraform Startup Script" > /startup.txt
EOF
