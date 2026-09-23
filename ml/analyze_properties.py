import json
from collections import Counter

with open("data/product_details.json", "r", encoding="utf-8") as f:
    products = json.load(f)

keys = Counter()

for product in products:
    properties = product.get("properties", {})

    for key in properties:
        keys[key] += 1

print(f"Products: {len(products)}")
print(f"Unique properties: {len(keys)}")
print()

for key, count in keys.most_common():
    print(f"{count:4}  {key}")
