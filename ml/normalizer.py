import json
import re


def number(value):
    if value is None:
        return None

    match = re.search(r"\d+(?:[.,]\d+)?", str(value))

    if not match:
        return None

    return float(match.group().replace(",", "."))


def first_property(properties, keys):
    for key in keys:
        value = properties.get(key)

        if value not in [None, ""]:
            return value

    return None


def extract_current_from_text(text):
    patterns = [
        r"(\d+(?:[.,]\d+)?)\s*[аa]\b",
        r"(\d+(?:[.,]\d+)?)\s*ампер"
    ]

    for pattern in patterns:
        match = re.search(pattern, text.lower())

        if match:
            return float(match.group(1).replace(",", "."))

    return None


def extract_poles_from_text(text):
    text = text.lower()

    patterns = [
        r"\b([1-4])\s*p\b",
        r"\b([1-4])\s*п\b",
        r"\b([1-4])\s*ф\b"
    ]

    for pattern in patterns:
        match = re.search(pattern, text)

        if match:
            return int(match.group(1))

    return None


def normalize_product(product):
    properties = product.get("properties", {})

    name = product.get("name", "")
    description = product.get("description", "")

    combined_text = f"{name} {description}"

    brand = first_property(
        properties,
        [
            "TORGOVAYA_MARKA"
        ]
    )

    property_current = number(
        first_property(
            properties,
            [
                "NOMINALNYY_TOK",
                "NOMINALNYY_TOK_A_1",
                "NOMINALNYY_TOK_3",
                "NOMIN_TOK_A"
            ]
        )
    )

    text_current = extract_current_from_text(combined_text)

    poles = number(
        first_property(
            properties,
            [
                "KOLICHESTVO_POLYUSOV",
                "KOLLICHESTVO_POLYUSOV"
            ]
        )
    )

    if poles is None:
        poles = extract_poles_from_text(combined_text)

    voltage = number(
        first_property(
            properties,
            [
                "NOMINALNOE_NAPRYAZHENIE",
                "NOMIN_RAB_NAPRYAZHENIE_V",
                "NOMIN_RAB_NAPRYAZHENIE_V_1"
            ]
        )
    )

    breaking_capacity = number(
        properties.get(
            "NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST"
        )
    )

    trip_curve = properties.get(
        "KHARAKTERISTIKA_SRABATYVANIYA"
    )

    series = first_property(
        properties,
        [
            "SERIYA",
            "SERIYA_1"
        ]
    )

    conflicts = []

    if (
        property_current is not None
        and text_current is not None
        and property_current != text_current
    ):
        conflicts.append({
            "field": "current",
            "property_value": property_current,
            "text_value": text_current
        })

    current = text_current or property_current

    search_parts = [
        name,
        description,
        brand,
        f"{current} ампер" if current is not None else None,
        f"{int(poles)} полюса" if poles is not None else None,
        f"{voltage} вольт" if voltage is not None else None,
        f"{breaking_capacity} кА"
        if breaking_capacity is not None else None,
        trip_curve,
        series
    ]

    search_text = " ".join(
        str(x) for x in search_parts if x
    )

    return {
        "id": product.get("id"),
        "name": name,
        "article": product.get("article"),
        "brand": brand,
        "current": current,
        "poles": poles,
        "voltage": voltage,
        "breaking_capacity": breaking_capacity,
        "trip_curve": trip_curve,
        "series": series,
        "price": product.get("price"),
        "quantity": product.get("quantity"),
        "stores": product.get("stores", []),
        "url": product.get("url"),
        "search_text": search_text,
        "conflicts": conflicts
    }


with open(
    "data/product_details.json",
    "r",
    encoding="utf-8"
) as f:
    products = json.load(f)

normalized = [
    normalize_product(product)
    for product in products
]

with open(
    "data/normalized_products.json",
    "w",
    encoding="utf-8"
) as f:
    json.dump(
        normalized,
        f,
        ensure_ascii=False,
        indent=2
    )

conflict_count = sum(
    1 for product in normalized
    if product["conflicts"]
)

print(f"Normalized: {len(normalized)}")
print(f"Products with conflicts: {conflict_count}")
