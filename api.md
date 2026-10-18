# API Documentation — TMS Backend

Базовый URL: `http://localhost:8080`

Все запросы и ответы — в формате `application/json`.

## Содержание

- [Общие сведения](#общие-сведения)
- [Роли и разрешения](#роли-и-разрешения)
- [Пользователи](#пользователи)
- [Переводы монет](#переводы-монет)
- [События](#события)
- [Участники событий](#участники-событий)
- [Коды ответов](#коды-ответов)
- [Жизненный цикл события](#жизненный-цикл-события)

---

## Общие сведения

### Формат даты

`LocalDateTime` — ISO-8601 без таймзоны:

```
2026-11-01T10:00:00
```

### Enums

**UserRole** — см. раздел [Роли и разрешения](#роли-и-разрешения).

**EventStatus**

| Значение | Описание |
|---|---|
| `NEW` | Создано, ожидает назначения администратором / HR |
| `ASSIGNED` | Принято, активно, можно регистрироваться |
| `COMPLETED` | Завершено (автоматически после `toTime`) |
| `CANCELLED` | Отменено |

---

## Роли и разрешения

В системе **5 ролей**. Каждая роль — это набор разрешений (`Permission`). Проверки в коде идут по разрешениям, а не по роли, поэтому расширение — добавление `Permission` в enum, без правки сервисов.

### Роли

| Роль | Кто это | Основные возможности |
|---|---|---|
| `EMPLOYEE` | Обычный сотрудник | Создавать события, переводить монеты, покупать в каталоге |
| `ORGANIZER` | Ответственный за мероприятия | Всё как `EMPLOYEE` + редактировать любые события |
| `MANAGER` | Руководитель | Всё как `EMPLOYEE` + начислять бонусы подчинённым |
| `HR` | HR / модератор | Approve событий (`NEW → ASSIGNED`), видеть все `NEW`, управлять пользователями |
| `ADMIN` | Технический админ | Полный доступ (все `Permission`) |

### Разрешения

| Permission | EMPLOYEE | ORGANIZER | MANAGER | HR | ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|
| `EVENT_CREATE` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `EVENT_EDIT_OWN` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `EVENT_EDIT_ANY` | ❌ | ✅ | ❌ | ✅ | ✅ |
| `EVENT_APPROVE` (NEW → ASSIGNED) | ❌ | ❌ | ❌ | ✅ | ✅ |
| `EVENT_VIEW_ANY` (видеть `NEW`) | ❌ | ❌ | ❌ | ✅ | ✅ |
| `COIN_TRANSFER` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `COIN_GRANT_SUBORDINATE` | ❌ | ❌ | ✅ | ❌ | ✅ |
| `COIN_GRANT_ANY` | ❌ | ❌ | ❌ | ❌ | ✅ |
| `CATALOG_VIEW` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `CATALOG_MANAGE` | ❌ | ❌ | ❌ | ❌ | ✅ |
| `USER_MANAGE` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `ROLE_ASSIGN` | ❌ | ❌ | ❌ | ❌ | ✅ |

### Как проверяется доступ

- `assignEvent` → `Permission.EVENT_APPROVE`;
- `getAllEvents` / `getEventsByUser` → `Permission.EVENT_VIEW_ANY`;
- остальные эндпоинты пока без проверок, добавятся по мере появления.

---

## Пользователи

### `POST /api/users` — создать пользователя

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `name` | string | да | Имя |
| `email` | string | да | Уникальный email |
| `password` | string | да | Пароль (не возвращается) |
| `role` | `UserRole` | да | `EMPLOYEE` / `ORGANIZER` / `MANAGER` / `HR` / `ADMIN` |
| `balance` | number | нет | По умолчанию `0` |
| `rating` | number | нет | По умолчанию `0.0` |

**Пример запроса**

```http
POST /api/users
Content-Type: application/json

{
  "name": "Мария Сотрудник",
  "email": "maria@tms.local",
  "password": "maria123",
  "role": "EMPLOYEE",
  "balance": 0
}
```

**Пример ответа `200 OK`**

```json
{
  "id": 3,
  "name": "Мария Сотрудник",
  "email": "maria@tms.local",
  "role": "EMPLOYEE",
  "rating": 0.0,
  "balance": 0.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Email уже существует |
| `400` | Недопустимое значение `role` (не из enum) |

---

### `GET /api/users` — все пользователи

```http
GET /api/users
```

**Ответ `200 OK`** — массив объектов пользователей (поля те же).

---

### `GET /api/users/{id}` — пользователь по ID

```http
GET /api/users/3
```

**Ответ `200 OK`**

```json
{
  "id": 3,
  "name": "Мария Сотрудник",
  "email": "maria@tms.local",
  "role": "EMPLOYEE",
  "rating": 5.0,
  "balance": 100.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Пользователь не найден |

---

## Переводы монет

### `POST /api/transfers` — перевести монеты

Атомарный перевод. Использует пессимистичную блокировку на обоих пользователях (в порядке возрастания ID — защита от deadlock).

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
  "toUserId": 3,
  "amount": 50.00
}
```

**Ответ `200 OK`**

```json
{
  "status": "OK",
  "transactionId": 42,
  "amount": 50.00
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Сумма <= 0 |
| `400` | ID не переданы |
| `400` | Перевод самому себе |
| `400` | Отправитель/получатель не найден |
| `400` | Недостаточно средств |

---

## События

### `POST /api/events` — создать событие

Создаёт событие в статусе `NEW`.

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `organizerId` | long | да | ID организатора |
| `fromTime` | datetime | да | Начало |
| `toTime` | datetime | да | Конец, должен быть > `fromTime` |
| `category` | string | да | Категория |
| `nameEvent` | string | да | Название |
| `description` | string | нет | Описание |
| `eventPlace` | string | нет | Место |
| `commentToAdmin` | string | нет | Комментарий для админа |
| `paidInCoins` | boolean | нет | Начислять ли награду |
| `rewardCoins` | number | если `paidInCoins=true` | Монет каждому участнику (> 0) |
| `rewardRating` | number | нет | Рейтинга каждому участнику (>= 0) |

**Пример запроса — с наградой**

```http
POST /api/events
Content-Type: application/json

{
  "organizerId": 2,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "description": "Воркшоп",
  "eventPlace": "Москва, офис 5",
  "commentToAdmin": "Прошу подтвердить",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0
}
```

**Ответ `201 Created`**

```json
{
  "id": 1,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "organizer": {
    "id": 2,
    "name": "Иван Организатор",
    "email": "ivan@tms.local",
    "role": "ORGANIZER",
    "rating": 0.0,
    "balance": 500.00
  },
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
| `400` | `paidInCoins=true` и `rewardCoins` отсутствует / <= 0 |
| `400` | `rewardRating < 0` |

---

### `PUT /api/events/{id}/assign` — назначить событие (`NEW → ASSIGNED`)

**Требуется `Permission.EVENT_APPROVE`** (HR или ADMIN).

**Path params**

| Параметр | Тип | Описание |
|---|---|---|
| `id` | long | ID события |

**Request body**

| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | да | ID того, кто выполняет действие |

**Пример запроса**

```http
PUT /api/events/1/assign
Content-Type: application/json

{ "actorId": 1 }
```

**Ответ `200 OK`**

```json
{
  "id": 1,
  "eventStatus": "ASSIGNED",
  "rewardsGranted": false,
  ...
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `403` | У актора нет `EVENT_APPROVE` |
| `400` | Событие не найдено |
| `400` | Текущий статус не `NEW` |
| `400` | Актор не найден |

---

### `PUT /api/events/{id}/cancel` — отменить событие

**Path params**

| Параметр | Тип |
|---|---|
| `id` | long |

**Пример запроса**

```http
PUT /api/events/1/cancel
```

**Ответ `200 OK`**

```json
{ "id": 1, "eventStatus": "CANCELLED", ... }
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Событие не найдено |
| `400` | Уже `COMPLETED` |
| `400` | Уже `CANCELLED` |

---

### `GET /api/events` — все события

Возвращает события с учётом прав актора:

- **без `actorId`** или без `EVENT_VIEW_ANY` → всё, **кроме `NEW`**;
- **с `EVENT_VIEW_ANY`** (HR, ADMIN) → **все**, включая `NEW`.

**Query params**

| Параметр | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | нет | Кто запрашивает |

**Пример запроса — HR**

```http
GET /api/events?actorId=4
```

**Пример запроса — сотрудник**

```http
GET /api/events?actorId=3
```

**Ответ `200 OK`** — массив `Event`. Поле `NEW` присутствует только в ответе HR/ADMIN.

---

### `GET /api/events/organizer/{organizerId}` — события организатора

- **HR / ADMIN / сам организатор** → все, включая `NEW`;
- **остальные** → только не-`NEW`.

**Path params**

| Параметр | Тип |
|---|---|
| `organizerId` | long |

**Query params**

| Параметр | Тип | Обязательно |
|---|---|---|
| `actorId` | long | нет |

**Пример запроса**

```http
GET /api/events/organizer/2?actorId=3
```

**Ответ `200 OK`** — массив `Event` этого организатора.

---

## Участники событий

Участвовать может любой пользователь, **кроме организатора** этого события. Записаться можно только на событие в статусе `ASSIGNED`.

### `POST /api/events/{eventId}/participants` — записаться на событие

**Path params**

| Параметр | Тип |
|---|---|
| `eventId` | long |

**Request body**

| Поле | Тип | Обязательно |
|---|---|---|
| `userId` | long | да |

**Пример запроса**

```http
POST /api/events/1/participants
Content-Type: application/json

{ "userId": 3 }
```

**Ответ `201 Created`**

```json
{
  "userId": 3,
  "name": "Мария Сотрудник",
  "email": "maria@tms.local"
}
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Событие не найдено |
| `400` | Статус события не `ASSIGNED` |
| `400` | Пользователь не найден |
| `400` | Пользователь — организатор этого события |
| `400` | Пользователь уже зарегистрирован |

---

### `DELETE /api/events/{eventId}/participants/{userId}` — отписаться

**Path params**

| Параметр | Тип |
|---|---|
| `eventId` | long |
| `userId` | long |

**Пример запроса**

```http
DELETE /api/events/1/participants/3
```

**Ответ `204 No Content`** — пустое тело.

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Событие не найдено |
| `400` | Статус `COMPLETED` или `CANCELLED` |
| `400` | Пользователь не зарегистрирован |

---

### `GET /api/events/{eventId}/participants` — список участников

**Path params**

| Параметр | Тип |
|---|---|
| `eventId` | long |

**Пример запроса**

```http
GET /api/events/1/participants
```

**Ответ `200 OK`**

```json
[
  { "userId": 3, "name": "Мария Сотрудник", "email": "maria@tms.local" },
  { "userId": 5, "name": "Пётр Разработчик", "email": "petr@tms.local" }
]
```

**Ошибки**

| Код | Причина |
|---|---|
| `400` | Событие не найдено |

---

## Коды ответов

| Код | Значение |
|---|---|
| `200 OK` | Успешный запрос |
| `201 Created` | Создан ресурс (`POST /events`, `POST /participants`) |
| `204 No Content` | Удалён ресурс (`DELETE /participants/{userId}`) |
| `400 Bad Request` | Ошибка валидации / бизнес-логики |
| `403 Forbidden` | Недостаточно прав (`Permission` не хватает) |
| `500 Internal Server Error` | Непредвиденная ошибка |

Тело ответа при ошибке — строка:

```json
"Только пользователь с правом EVENT_APPROVE может присвоить ASSIGNED"
```

---

## Жизненный цикл события

```
POST /api/events
       │
       ▼
   ┌──────┐   PUT /{id}/assign (HR/ADMIN)   ┌──────────┐
   │ NEW  │ ──────────────────────────────► │ ASSIGNED │
   └──────┘                                 └──────────┘
       │                                          │
       │ PUT /{id}/cancel                         │ POST /participants
       ▼                                          │ toTime < now (планировщик)
  ┌───────────┐                                   ▼
  │ CANCELLED │                              ┌───────────┐
  └───────────┘                              │ COMPLETED │
                                             └───────────┘

При переходе в COMPLETED:
  • rewardCoins и rewardRating начисляются всем участникам
  • rewardsGranted = true
  • идемпотентно: повторный запуск не задвоит
```