from stage1_search import stage1_search
from detail_client import get_product_details
from normalizer import normalize_product


def same_number(a, b, tolerance=0.01):
    if a is None or b is None:
        return False

    return abs(
        float(a) - float(b)
    ) <= tolerance


def stage2_score(parsed, product):
    score = 0.0
    reasons = []

    if parsed["brand"]:
        product_brand = product.get("brand")

        if (
            product_brand
            and parsed["brand"].lower()
            == product_brand.lower()
        ):
            score += 2.0
            reasons.append("brand exact")
        elif product_brand:
            score -= 1.0

    if parsed["current"] is not None:
        product_current = product.get(
            "current"
        )

        if same_number(
            parsed["current"],
            product_current
        ):
            score += 3.0
            reasons.append("current exact")
        elif product_current is not None:
            score -= 2.0

    if parsed["poles"] is not None:
        product_poles = product.get(
            "poles"
        )

        if same_number(
            parsed["poles"],
            product_poles
        ):
            score += 2.0
            reasons.append("poles exact")
        elif product_poles is not None:
            score -= 1.5

    if parsed["voltage"] is not None:
        product_voltage = product.get(
            "voltage"
        )

        if same_number(
            parsed["voltage"],
            product_voltage
        ):
            score += 1.5
            reasons.append("voltage exact")
        elif product_voltage is not None:
            score -= 1.0

    if parsed["breaking_capacity"] is not None:
        capacity = product.get(
            "breaking_capacity"
        )

        if same_number(
            parsed["breaking_capacity"],
            capacity
        ):
            score += 1.5
            reasons.append(
                "breaking capacity exact"
            )
        elif capacity is not None:
            score -= 1.0

    return score, reasons


def search_products(query, top_k=5):
    parsed, stage1_results = stage1_search(
        query,
        top_k=30
    )

    ids = [
        result["id"]
        for result in stage1_results
    ]

    details = get_product_details(ids)

    normalized = [
        normalize_product(product)
        for product in details
    ]

    stage1_scores = {
        result["id"]: result["final_score"]
        for result in stage1_results
    }

    results = []

    for product in normalized:
        attribute_score, reasons = (
            stage2_score(
                parsed,
                product
            )
        )

        stage1_score = stage1_scores.get(
            product["id"],
            0.0
        )

        final_score = (
            stage1_score * 0.25
            + attribute_score
        )

        results.append({
            "product": product,
            "stage1_score": stage1_score,
            "attribute_score": attribute_score,
            "final_score": final_score,
            "reasons": reasons
        })

    results.sort(
        key=lambda x: x["final_score"],
        reverse=True
    )

    return parsed, results[:top_k]


if __name__ == "__main__":
    query = input("Search: ")

    parsed, results = search_products(
        query
    )

    print()
    print("Parsed:")
    print(parsed)

    print()
    print("Final results:")

    for i, result in enumerate(
        results,
        start=1
    ):
        product = result["product"]

        print()
        print(
            f"{i}. {product['name']}"
        )
        print(
            f"ID: {product['id']}"
        )
        print(
            f"Score: "
            f"{result['final_score']:.4f}"
        )
        print(
            f"Stage1: "
            f"{result['stage1_score']:.4f}"
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
            f"Brand: {product['brand']}"
        )
        print(
            f"Current: {product['current']}A"
        )
        print(
            f"Poles: {product['poles']}"
        )
        print(
            f"Voltage: {product['voltage']}V"
        )
        print(
            f"Capacity: "
            f"{product['breaking_capacity']}kA"
        )
        print(
            f"Price: {product['price']}"
        )
        print(
            f"Quantity: {product['quantity']}"
        )
