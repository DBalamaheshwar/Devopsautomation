# Use official Python 3.11 slim image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy requirements first (better caching)
COPY app/requirements.txt /app/requirements.txt

# Install Python dependencies
RUN pip install --no-cache-dir -r /app/requirements.txt

# Copy the entire application code
COPY app/ /app/

# Expose both ports (Flask=5000, FastAPI=8000)
EXPOSE 5000
EXPOSE 8000

# -------- choose which app to run ---------
# Uncomment ONE based on what you want:

# ▶ Run Flask (app.py on port 5000)
# CMD ["python", "app.py"]

# ▶ Run FastAPI (main.py on port 8000)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
