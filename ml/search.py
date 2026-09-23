import json
from rapidfuzz import fuzz

with open("data/products.json", "r", encoding="utf-8") as f:
    products = json.load(f)


def search_products(query, top_k=5):
    query = query.lower()

    results = []

    for product in products:
        name = product.get("name", "").lower()
        article = product.get("article", "").lower()

        name_score = fuzz.WRatio(query, name)
        article_score = fuzz.WRatio(query, article)

        score = max(name_score, article_score)

        results.append((score, product))

    results.sort(key=lambda x: x[0], reverse=True)

    return results[:top_k]


query = input("Search: ")

results = search_products(query)

for i, (score, product) in enumerate(results, 1):
    print()
    print(f"{i}. Score: {score:.2f}")
    print(f"ID: {product['id']}")
    print(f"Name: {product['name']}")
    print(f"Article: {product['article']}")
    print(f"Price: {product['price']}")
