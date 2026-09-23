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
| База      | H2 (локально) / PostgreSQL (docker compose)  |

## Структура

```
.
├── backend/                  # Spring Boot API (порт 8080)
│   └── src/main/java/com/powerpuff/backend/
│       ├── config/           # CORS и прочая конфигурация
│       ├── controller/       # REST-эндпоинты (/api/...)
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
└── docker-compose.yml        # PostgreSQL
```

## Запуск

**Нужно:** Java 21, Maven, Node.js 20+.

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

### 2. Frontend

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

`Item` — пример сущности. Замените на свою (модель → DTO → репозиторий → сервис → контроллер → тип и API на фронте).

## Как работаем

- `main` всегда рабочая. Фичи — в отдельных ветках: `git checkout -b feature/название`.
- Маленькие частые коммиты, merge через Pull Request.
- Секреты — только в `.env` (он в `.gitignore`), в репо — `.env.example`.

## Команда

- _TODO — имя, роль_
