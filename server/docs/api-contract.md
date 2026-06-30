# API контракт PlotMap (актуальный MVP)

> Последнее обновление: Софа (26.06.2026)

## Base URL

```text
/api/v1
```

## Формат

- Все запросы и ответы — `application/json`
- Для защищённых ручек нужен заголовок:

```text
Authorization: Bearer <token>
```

---

# 1. Health

## GET /health

Проверка, что backend запущен.

### Response 200
```json
{
  "status": "OK"
}
```

---

# 2. Аутентификация

## POST /auth/register

Регистрация нового пользователя.

### Request
```json
{
  "email": "writer@example.com",
  "password": "securePassword123",
  "name": "Лев Толстой"
}
```

### Response 201
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "writer@example.com",
  "name": "Лев Толстой",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### Возможные ошибки
- `400` — невалидные данные
- `409` — email уже занят

---

## POST /auth/login/email

Альтернативная ручка логина по email.

### Request
```json
{
  "email": "writer@example.com",
  "password": "securePassword123"
}
```

### Response 200
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "writer@example.com",
  "name": "Лев Толстой",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

---

## POST /auth/login/name

Альтернативная ручка логина по имени.

### Request
```json
{
  "name": "Лев Толстой",
  "password": "securePassword123"
}
```

### Response 200
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "writer@example.com",
  "name": "Лев Толстой",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

---

# 3. Проекты

## GET /projects

Получить список проектов текущего пользователя.

### Headers
```text
Authorization: Bearer <token>
```

### Response 200
```json
[
  {
    "id": "project-uuid",
    "title": "Война и мир",
    "type": "MANUAL",
    "description": "Черновик романа",
    "createdAt": "2026-06-25T07:02:43.665513Z"
  }
]
```

---

## POST /projects

Создать новый manual-проект.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Война и мир",
  "description": "Черновик романа"
}
```

### Response 201
```json
{
  "id": "project-uuid",
  "title": "Война и мир",
  "type": "MANUAL",
  "description": "Черновик романа",
  "createdAt": "2026-06-25T07:02:43.665513Z"
}
```

### Возможные ошибки
- `400` — пустой `title`
- `401` — нет токена / невалидный токен

---

## POST /projects/generate

Создать AI-проект по тексту и запустить асинхронную генерацию.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "name": "Преступление и наказание",
  "description": "Граф романа",
  "text": "Родион Раскольников, бедный студент..."
}
```

### Response 202
```json
{
  "jobId": "job-uuid",
  "projectId": "project-uuid",
  "status": "PENDING",
  "errorMessage": null,
  "result": null,
  "createdAt": "2026-06-25T07:02:43.672844Z",
  "updatedAt": "2026-06-25T07:02:43.672844Z"
}
```

### Важно
После этого клиент должен:
1. сохранить `projectId`
2. поллить `GET /jobs/{jobId}`
3. после `COMPLETED` запросить `GET /projects/{projectId}`

### Возможные ошибки
- `400` — пустой текст / пустое имя проекта
- `401` — нет токена / невалидный токен

---

## GET /projects/{projectId}

Получить проект целиком вместе с графом.

### Headers
```text
Authorization: Bearer <token>
```

### Response 200
```json
{
  "id": "project-uuid",
  "title": "Преступление и наказание",
  "type": "AI_GENERATED",
  "description": "Граф романа",
  "events": [
    {
      "id": "event-uuid-1",
      "title": "Родион вынашивает теорию",
      "description": "Родион размышляет о своей теории",
      "suggestedSystemRole": "INCITING_INCIDENT",
      "impactLevel": 8,
      "status": "IMPLEMENTED",
      "userNotes": "",
      "level": 0,
      "orderInLevel": 0,
      "customPositionX": null,
      "customPositionY": null,
      "color": "#FFEFD5",
      "source": "AI_GENERATED",
      "sourceContext": "Родион вынашивает свою теорию.",
      "characterIds": [
        "char-uuid-1"
      ],
      "tagIds": [],
      "storyArcIds": [
        "arc-uuid-1"
      ],
      "createdAt": "2026-06-25T07:03:20.769739Z"
    }
  ],
  "connections": [
    {
      "id": "edge-uuid-1",
      "sourceEventId": "event-uuid-1",
      "targetEventId": "event-uuid-2",
      "type": "CAUSAL",
      "description": "Теория приводит к действию"
    }
  ],
  "characters": [
    {
      "id": "char-uuid-1",
      "name": "Родион Раскольников",
      "description": "Главный герой",
      "role": "PROTAGONIST",
      "color": null
    }
  ],
  "storyArcs": [
    {
      "id": "arc-uuid-1",
      "title": "Теория и преступление",
      "description": "Основная сюжетная арка",
      "color": null
    }
  ],
  "tags": [],
  "createdAt": "2026-06-25T07:02:43.665513Z"
}
```

### Возможные ошибки
- `404` — проект не найден
- `403` — нет доступа к проекту
- `401` — нет токена / невалидный токен

---

## PATCH /projects/{projectId}

Обновить название и/или описание проекта.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Новое название",
  "description": "Новое описание"
}
```

### Response 200
```json
{
  "id": "project-uuid",
  "title": "Новое название",
  "type": "MANUAL",
  "description": "Новое описание",
  "createdAt": "2026-06-25T07:02:43.665513Z"
}
```

### Примечание
- поля опциональны
- если `title` передан, он не должен быть пустым

### Возможные ошибки
- `400` — невалидные данные
- `404` — проект не найден
- `403` — нет доступа

---

## DELETE /projects/{projectId}

Удалить проект.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

### Возможные ошибки
- `404` — проект не найден
- `403` — нет доступа

---

# 4. Jobs

## GET /jobs/{jobId}

Получить статус асинхронной генерации.

### Headers
```text
Authorization: Bearer <token>
```

### Response 200
```json
{
  "jobId": "job-uuid",
  "projectId": "project-uuid",
  "status": "PENDING",
  "errorMessage": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Возможные значения `status`
- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

### Возможные ошибки
- `404` — job не найдена
- `403` — нет доступа
- `401` — нет токена / невалидный токен

---

# 5. Главы

## GET /projects/{projectId}/chapters

Получить список глав проекта.

### Headers
```text
Authorization: Bearer <token>
```

### Response 200
```json
[
  {
    "id": "chapter-uuid-1",
    "chapterOrder": 1,
    "title": "Chapter 1",
    "createdAt": "2026-06-25T07:02:43.665513Z"
  }
]
```

### Примечание
Ручка в первую очередь нужна для AI-проектов.  
У manual-проектов список глав обычно пустой.

---

## GET /projects/{projectId}/chapters/{chapterId}

Получить содержимое конкретной главы (включая текст).

### Headers
```text
Authorization: Bearer <token>
```
### Response 200
```json
{
  "id": "chapter-uuid",
  "chapterOrder": 1,
  "title": "Глава 1",
  "text": "Полный текст главы...",
  "createdAt": "2026-06-25T07:02:43.665513Z"
}
```

---

## POST /projects/{projectId}/chapters

Добавить новую главу в AI-проект и запустить её асинхронную обработку.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Chapter 2",
  "text": "Свидригайлов приезжает в Петербург..."
}
```

### Response 201
```json
{
  "chapter": {
    "id": "chapter-uuid-2",
    "chapterOrder": 2,
    "title": "Chapter 2",
    "createdAt": "2026-06-25T07:27:48.101000Z"
  },
  "job": {
    "jobId": "job-uuid",
    "projectId": "project-uuid",
    "status": "PENDING",
    "errorMessage": null,
    "result": null,
    "createdAt": "2026-06-25T07:27:48.110000Z",
    "updatedAt": "2026-06-25T07:27:48.110000Z"
  }
}
```

### Важно
- `title` может быть `null`
- ручка доступна только для `AI_GENERATED` проектов
- после этого клиент должен поллить `GET /jobs/{jobId}`
- после `COMPLETED` нужно заново вызвать `GET /projects/{projectId}`

### Возможные ошибки
- `400` — пустой текст
- `400` — попытка добавить главу в `MANUAL` проект
- `404` — проект не найден
- `403` — нет доступа
---

# 6. События

## POST /projects/{projectId}/events

Создать событие.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Вечер у Анны Шерер",
  "description": "Светский вечер",
  "suggestedSystemRole": "REGULAR",
  "impactLevel": 5,
  "status": "PLANNED",
  "userNotes": "",
  "level": 0,
  "orderInLevel": 0,
  "customPositionX": 100.0,
  "customPositionY": 200.0,
  "color": "#FAFAD2",
  "characterIds": [],
  "storyArcIds": [],
  "tagIds": []
}
```

### Response 201
```json
{
  "id": "event-uuid",
  "title": "Вечер у Анны Шерер",
  "description": "Светский вечер",
  "suggestedSystemRole": "REGULAR",
  "impactLevel": 5,
  "status": "PLANNED",
  "userNotes": "",
  "level": 0,
  "orderInLevel": 0,
  "customPositionX": 100.0,
  "customPositionY": 200.0,
  "color": "#FAFAD2",
  "source": "USER_CREATED",
  "sourceContext": "",
  "characterIds": [],
  "tagIds": [],
  "storyArcIds": [],
  "createdAt": "2026-06-25T07:35:00.000000Z"
}
```

---

## PATCH /projects/{projectId}/events/{eventId}

Частично обновить событие.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Новое название события",
  "userNotes": "важная сцена",
  "color": "#FFDAB9",
  "impactLevel": 8,
  "status": "IMPLEMENTED",
  "characterIds": [],
  "storyArcIds": [],
  "tagIds": []
}
```

### Response 200
```json
{
  "id": "event-uuid",
  "title": "Новое название события",
  "description": "Светский вечер",
  "suggestedSystemRole": "REGULAR",
  "impactLevel": 8,
  "status": "IMPLEMENTED",
  "userNotes": "важная сцена",
  "level": 0,
  "orderInLevel": 0,
  "customPositionX": 100.0,
  "customPositionY": 200.0,
  "color": "#FFDAB9",
  "source": "USER_CREATED",
  "sourceContext": "",
  "characterIds": [],
  "tagIds": [],
  "storyArcIds": [],
  "createdAt": "2026-06-25T07:35:00.000000Z"
}
```

---

## DELETE /projects/{projectId}/events/{eventId}

Удалить событие.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

# 7. Связи

## POST /projects/{projectId}/edges

Создать связь между событиями.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "sourceEventId": "event-uuid-1",
  "targetEventId": "event-uuid-2",
  "type": "CAUSAL",
  "description": "Событие A приводит к событию B"
}
```

### Response 201
```json
{
  "id": "edge-uuid",
  "sourceEventId": "event-uuid-1",
  "targetEventId": "event-uuid-2",
  "type": "CAUSAL",
  "description": "Событие A приводит к событию B"
}
```

---

## PATCH /projects/{projectId}/edges/{edgeId}

Обновить связь.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "type": "PARALLEL",
  "description": "Эти события происходят параллельно"
}
```

### Response 200
```json
{
  "id": "edge-uuid",
  "sourceEventId": "event-uuid-1",
  "targetEventId": "event-uuid-2",
  "type": "PARALLEL",
  "description": "Эти события происходят параллельно"
}
```

---

## DELETE /projects/{projectId}/edges/{edgeId}

Удалить связь.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

# 8. Персонажи

## POST /projects/{projectId}/characters

Создать персонажа.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "name": "Пьер Безухов",
  "description": "Один из главных героев",
  "role": "PROTAGONIST",
  "color": "#FAFAD2"
}
```

### Response 201
```json
{
  "id": "character-uuid",
  "name": "Пьер Безухов",
  "description": "Один из главных героев",
  "role": "PROTAGONIST",
  "color": "#FAFAD2"
}
```

---

## PATCH /projects/{projectId}/characters/{characterId}

Обновить персонажа.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "name": "Пьер",
  "description": "Обновлённое описание",
  "role": "MAJOR",
  "color": "#FFEFD5"
}
```

### Response 200
```json
{
  "id": "character-uuid",
  "name": "Пьер",
  "description": "Обновлённое описание",
  "role": "MAJOR",
  "color": "#FFEFD5"
}
```

---

## DELETE /projects/{projectId}/characters/{characterId}

Удалить персонажа.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

# 9. Сюжетные арки

## POST /projects/{projectId}/story-arcs

Создать сюжетную арку.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Арка Пьера",
  "description": "Линия взросления Пьера",
  "color": "#FAFAD2"
}
```

### Response 201
```json
{
  "id": "arc-uuid",
  "title": "Арка Пьера",
  "description": "Линия взросления Пьера",
  "color": "#FAFAD2"
}
```

---

## PATCH /projects/{projectId}/story-arcs/{arcId}

Обновить сюжетную арку.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "title": "Основная арка Пьера",
  "description": "Новое описание",
  "color": "#FFEFD5"
}
```

### Response 200
```json
{
  "id": "arc-uuid",
  "title": "Основная арка Пьера",
  "description": "Новое описание",
  "color": "#FFEFD5"
}
```

---

## DELETE /projects/{projectId}/story-arcs/{arcId}

Удалить сюжетную арку.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

# 10. Теги

## POST /projects/{projectId}/tags

Создать тег.

### Headers
```text
Authorization: Bearer <token>
```

### Request
```json
{
  "name": "любимая сцена",
  "color": "#FFDAB9"
}
```

### Response 201
```json
{
  "id": "tag-uuid",
  "name": "любимая сцена",
  "color": "#FFDAB9"
}
```

---

## DELETE /projects/{projectId}/tags/{tagId}

Удалить тег.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

## POST /projects/{projectId}/events/{eventId}/tags/{tagId}

Привязать тег к событию.

### Headers
```text
Authorization: Bearer <token>
```

### Response 201
Пустое тело.

---

## DELETE /projects/{projectId}/events/{eventId}/tags/{tagId}

Отвязать тег от события.

### Headers
```text
Authorization: Bearer <token>
```

### Response 204
Пустое тело.

---

# 11. Основные enum-значения

## ProjectType
- `MANUAL`
- `AI_GENERATED`

## EventStatus
- `PLANNED`
- `IMPLEMENTED`
- `DISCARDED`

## EventSource
- `USER_CREATED`
- `AI_GENERATED`

## SystemEventRole
- `INCITING_INCIDENT`
- `RISING_ACTION`
- `CLIMAX`
- `FALLING_ACTION`
- `RESOLUTION`
- `PLOT_TWIST`
- `REGULAR`

## ConnectionType
- `CAUSAL`
- `TEMPORAL`
- `PARALLEL`

## JobStatus
- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

## CharacterRole
- `PROTAGONIST`
- `ANTAGONIST`
- `MAJOR`
- `SUPPORTING`
- `EPISODIC`

---

# 12. Важные замечания для фронта

## 1. Главная ручка проекта
Основная ручка для загрузки содержимого проекта:

```text
GET /projects/{projectId}
```

Она уже возвращает всё основное:
- events
- connections
- characters
- storyArcs
- tags

Отдельных `GET`-ручек для событий, персонажей или арок сейчас нет.

## 2. AI-генерация асинхронная
Для AI-проекта нужно:
1. вызвать `POST /projects/generate`
2. сохранить `jobId` и `projectId`
3. поллить `GET /jobs/{jobId}`
4. после `COMPLETED` вызвать `GET /projects/{projectId}`

## 3. Добавление главы в AI-проект
Добавление главы доступно только для `AI_GENERATED` проекта.

После вызова:

```text
POST /projects/{projectId}/chapters
```

backend возвращает:
- `chapter`
- `job`

Поэтому клиент может сразу взять `job.jobId` и начать polling через:

```text
GET /jobs/{jobId}
```

После `COMPLETED` нужно заново вызвать:

```text
GET /projects/{projectId}
```

чтобы получить обновлённый граф.
```
