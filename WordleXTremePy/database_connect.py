import os

import mysql.connector

from dotenv import load_dotenv

# Replace with your Sparked Host details
load_dotenv()
HOST = os.getenv('DB_HOST')
USER = os.getenv('DB_USERNAME')
PASSWORD = os.getenv('DP_PASSWORD')
DATABASE = os.getenv('DATABASE_NAME')

try:
    # Establish the connection
    conn = mysql.connector.connect(
        host=HOST,
        user=USER,
        password=PASSWORD,
        database=DATABASE  # Omit if you want to connect to the server without specifying a DB
    )
    print("Connection successful!")

    # Example query
    cursor = conn.cursor()
    cursor.execute("SHOW TABLES;")
    for table in cursor.fetchall():
        print(table)

    # Close the connection
    conn.close()
except mysql.connector.Error as e:
    print(f"Error: {e}")
