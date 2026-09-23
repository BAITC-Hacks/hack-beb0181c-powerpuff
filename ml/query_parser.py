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

    if any(x in text for x in [
        "автоматический выключатель",
        "автомат",
        "авт.выкл",
        "авт выкл"
    ]):
        result["product_type"] = "circuit_breaker"

    brands = [
        "legrand",
        "schneider",
        "iek",
        "abb",
        "dekraft"
    ]

    for brand in brands:
        if brand in text:
            result["brand"] = brand.capitalize()
            break

    current_match = re.search(
        r"(\d+(?:[.,]\d+)?)\s*(?:а|a|ампер)",
        text
    )

    if current_match:
        result["current"] = float(
            current_match.group(1).replace(",", ".")
        )

    pole_patterns = [
        (r"\b([1-4])\s*(?:p|п)\b", 1),
        (r"\b([1-4])\s*(?:ф|фазы|фазный|фазной)\b", 1)
    ]

    for pattern, _ in pole_patterns:
        match = re.search(pattern, text)

        if match:
            result["poles"] = int(match.group(1))
            break

    words = {
        "однофазный": 1,
        "двухфазный": 2,
        "трехфазный": 3,
        "трёхфазный": 3,
        "четырехполюсный": 4,
        "четырёхполюсный": 4
    }

    for word, value in words.items():
        if word in text:
            result["poles"] = value
            break

    voltage_match = re.search(
        r"(\d+(?:[.,]\d+)?)\s*(?:в|v|вольт)",
        text
    )

    if voltage_match:
        result["voltage"] = float(
            voltage_match.group(1).replace(",", ".")
        )

    capacity_match = re.search(
        r"(\d+(?:[.,]\d+)?)\s*(?:ka|ка|кa)",
        text
    )

    if capacity_match:
        result["breaking_capacity"] = float(
            capacity_match.group(1).replace(",", ".")
        )

    return result


while True:
    query = input("Query: ")

    if query.lower() == "exit":
        break

    print(parse_query(query))
