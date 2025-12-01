project_id     = "perfect-reducer-477806-j9"
region         = "central1"
zone           = "us-central1-a"
project_name   = "My First Project"
environment    = "dev"

vm_machine_type = "e2-small"
bucket_name     = "my-terraform-state-bala-bucket"

vm_startup_script = <<EOF
#!/bin/bash
echo "Hello from Terraform Startup Script" > /startup.txt
EOF
