import json
import faiss

from sentence_transformers import SentenceTransformer

model = SentenceTransformer(
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

index = faiss.read_index("data/products.index")

with open("data/product_metadata.json", "r", encoding="utf-8") as f:
    products = json.load(f)


def search_products(query, top_k=5):
    query_embedding = model.encode(
        [query],
        normalize_embeddings=True
    )

    scores, indices = index.search(query_embedding, top_k)

    results = []

    for score, idx in zip(scores[0], indices[0]):
        results.append((float(score), products[idx]))

    return results


query = input("Search: ")

results = search_products(query)

for i, (score, product) in enumerate(results, 1):
    print()
    print(f"{i}. Score: {score:.4f}")
    print(f"ID: {product['id']}")
    print(f"Name: {product['name']}")
    print(f"Article: {product['article']}")
    print(f"Price: {product['price']}")
