FROM python:3.11-slim

WORKDIR /app
COPY app/ /app/
COPY app/requirements.txt .
#RUN pip install -r requirements.txt

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]


# Base image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy requirements
#COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY . .

# Expose port
EXPOSE 5000

# Start the app
CMD ["python", "app.py"]
