# PlotMap Backend

Backend-часть проекта **PlotMap** — приложения для построения сюжетной карты произведения.

Идея проекта: пользователь может либо собрать граф сюжета вручную, либо загрузить текст, после чего система с помощью AI выделит события, связи, персонажей и сюжетные арки.

## Что умеет backend

Сейчас backend поддерживает:

- регистрацию и логин пользователя
- создание manual-проекта
- создание AI-проекта по тексту
- асинхронную генерацию графа через job
- получение проекта целиком вместе с графом
- добавление глав
- CRUD для:
  - событий
  - связей
  - персонажей
  - сюжетных арок
  - тегов

## Стек

- Kotlin
- Spring Boot
- Spring Security + JWT
- PostgreSQL
- Neo4j
- Flyway
- Jackson
- Yandex GPT API

## Основные сущности

- **Project** — проект
- **Chapter** — глава / кусок текста
- **Event** — событие сюжета
- **Edge** — связь между событиями
- **Character** — персонаж
- **StoryArc** — сюжетная арка
- **Tag** — пользовательский тег
- **GenerationJob** — асинхронная задача генерации

## Как запустить

### 1. Клонировать проект

```bash
git clone <repo-url>
cd plotmap-backend/server
```

### 2. Настроить `application.properties`

Нужно указать рабочие настройки для:

- PostgreSQL
- Neo4j
- JWT
- Yandex GPT API

Файл лежит здесь:

```bash
src/main/resources/application.properties
```

### 3. Запустить приложение

```bash
./gradlew bootRun
```

По умолчанию backend будет доступен на:

```bash
http://localhost:8080
```

Base URL API:

```bash
http://localhost:8080/api/v1
```

## Основные endpoints

### Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/login/email`
- `POST /api/v1/auth/login/name`

### Projects
- `GET /api/v1/projects`
- `POST /api/v1/projects`
- `POST /api/v1/projects/generate`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}`

### Jobs
- `GET /api/v1/jobs/{jobId}`

### Chapters
- `GET /api/v1/projects/{projectId}/chapters`
- `POST /api/v1/projects/{projectId}/chapters`

### Events
- `POST /api/v1/projects/{projectId}/events`
- `PATCH /api/v1/projects/{projectId}/events/{eventId}`
- `DELETE /api/v1/projects/{projectId}/events/{eventId}`

### Edges
- `POST /api/v1/projects/{projectId}/edges`
- `PATCH /api/v1/projects/{projectId}/edges/{edgeId}`
- `DELETE /api/v1/projects/{projectId}/edges/{edgeId}`

### Characters
- `POST /api/v1/projects/{projectId}/characters`
- `PATCH /api/v1/projects/{projectId}/characters/{characterId}`
- `DELETE /api/v1/projects/{projectId}/characters/{characterId}`

### Story Arcs
- `POST /api/v1/projects/{projectId}/story-arcs`
- `PATCH /api/v1/projects/{projectId}/story-arcs/{arcId}`
- `DELETE /api/v1/projects/{projectId}/story-arcs/{arcId}`

### Tags
- `POST /api/v1/projects/{projectId}/tags`
- `DELETE /api/v1/projects/{projectId}/tags/{tagId}`
- `POST /api/v1/projects/{projectId}/events/{eventId}/tags/{tagId}`
- `DELETE /api/v1/projects/{projectId}/events/{eventId}/tags/{tagId}`

## Как работает AI-генерация

1. Пользователь отправляет текст через `POST /projects/generate`
2. Backend создает проект и job
3. Frontend / клиент опрашивает `GET /jobs/{jobId}`
4. Когда job получает статус `COMPLETED`, можно запрашивать:
   - `GET /projects/{projectId}`

Важно: генерация больших текстов работает **асинхронно** и может занимать какое-то время.

## Тесты

Запуск тестов:

```bash
./gradlew test
```

## Автор
- София Гареева
