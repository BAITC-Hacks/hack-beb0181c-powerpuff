# EKT backend — этап 1

Подготовлена основа приложения: Spring Boot 3.5.6, Java 21, Maven Wrapper,
PostgreSQL, Flyway, JPA, Validation и REST-ошибки в формате ProblemDetail.
Существующие `/api/items` и `/api/predict` сохранены. Каталог EKT, импорт,
поиск, LLM и корзина ещё не реализованы; внешние запросы при старте не выполняются.

## Совместимость

Существующая версия Spring Boot 3.5.6 сохранена. Ветка 3.5 поддерживает Java 21
и Maven 3.6.3+ ([официальная документация](https://docs.spring.io/spring-boot/3.5/system-requirements.html)).
Версии Flyway, PostgreSQL JDBC, JUnit, Mockito и Testcontainers управляются Spring Boot BOM.
Wrapper использует Maven 3.9.11; первый запуск требует интернета.

## PostgreSQL в Docker, Spring Boot в IDE

Нужны JDK 21 и запущенный Docker с Compose. Контейнер содержит только PostgreSQL 16.
Команды выполняются из корня репозитория, где находится `docker-compose.yml`:

```bash
# Если .env уже существует, отредактируйте его, не перезаписывайте.
cp .env.example .env
# Задайте свои DB_USERNAME и DB_PASSWORD в .env.
docker compose up -d --wait db
docker compose ps db
```

На Windows файл можно скопировать через `Copy-Item .env.example .env` в PowerShell.
Compose завершит ожидание, когда PostgreSQL пройдёт healthcheck.
База называется `hackdb`, порт доступен только локально: `127.0.0.1:5432`.
Пользователь и пароль берутся из окружения или корневого `.env`.
Значения переменных, уже экспортированных в терминале, имеют приоритет над `.env`.

### Настройка IDE

1. Импортируйте `backend/pom.xml` как Maven-проект и выберите JDK 21.
2. Создайте Java/Spring Boot конфигурацию запуска для
   `com.powerpuff.backend.BackendApplication`, модуль `backend`.
3. В Environment variables задайте значения из вашего `.env`:
   - `DB_URL=jdbc:postgresql://localhost:5432/hackdb`
   - `DB_USERNAME` — тот же пользователь, что у контейнера.
   - `DB_PASSWORD` — тот же пароль, что у контейнера.
4. Запустите основной класс через Run/Debug. Profile `postgres` не требуется:
   PostgreSQL используется по умолчанию.

В IntelliJ IDEA переменные задаются в Run → Edit Configurations → Environment variables.
В VS Code используйте переменные `env` локальной Java launch-конфигурации
или загрузку `.env`, если она настроена вашим расширением. Не коммитьте секреты.

Docker Compose автоматически читает `.env`, Spring Boot — нет. Экспорт переменных
в терминале не меняет окружение IDE, которая была открыта раньше.
Доступы EKT для этого этапа не нужны: оставьте `EKT_ENABLED=false`.

### Управление базой

```bash
docker compose logs --tail=50 db
docker compose stop db
docker compose up -d --wait db
```

Данные сохраняются в named volume `pgdata`, в том числе после `docker compose down`.
Не используйте `down -v`, если данные нужны: этот флаг удаляет volume.
Если порт 5432 занят, освободите его или задайте свободный `DB_PORT` в `.env`
и соответствующий порт в `DB_URL` конфигурации IDE.

| Переменная | Значение |
|---|---|
| DB_URL | По умолчанию jdbc:postgresql://localhost:5432/hackdb |
| DB_USERNAME | Обязательный пользователь БД |
| DB_PASSWORD | Обязательный пароль БД |
| EKT_BASE_URL | https://ekt.kz |
| EKT_ENABLED | false; пока только переключатель проверки конфигурации |
| EKT_USERNAME / EKT_PASSWORD | Не нужны для этапа 1; обязательны при EKT_ENABLED=true |
| CORS_ORIGINS | http://localhost:5173; несколько адресов через запятую |
| PORT | 8080 |

`.env` исключён из Git. Не добавляйте реальные доступы в код или тесты.
Параметр `EKT_ENABLED=true` пока не запускает интеграцию, а проверяет наличие доступов.

## Проверка

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

`/api/health` возвращает `{"status":"ok"}` и проверяет HTTP-приложение.
`/actuator/health` дополнительно проверяет подключение к БД, без раскрытия деталей.

Ошибки используют `application/problem+json`: `type`, `title`, `status`, `detail`.
Для ошибок валидации добавляется объект `errors` с именами полей.
В ответах нет stack trace, исходного невалидного JSON или внутренних сообщений исключений.

## Схема и код

- `controller`, `service`, `repository`, `dto`: существующие слои API.
- `entity/Product`: базовые поля будущего каталога.
- `client`: место будущего клиента EKT.
- `config`: CORS и проверяемая конфигурация EKT.
- `exception`: единая обработка ошибок.
- `db/migration/V1__initial_schema.sql`: таблицы `items` и `products`.

ID товара — Long, назначается EKT. Цена и количество — BigDecimal,
в БД NUMERIC(38,10). Артикул и штрихкод — строки без удаления `_` и ведущих нулей.
Неизвестные цена и количество допускают NULL. Полная структура карточки,
JSONB properties/offers и склады будут добавлены на следующем этапе.

Flyway создаёт схему; Hibernate использует `ddl-auto=validate` и не изменяет её.
Для новой установки нужна пустая БД. Если старый Docker volume уже содержит
таблицы от Hibernate, не удаляйте его: используйте отдельную пустую БД либо
согласуйте миграцию существующей схемы. Автоматический baseline не включён.
Изменение пароля в .env не меняет пароль уже созданного пользователя PostgreSQL.

## Тесты

Из backend:

```bash
./mvnw test
```

Быстрые тесты запускают Flyway и JPA validation на H2 (только test scope).
Проверяют health, формат ошибок и сохранение идентификаторов/дробных значений.
Это не заменяет проверку PostgreSQL.

С работающим Docker:

```bash
./mvnw -DpostgresTests=true test
```

Дополнительно запускается PostgreSQL 16 через Testcontainers для проверки
миграции и Hibernate. Без флага этот тест явно пропускается; с флагом и
недоступным Docker тест завершится ошибкой.
