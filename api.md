# API Documentation — TMS Backend

Базовый URL: `http://localhost:8080`

Все запросы и ответы — в формате `application/json`.

## Содержание

- [Общие сведения](#общие-сведения)
- [Пользователи](#пользователи)
- [Переводы монет](#переводы-монет)
- [События](#события)
- [Коды ответов](#коды-ответов)

---

## Общие сведения

### Enums

**UserRole**

| Значение | Описание |
|---|---|
| `Admin` | Администратор — может назначать события, видеть все `NEW` |
| `User` | Обычный пользователь — организатор мероприятий |

**EventStatus**

| Значение | Описание |
|---|---|
| `NEW` | Создано, ожидает назначения администратором |
| `ASSIGNED` | Принято администратором, активно |
| `COMPLETED` | Завершено (автоматически после `toTime` или вручную) |
| `CANCELLED` | Отменено |

### Формат даты

`LocalDateTime` — ISO-8601 без таймзоны:

```
2026-11-01T10:00:00
```

---

## Пользователи

### `POST /api/users` — создать пользователя

Создаёт нового пользователя. Баланс и рейтинг инициализируются нулями, если не переданы.

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `name` | string | да | Имя пользователя |
| `email` | string | да | Уникальный email |
| `password` | string | да | Пароль (не возвращается в ответе) |
| `role` | `UserRole` | да | `Admin` или `User` |
| `balance` | number | нет | По умолчанию `0` |
| `rating` | number | нет | По умолчанию `0.0` |

**Пример запроса**

```http
POST /api/users
Content-Type: application/json

{
  "name": "Иван Иванов",
  "email": "ivan@example.com",
  "password": "secret123",
  "role": "User",
  "balance": 1000
}
```

**Пример ответа `200 OK`**

```json
{
  "id": 1,
  "name": "Иван Иванов",
  "email": "ivan@example.com",
  "role": "User",
  "rating": 0.0,
  "balance": 1000.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Пользователь с таким email уже существует |

---

### `GET /api/users` — получить всех пользователей

**Пример запроса**

```http
GET /api/users
```

**Пример ответа `200 OK`**

```json
[
  {
    "id": 1,
    "name": "Иван Иванов",
    "email": "ivan@example.com",
    "role": "User",
    "rating": 0.0,
    "balance": 1000.00
  },
  {
    "id": 2,
    "name": "Админ",
    "email": "admin@example.com",
    "role": "Admin",
    "rating": 0.0,
    "balance": 5000.00
  }
]
```

---

### `GET /api/users/{id}` — получить пользователя по ID

**Path params**

| Параметр | Тип | Описание |
|---|---|---|
| `id` | long | ID пользователя |

**Пример запроса**

```http
GET /api/users/1
```

**Пример ответа `200 OK`**

```json
{
  "id": 1,
  "name": "Иван Иванов",
  "email": "ivan@example.com",
  "role": "User",
  "rating": 0.0,
  "balance": 1000.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Пользователь не найден |

---

## Переводы монет

### `POST /api/transfers` — перевести монеты

Атомарный перевод монет между пользователями. Использует пессимистичную блокировку — безопасно при параллельных запросах.

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `fromUserId` | long | да | ID отправителя |
| `toUserId` | long | да | ID получателя |
| `amount` | number | да | Сумма > 0 |

**Пример запроса**

```http
POST /api/transfers
Content-Type: application/json

{
  "fromUserId": 1,
  "toUserId": 2,
  "amount": 100.00
}
```

**Пример ответа `200 OK`**

```json
{
  "status": "OK",
  "transactionId": 42,
  "amount": 100.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Сумма <= 0 |
| `400` | ID пользователей не переданы |
| `400` | Попытка перевода самому себе |
| `400` | Отправитель или получатель не найден |
| `400` | Недостаточно средств |

---

## События

### `POST /api/events` — создать событие

Создаёт событие в статусе `NEW`. Награда (`rewardCoins`, `rewardRating`) применяется только если `paidInCoins = true`.

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `organizerId` | long | да | ID организатора |
| `fromTime` | datetime | да | Начало события |
| `toTime` | datetime | да | Конец события (должен быть > `fromTime`) |
| `category` | string | да | Категория |
| `nameEvent` | string | да | Название |
| `description` | string | нет | Описание |
| `eventPlace` | string | нет | Место проведения |
| `commentToAdmin` | string | нет | Комментарий для админа |
| `paidInCoins` | boolean | нет | Начислять ли награду (по умолчанию `false`) |
| `rewardCoins` | number | если `paidInCoins=true` | Монет каждому участнику (> 0) |
| `rewardRating` | number | нет | Рейтинга каждому участнику (>= 0) |

**Пример запроса — с наградой**

```http
POST /api/events
Content-Type: application/json

{
  "organizerId": 1,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "description": "Воркшоп по Spring Boot",
  "eventPlace": "Москва, офис 5",
  "commentToAdmin": "Прошу подтвердить",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0
}
```

**Пример запроса — без награды**

```http
POST /api/events
Content-Type: application/json

{
  "organizerId": 1,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "paidInCoins": false
}
```

**Пример ответа `201 Created`**

```json
{
  "id": 1,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "organizer": {
    "id": 1,
    "name": "Иван Иванов",
    "email": "ivan@example.com",
    "role": "User",
    "rating": 0.0,
    "balance": 1000.00
  },
  "description": "Воркшоп по Spring Boot",
  "eventPlace": "Москва, офис 5",
  "commentToAdmin": "Прошу подтвердить",
  "eventStatus": "NEW",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0,
  "rewardsGranted": false
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | `fromTime` не раньше `toTime` |
| `400` | Организатор не найден |
| `400` | `paidInCoins=true` и `rewardCoins <= 0` / не передан |
| `400` | `rewardRating < 0` |

---

### `PUT /api/events/{id}/assign` — назначить событие (только админ)

Переводит событие из `NEW` в `ASSIGNED`. Только пользователь с ролью `Admin`.

**Path params**

| Параметр | Тип | Описание |
|---|---|---|
| `id` | long | ID события |

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | да | ID администратора, выполняющего действие |

**Пример запроса**

```http
PUT /api/events/1/assign
Content-Type: application/json

{
  "actorId": 2
}
```

**Пример ответа `200 OK`**

```json
{
  "id": 1,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "organizer": { "id": 1, "name": "Иван Иванов" },
  "eventStatus": "ASSIGNED",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardsGranted": false
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `403` | `actorId` не администратор |
| `400` | Событие не найдено |
| `400` | Текущий статус не `NEW` |

---

### `PUT /api/events/{id}/cancel` — отменить событие

Переводит событие в `CANCELLED`. Нельзя отменить `COMPLETED` или уже `CANCELLED`.

**Path params**

| Параметр | Тип | Описание |
|---|---|---|
| `id` | long | ID события |

**Пример запроса**

```http
PUT /api/events/1/cancel
```

**Пример ответа `200 OK`**

```json
{
  "id": 1,
  "eventStatus": "CANCELLED",
  ...
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Событие не найдено |
| `400` | Уже `COMPLETED` |
| `400` | Уже `CANCELLED` |

---

### `GET /api/events` — получить все события

Возвращает список событий. Видимость зависит от роли актора:

- **без `actorId`** или обычный `User` — всё, **кроме** `NEW`;
- **`Admin`** — **все**, включая `NEW`.

**Query params**

| Параметр | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | нет | ID пользователя, от чьего имени запрос |

**Пример запроса — гость**

```http
GET /api/events
```

**Пример запроса — админ (id=2)**

```http
GET /api/events?actorId=2
```

**Пример ответа `200 OK`**

```json
[
  {
    "id": 1,
    "nameEvent": "Spring Boot Workshop",
    "fromTime": "2026-11-01T10:00:00",
    "toTime": "2026-11-01T12:00:00",
    "eventStatus": "ASSIGNED",
    "organizer": { "id": 1, "name": "Иван Иванов" },
    "paidInCoins": true,
    "rewardCoins": 100.00
  },
  {
    "id": 2,
    "nameEvent": "Java Meetup",
    "eventStatus": "COMPLETED",
    ...
  }
]
```

---

### `GET /api/events/organizer/{organizerId}` — события организатора

Список событий конкретного организатора. Видимость:

- **`Admin`** и **сам организатор** — все, включая `NEW`;
- **остальные** — только не-`NEW`.

**Path params**

| Параметр | Тип | Описание |
|---|---|---|
| `organizerId` | long | ID организатора |

**Query params**

| Параметр | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | нет | ID пользователя, от чьего имени запрос |

**Пример запроса — организатор смотрит свои**

```http
GET /api/events/organizer/1?actorId=1
```

**Пример запроса — чужой смотрит**

```http
GET /api/events/organizer/1?actorId=3
```

**Пример ответа `200 OK`**

```json
[
  {
    "id": 1,
    "nameEvent": "Spring Boot Workshop",
    "eventStatus": "NEW",
    ...
  }
]
```

---

## Коды ответов

| Код | Значение |
|---|---|
| `200 OK` | Успешный запрос |
| `201 Created` | Ресурс создан (POST /api/events) |
| `400 Bad Request` | Ошибка валидации / бизнес-логики |
| `403 Forbidden` | Недостаточно прав (не админ для assign) |
| `500 Internal Server Error` | Непредвиденная ошибка на сервере |

Обработка ошибок — глобальная через `GlobalExceptionHandler`. Тело ответа при ошибке — строка с сообщением:

```json
"Только администратор может присвоить статус ASSIGNED"
```

---

## Жизненный цикл события

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  POST /api/events                                               │
│         │                                                       │
│         ▼                                                       │
│      ┌──────┐   PUT /{id}/assign (admin)   ┌──────────┐         │
│      │ NEW  │ ───────────────────────────► │ ASSIGNED │         │
│      └──────┘                              └──────────┘         │
│         │                                       │               │
│         │ PUT /{id}/cancel                      │ toTime < now  │
│         │                                       │ (scheduler)   │
│         ▼                                       ▼               │
│    ┌───────────┐                          ┌───────────┐         │
│    │ CANCELLED │                          │ COMPLETED │         │
│    └───────────┘                          └───────────┘         │
│                                                                 │
│  При переходе в COMPLETED:                                      │
│    • Начисляются rewardCoins и rewardRating всем участникам     │
│    • rewardsGranted = true                                      │
│    • Идемпотентно: повторный запуск не задвоит награду          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Планировщик

Каждые 30 секунд сервис `EventSchedulerService`:

1. Находит события со статусом `ASSIGNED` и `toTime < now`.
2. Для каждого вызывает `completeAndAward`:
    - начисляет награду всем участникам (если `paidInCoins = true`);
    - переводит в `COMPLETED`;
    - ставит `rewardsGranted = true`.

Ручного эндпоинта для завершения нет — оно происходит автоматически. Для тестирования можно временно создать событие с `toTime` в прошлом и `status = ASSIGNED`.