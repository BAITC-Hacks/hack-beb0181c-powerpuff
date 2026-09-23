# hack-beb0181c-powerpuff

Hackathon team repository for Powerpuff.

> **Идея в одном предложении:** _TODO — какую проблему решаем и для кого._

## Демо

- 🔗 Live: _TODO — ссылка после деплоя_
- 🎥 Видео / скриншоты: _TODO_

## Стек

| Часть     | Технологии                                   |
|-----------|----------------------------------------------|
| Backend   | Java 21, Spring Boot 3.5, Spring Data JPA    |
| Frontend  | React 19, TypeScript, Vite                   |
| AI / ML   | Python 3.11+, FastAPI, scikit-learn          |
| База      | H2 (локально) / PostgreSQL (docker compose)  |

## Структура

```
.
├── backend/                  # Spring Boot API (порт 8080)
│   └── src/main/java/com/powerpuff/backend/
│       ├── config/           # CORS и прочая конфигурация
│       ├── controller/       # REST-эндпоинты (/api/...)
│       ├── ml/               # Клиент к ML-сервису + /api/predict
│       ├── dto/              # Запросы и ответы API (records)
│       ├── exception/        # Глобальная обработка ошибок
│       ├── model/            # JPA-сущности
│       ├── repository/       # Spring Data репозитории
│       └── service/          # Бизнес-логика
├── frontend/                 # React + TS (порт 5173)
│   └── src/
│       ├── api/              # Клиент для запросов к бэкенду
│       ├── components/       # Переиспользуемые компоненты
│       ├── pages/            # Страницы
│       └── types/            # TS-типы (совпадают с DTO)
├── ml/                       # AI/ML: FastAPI + обучение (порт 8000), см. ml/README.md
│   ├── app/                  # API инференса
│   ├── src/                  # Обучение, обработка данных
│   ├── notebooks/            # Эксперименты
│   ├── data/                 # Данные (не в git)
│   └── models/               # Веса моделей (не в git)
└── docker-compose.yml        # PostgreSQL
```

## Запуск

**Нужно:** Java 21, Maven, Node.js 20+, Python 3.11+.

Схема: `frontend :5173 → backend :8080 → ml :8000`

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

Проверка: http://localhost:8080/api/health → `{"status":"ok"}`

По умолчанию используется H2 в памяти — ничего устанавливать не надо.
Консоль БД: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:hackdb`, user `sa`).

С PostgreSQL:

```bash
docker compose up -d
cd backend
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

### 2. ML-сервис

```bash
cd ml
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python -m src.train
uvicorn app.main:app --reload --port 8000
```

Подробнее — в [ml/README.md](ml/README.md).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Открыть http://localhost:5173. Запросы на `/api` проксируются на `:8080`.

## API

| Метод  | Путь              | Описание           |
|--------|-------------------|--------------------|
| GET    | `/api/health`     | Проверка           |
| GET    | `/api/items`      | Список             |
| GET    | `/api/items/{id}` | Один элемент       |
| POST   | `/api/items`      | Создать            |
| DELETE | `/api/items/{id}` | Удалить            |
| POST   | `/api/predict`    | Предсказание (проксирует в ML-сервис) |

`Item` — пример сущности. Замените на свою (модель → DTO → репозиторий → сервис → контроллер → тип и API на фронте).

## Как работаем

- `main` всегда рабочая. Фичи — в отдельных ветках: `git checkout -b feature/название`.
- Маленькие частые коммиты, merge через Pull Request.
- Секреты — только в `.env` (он в `.gitignore`), в репо — `.env.example`.

## Команда

- _TODO — имя, роль_
