import os
import json
import time
import requests
from dotenv import load_dotenv

load_dotenv()

username = os.getenv("EKT_USERNAME")
password = os.getenv("EKT_PASSWORD")

with open("data/products.json", "r", encoding="utf-8") as f:
    products = json.load(f)

keywords = [
    "автомат",
    "авт.",
    "авт ",
    "авт.выкл",
    "автоматический выключатель"
]

candidates = []

for product in products:
    name = product.get("name", "").lower()

    if any(keyword in name for keyword in keywords):
        candidates.append(product)

print(f"Candidates found: {len(candidates)}")

details = []

for i, product in enumerate(candidates[:500], 1):
    product_id = product["id"]

    print(f"{i}/{min(len(candidates), 500)}: {product_id}")

    try:
        response = requests.get(
            "https://ekt.kz/api/products/detail",
            params={"id": product_id},
            auth=(username, password),
            timeout=30
        )

        response.raise_for_status()

        details.append(response.json())

        with open(
            "data/product_details.json",
            "w",
            encoding="utf-8"
        ) as f:
            json.dump(
                details,
                f,
                ensure_ascii=False,
                indent=2
            )

        time.sleep(0.1)

    except requests.exceptions.RequestException as e:
        print(f"Error: {e}")

print(f"Downloaded: {len(details)}")
