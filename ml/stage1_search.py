import json
import re

import faiss
from rapidfuzz import fuzz
from sentence_transformers import SentenceTransformer

from query_parser import parse_query


PRODUCTS_FILE = "data/products.json"
INDEX_FILE = "data/products.index"

SEMANTIC_K = 50
FUZZY_K = 50
ATTRIBUTE_K = 50
FINAL_K = 30


with open(PRODUCTS_FILE, "r", encoding="utf-8") as f:
    products = json.load(f)


model = SentenceTransformer(
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

index = faiss.read_index(INDEX_FILE)


def normalize_text(text):
    if not text:
        return ""

    text = str(text).lower()

    replacements = {
        "трехфазный": "3ф",
        "трёхфазный": "3ф",
        "трехфазной": "3ф",
        "трёхфазной": "3ф",
        "трехфазную": "3ф",
        "трёхфазную": "3ф",
        "трехполюсный": "3p",
        "трёхполюсный": "3p",
        "трехполюсной": "3p",
        "трёхполюсной": "3p",
        "ампер": "а",
        "ампера": "а",
        "амперa": "а",
        "вольт": "в",
        "вольта": "в",
        "автоматический выключатель": "автомат",
        "автоматического выключателя": "автомат",
        "авт. выкл.": "автомат",
        "авт.выкл.": "автомат",
        "авт. выкл": "автомат",
        "авт.выкл": "автомат"
    }

    for old, new in replacements.items():
        text = text.replace(old, new)

    text = text.replace("ё", "е")
    text = re.sub(r"\s+", " ", text)

    return text.strip()


def tokenize(text):
    text = normalize_text(text)

    return set(
        re.findall(
            r"[a-zа-я0-9]+",
            text,
            flags=re.IGNORECASE
        )
    )


def lexical_score(query, product):
    query_tokens = tokenize(query)

    name = product.get("name", "")
    article = product.get("article", "")

    product_text = f"{name} {article}"
    product_tokens = tokenize(product_text)

    if not query_tokens:
        return 0.0

    matches = query_tokens.intersection(product_tokens)

    return len(matches) / len(query_tokens)


def fuzzy_candidates(query, top_k=FUZZY_K):
    normalized_query = normalize_text(query)

    results = []

    for idx, product in enumerate(products):
        name = normalize_text(
            product.get("name", "")
        )

        article = normalize_text(
            product.get("article", "")
        )

        name_score = fuzz.WRatio(
            normalized_query,
            name
        ) / 100.0

        article_score = fuzz.WRatio(
            normalized_query,
            article
        ) / 100.0

        score = max(
            name_score,
            article_score
        )

        results.append(
            (score, idx)
        )

    results.sort(
        key=lambda x: x[0],
        reverse=True
    )

    return results[:top_k]


def semantic_candidates(query, top_k=SEMANTIC_K):
    query_embedding = model.encode(
        [query],
        normalize_embeddings=True
    )

    scores, indices = index.search(
        query_embedding,
        top_k
    )

    results = []

    for score, idx in zip(
        scores[0],
        indices[0]
    ):
        if idx < 0:
            continue

        results.append(
            (float(score), int(idx))
        )

    return results


def exact_attribute_bonus(parsed, product):
    name = normalize_text(
        product.get("name", "")
    )

    score = 0.0
    reasons = []

    if parsed["brand"]:
        brand = normalize_text(
            parsed["brand"]
        )

        if brand in name:
            score += 1.0
            reasons.append("brand")

    if parsed["current"] is not None:
        current = parsed["current"]

        if float(current).is_integer():
            current = int(current)

        current_string = re.escape(
            str(current)
        )

        patterns = [
            rf"(?<![\d]){current_string}\s*а\b",
            rf"(?<![\d]){current_string}\s*a\b"
        ]

        if any(
            re.search(pattern, name)
            for pattern in patterns
        ):
            score += 1.5
            reasons.append("current")

    if parsed["poles"] is not None:
        poles = int(parsed["poles"])

        patterns = [
            rf"\b{poles}\s*p\b",
            rf"\b{poles}\s*п\b",
            rf"\b{poles}\s*ф\b"
        ]

        if any(
            re.search(pattern, name)
            for pattern in patterns
        ):
            score += 1.0
            reasons.append("poles")

    if parsed["voltage"] is not None:
        voltage = parsed["voltage"]

        if float(voltage).is_integer():
            voltage = int(voltage)

        voltage_string = re.escape(
            str(voltage)
        )

        patterns = [
            rf"(?<![\d]){voltage_string}\s*в\b",
            rf"(?<![\d]){voltage_string}\s*v\b"
        ]

        if any(
            re.search(pattern, name)
            for pattern in patterns
        ):
            score += 0.75
            reasons.append("voltage")

    if parsed["breaking_capacity"] is not None:
        capacity = parsed["breaking_capacity"]

        if float(capacity).is_integer():
            capacity = int(capacity)

        capacity_string = re.escape(
            str(capacity)
        )

        patterns = [
            rf"(?<![\d]){capacity_string}\s*ka\b",
            rf"(?<![\d]){capacity_string}\s*ка\b"
        ]

        if any(
            re.search(pattern, name)
            for pattern in patterns
        ):
            score += 0.75
            reasons.append("capacity")

    return score, reasons


def attribute_candidates(parsed, top_k=ATTRIBUTE_K):
    results = []

    for idx, product in enumerate(products):
        score, reasons = exact_attribute_bonus(
            parsed,
            product
        )

        if score > 0:
            results.append(
                (score, idx, reasons)
            )

    results.sort(
        key=lambda x: x[0],
        reverse=True
    )

    return results[:top_k]


def stage1_search(query, top_k=FINAL_K):
    parsed = parse_query(query)

    fuzzy = fuzzy_candidates(query)
    semantic = semantic_candidates(query)
    attributes = attribute_candidates(parsed)

    candidates = {}

    def ensure_candidate(idx):
        if idx not in candidates:
            candidates[idx] = {
                "fuzzy_score": 0.0,
                "semantic_score": 0.0,
                "attribute_candidate_score": 0.0
            }

    for score, idx in fuzzy:
        ensure_candidate(idx)
        candidates[idx]["fuzzy_score"] = score

    for score, idx in semantic:
        ensure_candidate(idx)
        candidates[idx]["semantic_score"] = score

    for score, idx, reasons in attributes:
        ensure_candidate(idx)
        candidates[idx]["attribute_candidate_score"] = score

    results = []

    for idx, scores in candidates.items():
        product = products[idx]

        lexical = lexical_score(
            query,
            product
        )

        attribute_bonus, reasons = (
            exact_attribute_bonus(
                parsed,
                product
            )
        )

        final_score = (
            scores["fuzzy_score"] * 0.30
            + scores["semantic_score"] * 0.30
            + lexical * 0.40
            + attribute_bonus
        )

        results.append({
            "id": product["id"],
            "name": product["name"],
            "article": product.get("article"),
            "price": product.get("price"),
            "final_score": final_score,
            "fuzzy_score": scores["fuzzy_score"],
            "semantic_score": scores["semantic_score"],
          