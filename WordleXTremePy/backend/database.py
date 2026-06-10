import os
from mysql.connector import pooling
from dotenv import load_dotenv

class Database:
    def __init__(self):
        self.pool = None
        load_dotenv()

    def connect(self):
        self.pool = pooling.MySQLConnectionPool(
            pool_name="conn_pool",
            pool_size=5,
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USERNAME'),
            password=os.getenv('DP_PASSWORD'),
            database=os.getenv('DATABASE_NAME'),
            port=3306
        )
        print("Database connection pool successful!")

    def get_connection(self):
        return self.pool.get_connection()

    def fetch(self, query, *args):
        """Execute a query (SELECT, ?)."""
        connection = self.get_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(query, args)
        result = cursor.fetchall()
        cursor.close()
        connection.close()
        return result

    def execute(self, query, *args):
        """Execute a query (INSERT, UPDATE, DELETE)."""
        connection = self.get_connection()
        cursor = connection.cursor()
        cursor.execute(query, args)
        connection.commit()
        cursor.close()
        connection.close()

    def close(self):
        if self.pool:
            self.pool._cnx.close()
            print("Database connection pool closed!")
db = Database()
db.connect()