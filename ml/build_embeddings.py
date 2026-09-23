import json
import numpy as np
import faiss

from sentence_transformers import SentenceTransformer

with open("data/products.json", "r", encoding="utf-8") as f:
    products = json.load(f)

model = SentenceTransformer(
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

texts = []

for product in products:
    name = product.get("name", "")
    article = product.get("article", "")

    text = f"{name} Артикул: {article}"
    texts.append(text)

print(f"Products: {len(texts)}")
print("Creating embeddings...")

embeddings = model.encode(
    texts,
    batch_size=64,
    show_progress_bar=True,
    normalize_embeddings=True
)

embeddings = np.asarray(embeddings, dtype="float32")

index = faiss.IndexFlatIP(embeddings.shape[1])
index.add(embeddings)

faiss.write_index(index, "data/products.index")

with open("data/product_metadata.json", "w", encoding="utf-8") as f:
    json.dump(products, f, ensure_ascii=False)

print(f"Indexed: {index.ntotal}")
print(f"Embedding dimension: {embeddings.shape[1]}")
