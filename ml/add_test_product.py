import os
import json
import requests
from dotenv import load_dotenv

load_dotenv()

username = os.getenv("EKT_USERNAME")
password = os.getenv("EKT_PASSWORD")

with open("data/product_details.json", "r", encoding="utf-8") as f:
    products = json.load(f)

product_id = 515279

if any(product["id"] == product_id for product in products):
    print("Product already exists")
    exit()

response = requests.get(
    "https://ekt.kz/api/products/detail",
    params={"id": product_id},
    auth=(username, password),
    timeout=60
)

response.raise_for_status()

products.append(response.json())

with open("data/product_details.json", "w", encoding="utf-8") as f:
    json.dump(products, f, ensure_ascii=False, indent=2)

print(f"Added product {product_id}")
print(f"Total: {len(products)}")
