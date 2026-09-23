import re


def parse_query(query):
    text = query.lower()

    result = {
        "original_query": query,
        "product_type": None,
        "brand": None,
        "current": None,
        "poles": None,
        "voltage": None,
        "breaking_capacity": None
    }

    product_type_patterns = [
        r"автоматическ\w*\s+выключател\w*",
        r"\bавтомат\w*\b",
        r"\bавт\.?\s*выкл\.?\b",
        r"\bав\b"
    ]

    for pattern in product_type_patterns:
        if re.search(pattern, text):
            result["product_type"] = "circuit_breaker"
            break

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

    for keyword, normalized_brand in brands.items():
        if keyword in text:
            result["brand"] = normalized_brand
            break

    current_match = re.search(
        r"(?<![\w])(\d+(?:[.,]\d+)?)\s*(?:а|a|ампер(?:а|ов)?)(?![\w])",
        text
    )

    if current_match:
        result["current"] = float(
            current_match.group(1).replace(",", ".")
        )

    pole_match = re.search(
        r"\b([1-4])\s*(?:p|п)\b",
        text
    )

    if pole_match:
        result["poles"] = int(pole_match.group(1))

    if result["poles"] is None:
        phase_match = re.search(
            r"\b([1-4])\s*ф\b",
            text
        )

        if phase_match:
            result["poles"] = int(phase_match.group(1))

    if result["poles"] is None:
        if re.search(r"тр[её]хфаз", text):
            result["poles"] = 3
        elif re.search(r"двухфаз", text):
            result["poles"] = 2
        elif re.search(r"однофаз", text):
            result["poles"] = 1

    if result["poles"] is None:
        if re.search(r"четыр[её]хполюс", text):
            result["poles"] = 4
        elif re.search(r"тр[её]хполюс", text):
            result["poles"] = 3
        elif re.search(r"двухполюс", text):
            result["poles"] = 2
        elif re.search(r"однополюс", text):
            result["poles"] = 1

    voltage_match = re.search(
        r"(?<![\w])(\d+(?:[.,]\d+)?)\s*(?:в|v|вольт(?:а|ов)?)(?![\w])",
        text
    )

    if voltage_match:
        result["voltage"] = float(
            voltage_match.group(1).replace(",", ".")
        )

    capacity_match = re.search(
        r"(?<![\w])(\d+(?:[.,]\d+)?)\s*(?:ka|ка|кa|кa)(?![\w])",
        text
    )

    if capacity_match:
        result["breaking_capacity"] = float(
            capacity_match.group(1).replace(",", ".")
        )

    return result


if __name__ == "__main__":
    while True:
        query = input("Query: ")

        if query.lower().strip() == "exit":
            break

        print(parse_query(query))