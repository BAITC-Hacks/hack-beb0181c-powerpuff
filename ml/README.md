# ML service

Python-сервис для AI/ML: обучение модели и API для инференса (FastAPI, порт **8000**).
Бэкенд на Spring Boot обращается к нему через `POST /predict`.

## Структура

```
ml/
├── app/            # FastAPI: main.py (эндпоинты), model.py (инференс), schemas.py
├── src/            # Обучение и обработка данных (train.py)
├── notebooks/      # Jupyter — эксперименты, EDA
├── data/
│   ├── raw/        # Исходные данные (в git не попадают)
│   └── processed/  # Обработанные данные (в git не попадают)
├── models/         # Сохранённые модели *.joblib, *.pt (в git не попадают)
└── tests/          # pytest
```

## Запуск

```bash
cd ml
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

python -m src.train                 # обучить пример → models/model.joblib
uvicorn app.main:app --reload --port 8000
```

- Swagger: http://localhost:8000/docs
- Проверка: http://localhost:8000/health

Без обученной модели `/predict` возвращает заглушку `"stub"` — так бэкенд может интегрироваться сразу.

## Контракт API

`POST /predict`

```json
{ "features": [5.1, 3.5, 1.4, 0.2] }
```

Ответ:

```json
{ "prediction": "setosa", "confidence": 1.0, "model_loaded": true }
```

Если меняете формат входа/выхода — обновите `app/schemas.py` и предупредите бэкенд
(`backend/.../ml/PredictRequest.java`, `PredictResponse.java`).

## Тесты

```bash
pytest
```
