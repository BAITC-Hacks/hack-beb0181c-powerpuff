import os
import json
import time
import requests
from dotenv import load_dotenv
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

load_dotenv()

username = os.getenv("EKT_USERNAME")
password = os.getenv("EKT_PASSWORD")

url = "https://ekt.kz/api/products"
output_file = "data/products.json"

os.makedirs("data", exist_ok=True)

if os.path.exists(output_file):
    with open(output_file, "r", encoding="utf-8") as f:
        products = json.load(f)
else:
    products = []

existing_ids = {product["id"] for product in products}

page = len(products) // 20 + 1

session = requests.Session()

retry = Retry(
    total=5,
    connect=5,
    read=5,
    backoff_factor=2,
    status_forcelist=[429, 500, 502, 503, 504],
    allowed_methods=["GET"]
)

session.mount("https://", HTTPAdapter(max_retries=retry))

while True:
    print(f"Downloading page {page}...")

    try:
        response = session.get(
            url,
            params={"page": page},
            auth=(username, password),
            timeout=30
        )

        response.raise_for_status()
        data = response.json()
        items = data.get("items", [])

        if not items:
            print("No more products.")
            break

        new_items = []

        for item in items:
            if item["id"] not in existing_ids:
                new_items.append(item)
                existing_ids.add(item["id"])

        products.extend(new_items)

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(products, f, ensure_ascii=False, indent=2)

        print(
            f"Received: {len(items)}, "
            f"new: {len(new_items)}, "
            f"total: {len(products)}"
        )

        if len(items) < data.get("per_page", 20):
            break

        page += 1
        time.sleep(0.1)

    except requests.exceptions.RequestException as e:
        print(f"Error on page {page}: {e}")
        print("Waiting 10 seconds...")
        time.sleep(10)

print(f"Finished. Total products: {len(products)}")