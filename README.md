# hack-beb0181c-powerpuff

Hackathon team repository for Powerpuff.

> Backend консультанта EKT: каталог электротехники, поиск и корзина с явным подтверждением.

## Демо

- 🔗 Live: _TODO — ссылка после деплоя_
- 🎥 Видео / скриншоты: _TODO_

## Стек

| Часть     | Технологии                                   |
|-----------|----------------------------------------------|
| Backend   | Java 21, Spring Boot 3.5, Spring Data JPA    |
| Frontend  | React 19, TypeScript, Vite                   |
| AI / ML   | Python 3.11+, FastAPI, scikit-learn          |
| База      | PostgreSQL (docker compose), H2 только в тестах  |

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

Инструкция по основе EKT, настройкам и тестам: [backend/README.md](backend/README.md).

Запустите Docker. Из корня репозитория (если `.env` уже есть, сохраните его):

```bash
cp .env.example .env
# Отредактируйте DB_USERNAME и DB_PASSWORD в .env.
docker compose up -d --wait db
docker compose ps db
```

Spring Boot запускайте из IDE: импортируйте `backend/pom.xml` как Maven-проект,
выберите JDK 21 и класс `com.powerpuff.backend.BackendApplication`.
В **Environment variables** конфигурации запуска укажите `DB_URL`, `DB_USERNAME`
и `DB_PASSWORD` из `.env`. JDBC URL: `jdbc:postgresql://localhost:5432/hackdb`.
Дополнительный Spring profile не нужен.

Docker Compose читает `.env` автоматически; Spring Boot и отдельно запущенная IDE
не получают эти значения автоматически. Пароль задавайте в локальной конфигурации IDE,
а не в отслеживаемых файлах проекта.

Проверка: http://localhost:8080/api/health → `{"status":"ok"}`.
Проверка подключения к БД: http://localhost:8080/actuator/health.

Остановка БД с сохранением данных: `docker compose stop db`.
Подробности и решение проблем: [backend/README.md](backend/README.md).

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

Контракт чата, сессий и корзины: [backend/docs/frontend-api.md](backend/docs/frontend-api.md).


| Метод  | Путь              | Описание           |
|--------|-------------------|--------------------|
| GET    | `/api/products/search?q=Legrand&page=0&size=20` | Поиск в PostgreSQL |
| GET    | `/api/catalog/status` | Статус загрузки каталога в PostgreSQL |
| GET    | `/api/products?page=1` | Страница каталога напрямую из EKT |
| GET    | `/api/products/{id}` | Карточка товара напрямую из EKT |
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
