## API контракт (MVP)

>Последнее обновление: Софа (15.03.26)

## Base URL

/api/v1
### Аутентификация

#### POST /auth/register

Регистрация нового пользователя.

**Request:**
```json
{
  "email": "writer@example.com",
  "password": "securePassword123",
  "name": "Лев Толстой"
}
```
**Response 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "writer@example.com",
  "displayName": "Лев Толстой",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Ошибки:**
- `400` — невалидный email или слабый пароль
- `409` — email уже занят

---

#### POST /auth/login

**Request:**
```json
{
  "email": "writer@example.com",
  "password": "securePassword123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "writer@example.com",
    "name": "Лев Толстой"
  }
}
```

**Ошибки:**
- `401` — неверный email или пароль

---

## Проекты

### POST /projects

Создать новый проект.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "title": "Война и мир"
}
```

**Response 201:**
```json
{
  "id": "project-uuid",
  "title": "Война и мир",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

---

#### GET /projects

Список проектов пользователя.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
[
  {
    "id": "project-uuid",
    "title": "Война и мир",
    "createdAt": "2025-01-15T10:30:00Z",
    "eventCount": 25
  }
]
```

---

#### GET /projects/{projectId}

Получить проект с графом.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
{
  "id": "project-uuid",
  "title": "Война и мир",
  "events": [
    {
      "id": "event-uuid-1",
      "title": "Вечер у Анны Шерер",
      "description": "Светский вечер, где знакомятся главные герои",
      "impactLevel": 7,
      "level": 0,
      "orderInLevel": 0
    },
    {
      "id": "event-uuid-2",
      "title": "Пьер получает наследство",
      "description": "Пьер становится богатейшим человеком России",
      "impactLevel": 9,
      "level": 1,
      "orderInLevel": 0
    }
  ],
  "connections": [
    {
      "id": "conn-uuid-1",
      "sourceEventId": "event-uuid-1",
      "targetEventId": "event-uuid-2",
      "type": "CAUSAL"
    }
  ]
}
```

**Ошибки:**
- `404` — проект не найден
- `403` — нет доступа (чужой проект)

---

## Генерация графа

#### POST /projects/{projectId}/generate

Отправить текст на парсинг, получить граф.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "text": "Глава 1. Вечер у Анны Павловны Шерер был в самом разгаре..."
}
```

**Response 200:**
```json
{
  "events": [
    {
      "id": "event-uuid-1",
      "title": "Вечер у Анны Шерер",
      "description": "Светский приём, завязка романа",
      "impactLevel": 7,
      "level": 0,
      "orderInLevel": 0
    },
    {
      "id": "event-uuid-2",
      "title": "Приезд князя Василия с дочерью",
      "description": "Появление Элен, будущей жены Пьера",
      "impactLevel": 6,
      "level": 0,
      "orderInLevel": 1
    },
    {
      "id": "event-uuid-3",
      "title": "Разговор о Наполеоне",
      "description": "Политическая дискуссия, обозначение конфликта эпохи",
      "impactLevel": 5,
      "level": 1,
      "orderInLevel": 0
    }
  ],
  "connections": [
    {
      "sourceEventId": "event-uuid-1",
      "targetEventId": "event-uuid-3",
      "type": "CAUSAL"
    },
    {
      "sourceEventId": "event-uuid-2",
      "targetEventId": "event-uuid-3",
      "type": "PARALLEL"
    }
  ]
}
```

**Ошибки:**
- `400` — текст пустой или слишком короткий
- `404` — проект не найден
- `503` — AI-сервис недоступен

---
