import os
import time
import requests

from dotenv import load_dotenv

load_dotenv()

USERNAME = os.getenv("EKT_USERNAME")
PASSWORD = os.getenv("EKT_PASSWORD")

DETAIL_URL = "https://ekt.kz/api/products/detail"


def get_product_detail(product_id, retries=3):
    for attempt in range(retries):
        try:
            response = requests.get(
                DETAIL_URL,
                params={"id": product_id},
                auth=(USERNAME, PASSWORD),
                timeout=20
            )

            response.raise_for_status()

            return response.json()

        except requests.exceptions.RequestException:
            if attempt == retries - 1:
                return None

            time.sleep(2 * (attempt + 1))

    return None


def get_product_details(product_ids):
    results = []

    for i, product_id in enumerate(
        product_ids,
        start=1
    ):
        print(
            f"Detail {i}/{len(product_ids)}: "
            f"{product_id}"
        )

        detail = get_product_detail(
            product_id
        )

        if detail is not None:
            results.append(detail)

    return results
