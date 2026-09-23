import json
import faiss
from sentence_transformers import SentenceTransformer
from query_parser import parse_query

model = SentenceTransformer(
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

index = faiss.read_index(
    "data/normalized_products.index"
)

with open(
    "data/normalized_products.json",
    "r",
    encoding="utf-8"
) as f:
    products = json.load(f)


def same_number(a, b, tolerance=0.01):
    if a is None or b is None:
        return False

    return abs(float(a) - float(b)) <= tolerance


def attribute_score(parsed, product):
    score = 0.0
    reasons = []

    if parsed["brand"]:
        product_brand = product.get("brand")

        if (
            product_brand
            and parsed["brand"].lower()
            == product_brand.lower()
        ):
            score += 1.0
            reasons.append("brand match")
        elif product_brand:
            score -= 0.5

    if parsed["current"] is not None:
        product_current = product.get("current")

        if same_number(
            parsed["current"],
            product_current
        ):
            score += 2.0
            reasons.append("current match")
        elif product_current is not None:
            score -= 1.5

    if parsed["poles"] is not None:
        product_poles = product.get("poles")

        if same_number(
            parsed["poles"],
            product_poles
        ):
            score += 1.5
            reasons.append("poles match")
        elif product_poles is not None:
            score -= 1.0

    if parsed["voltage"] is not None:
        product_voltage = product.get("voltage")

        if same_number(
            parsed["voltage"],
            product_voltage
        ):
            score += 1.0
            reasons.append("voltage match")
        elif product_voltage is not None:
            score -= 0.5

    if parsed["breaking_capacity"] is not None:
        product_capacity = product.get(
            "breaking_capacity"
        )

        if same_number(
            parsed["breaking_capacity"],
            product_capacity
        ):
            score += 1.0
            reasons.append("breaking capacity match")
        elif product_capacity is not None:
            score -= 0.5

    return score, reasons


def hybrid_search(query, top_k=5):
    parsed = parse_query(query)

    query_embedding = model.encode(
        [query],
        normalize_embeddings=True
    )

    semantic_scores, indices = index.search(
        query_embedding,
        len(products)
    )

    results = []

    for semantic_score, idx in zip(
        semantic_scores[0],
        indices[0]
    ):
        product = products[idx]

        attr_score, reasons = attribute_score(
            parsed,
            product
        )

        final_score = (
            float(semantic_score)
            + attr_score
        )

        results.append({
            "final_score": final_score,
            "semantic_score": float(semantic_score),
            "attribute_score": attr_score,
            "reasons": reasons,
            "product": product
        })

    results.sort(
        key=lambda x: x["final_score"],
        reverse=True
    )

    return parsed, results[:top_k]


if __name__ == "__main__":
    query = input("Search: ")

    parsed, results = hybrid_search(query)

    print()
    print("Parsed query:")
    print(parsed)

    for i, result in enumerate(results, 1):
        product = result["product"]

        print()
        print(f"{i}. {product['name']}")
        print(f"ID: {product['id']}")
        print(
            f"Final score: "
            f"{result['final_score']:.4f}"
        )
        print(
            f"Semantic: "
            f"{result['semantic_score']:.4f}"
        )
        print(
            f"Attributes: "
            f"{result['attribute_score']:.4f}"
        )
        print(
            f"Reasons: "
            f"{', '.join(result['reasons'])}"
        )
        print(
            f"Brand: {product['brand']} | "
            f"Current: {product['current']}A | "
            f"Poles: {product['poles']} | "
            f"Voltage: {product['voltage']}V | "
            f"Capacity: "
            f"{product['breaking_capacity']}kA"
        )
