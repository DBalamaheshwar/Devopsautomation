from fastapi import FastAPI
from app.database import engine
from app import models

app = FastAPI()

@app.get("/")
def home():
    return {"message": "Python Web App CI/CD Running!"}
