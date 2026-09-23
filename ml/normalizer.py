import json
import re


def number(value):
    if value is None:
        return None

    match = re.search(
        r"\d+(?:[.,]\d+)?",
        str(value)
    )

    if not match:
        return None

    return float(
        match.group().replace(",", ".")
    )


def first_property(properties, keys):
    for key in keys:
        value = properties.get(key)

        if value not in [None, ""]:
            return value

    return None


def extract_current_from_text(text):
    if not text:
        return None

    patterns = [
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*[аa]\b",
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*ампер"
    ]

    text = text.lower()

    for pattern in patterns:
        match = re.search(pattern, text)

        if match:
            return float(
                match.group(1).replace(",", ".")
            )

    return None


def extract_poles_from_text(text):
    if not text:
        return None

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

    if re.search(r"тр[её]хфаз", text):
        return 3

    if re.search(r"двухфаз", text):
        return 2

    if re.search(r"однофаз", text):
        return 1

    if re.search(r"четыр[её]хполюс", text):
        return 4

    if re.search(r"тр[её]хполюс", text):
        return 3

    if re.search(r"двухполюс", text):
        return 2

    if re.search(r"однополюс", text):
        return 1

    return None


def extract_voltage_from_text(text):
    if not text:
        return None

    patterns = [
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*[вv]\b",
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*вольт"
    ]

    text = text.lower()

    for pattern in patterns:
        match = re.search(pattern, text)

        if match:
            return float(
                match.group(1).replace(",", ".")
            )

    return None


def extract_breaking_capacity_from_text(text):
    if not text:
        return None

    patterns = [
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*k\s*a\b",
        r"(?<![\d])(\d+(?:[.,]\d+)?)\s*к\s*[аa]\b"
    ]

    text = text.lower()

    for pattern in patterns:
        match = re.search(pattern, text)

        if match:
            return float(
                match.group(1).replace(",", ".")
            )

    return None


def normalize_brand(brand):
    if not brand:
        return None

    brand_text = str(brand).strip()
    brand_lower = brand_text.lower()

    brands = {
        "legrand": "Legrand",
        "schneider": "Schneider Electric",
        "schneider electric": "Schneider Electric",
        "iek": "IEK",
        "иэк": "IEK",
        "abb": "ABB",
        "dekraft": "DEKraft",
        "chint": "CHINT"
    }

    for keyword, normalized in brands.items():
        if keyword in brand_lower:
            return normalized

    return brand_text


def extract_brand_from_text(text):
    if not text:
        return None

    text = text.lower()

    brands = {
        "legrand": "Legrand",
        "schneider electric": "Schneider Electric",
        "schneider": "Schneider Electric",
        "iek": "IEK",
        "иэк": "IEK",
        "abb": "ABB",
        "dekraft": "DEKraft",
        "chint": "CHINT"
    }

    for keyword, normalized in brands.items():
        if keyword in text:
            return normalized

    return None


def add_conflict(
    conflicts,
    field,
    property_value,
    text_value
):
    if (
        property_value is not None
        and text_value is not None
        and property_value != text_value
    ):
        conflicts.append({
            "field": field,
            "property_value": property_value,
            "text_value": text_value
        })


def normalize_product(product):
    properties = product.get(
        "properties",
        {}
    ) or {}

    name = product.get(
        "name",
        ""
    ) or ""

    description = product.get(
        "description",
        ""
    ) or ""

    combined_text = (
        f"{name} {description}"
    ).strip()

    property_brand = first_property(
        properties,
        [
            "TORGOVAYA_MARKA"
        ]
    )

    brand = normalize_brand(
        property_brand
    )

    if brand is None:
        brand = extract_brand_from_text(
            combined_text
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

    text_current = extract_current_from_text(
        combined_text
    )

    property_poles = number(
        first_property(
            properties,
            [
                "KOLICHESTVO_POLYUSOV",
                "KOLLICHESTVO_POLYUSOV"
            ]
        )
    )

    text_poles = extract_poles_from_text(
        combined_text
    )

    property_voltage = number(
        first_property(
            properties,
            [
                "NOMINALNOE_NAPRYAZHENIE",
                "NOMIN_RAB_NAPRYAZHENIE_V",
                "NOMIN_RAB_NAPRYAZHENIE_V_1",
                "NAPRYAZHENIE_PITANIYA_V",
                "NAPRYAZHENIE_PITANIYA_V_1",
                "NAPRYAZHENIE_PITANIYA_V_2"
            ]
        )
    )

    text_voltage = extract_voltage_from_text(
        combined_text
    )

    property_breaking_capacity = number(
        first_property(
            properties,
            [
                "NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST"
            ]
        )
    )

    text_breaking_capacity = (
        extract_breaking_capacity_from_text(
            combined_text
        )
    )

    trip_curve = first_property(
        properties,
        [
            "KHARAKTERISTIKA_SRABATYVANIYA"
        ]
    )

    series = first_property(
        properties,
        [
            "SERIYA",
            "SERIYA_1"
        ]
    )

    conflicts = []

    add_conflict(
        conflicts,
        "current",
        property_current,
        text_current
    )

    add_conflict(
        conflicts,
        "poles",
        property_poles,
        text_poles
    )

    add_conflict(
        conflicts,
        "voltage",
        property_voltage,
        text_voltage
    )

    add_conflict(
        conflicts,
        "breaking_capacity",
        property_breaking_capacity,
        text_breaking_capacity
    )

    current = (
        text_current
        if text_current is not None
        else property_current
    )

    poles = (
        text_poles
        if text_poles is not None
        else property_poles
    )

    voltage = (
        text_voltage
        if text_voltage is not None
        else property_voltage
    )

    breaking_capacity = (
        text_breaking_capacity
        if text_breaking_capacity is not None
        else property_breaking_capacity
    )

    search_parts = [
        name,
        description,
        brand,
        (
            f"{current} ампер"
            if current is not None
            else None
        ),
        (
            f"{int(poles)} полюса"
            if poles is not None
            else None
        ),
        (
            f"{voltage} вольт"
            if voltage is not None
            else None
        ),
        (
            f"{breaking_capacity} кА"
            if breaking_capacity is not None
            else None
        ),
        trip_curve,
        series
    ]

    search_text = " ".join(
        str(value)
        for value in search_parts
        if value not in [None, ""]
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
        "stores": product.get(
            "stores",
            []
        ),
        "image": product.get("image"),
        "url": product.get("url"),
        "search_text": search_text,
        "conflicts": conflicts
    }


def normalize_products(products):
    return [
        normalize_product(product)
        for product in products
    ]


if __name__ == "__main__":
    with open(
        "data/product_details.json",
        "r",
        encoding="utf-8"
    ) as f:
        products = json.load(f)

    normalized = normalize_products(
        products
    )

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
        1
        for product in normalized
        if product["conflicts"]
    )

    print(
        f"Normalized: {len(normalized)}"
    )

    print(
        f"Products with conflicts: "
        f"{conflict_count}"
    )