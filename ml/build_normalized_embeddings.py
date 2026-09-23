import json
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer

with open("data/normalized_products.json", "r", encoding="utf-8") as f:
    products = json.load(f)

model = SentenceTransformer(
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

texts = [product["search_text"] for product in products]

print(f"Products: {len(texts)}")
print("Creating normalized embeddings...")

embeddings = model.encode(
    texts,
    batch_size=64,
    show_progress_bar=True,
    normalize_embeddings=True
)

embeddings = np.asarray(embeddings, dtype="float32")

index = faiss.IndexFlatIP(embeddings.shape[1])
index.add(embeddings)

faiss.write_index(
    index,
    "data/normalized_products.index"
)

print(f"Indexed: {index.ntotal}")
