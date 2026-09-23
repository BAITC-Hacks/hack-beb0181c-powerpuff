"""Обучение модели. Запуск из папки ml/:  python -m src.train

Сейчас обучает пример на датасете Iris. Замените загрузку данных и модель на свои:
сырые данные кладите в data/raw/, обработанные — в data/processed/.
"""

from pathlib import Path

import joblib
from sklearn.datasets import load_iris
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split

MODELS_DIR = Path("models")


def main() -> None:
    iris = load_iris()
    X, y = iris.data, iris.target_names[iris.target]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)

    acc = accuracy_score(y_test, model.predict(X_test))
    print(f"Test accuracy: {acc:.3f}")

    MODELS_DIR.mkdir(exist_ok=True)
    joblib.dump(model, MODELS_DIR / "model.joblib")
    print(f"Saved to {MODELS_DIR / 'model.joblib'}")


if __name__ == "__main__":
    main()
