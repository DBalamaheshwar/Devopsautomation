from sqlalchemy import create_engine

DB_URL = "postgresql://postgres:password@postgres:5432/mydb"
engine = create_engine(DB_URL)
