# API Documentation — TMS Backend

Базовый URL: `http://localhost:8080`
Все запросы и ответы передаются в формате `application/json`.

## Содержание
- [Общие сведения](#общие-сведения)
- [Ролевая модель (RBAC)](#ролевая-модель-rbac)
- [Пользователи (Users)](#пользователи-users)
- [Переводы и Начисления (Economy)](#переводы-и-начисления-economy)
- [События (Events)](#события-events)
- [Участники событий (Participants)](#участники-событий-participants)
- [Системные процессы (Фоновые задачи)](#системные-процессы-фоновые-задачи)

---

## Общие сведения

### Формат даты
Для дат используется стандарт ISO-8601 без таймзоны:
`2026-11-01T10:00:00`

### Перечисления (Enums)
**EventStatus** (Статус события):
- `NEW` — создано, ожидает одобрения.
- `ASSIGNED` — одобрено, активно, можно регистрироваться.
- `COMPLETED` — завершено (автоматически после `toTime`).
- `CANCELLED` — отменено.

**CoinTransactionType** (Тип операции с монетами):
- `PEER_THANKS` — перевод сотрудником коллеге.
- `EVENT_REWARD` — награда за участие в событии.
- `MANAGER_BONUS` — премия от руководителя.
- `PURCHASE` — списание монет (каталог).
- `ADMIN_ADJUSTMENT` — ручная правка от админа.

---

## Ролевая модель (RBAC)

В системе используются роли, каждая из которых имеет набор прав (`Permission`).

| Роль | Описание | Ключевые права |
|---|---|---|
| `EMPLOYEE` | Обычный сотрудник | Переводы монет, создание событий, просмотр |
| `ORGANIZER` | Организатор | Всё как `EMPLOYEE` + редактирование любых событий |
| `MANAGER` | Руководитель | Всё как `EMPLOYEE` + начисление бонусов (`MANAGER_BONUS`) |
| `HR` | HR / Модератор | Одобрение событий (`NEW → ASSIGNED`), просмотр скрытых `NEW` |
| `ADMIN` | Администратор | Полный доступ ко всем функциям и ручная правка баланса |

> **Важно:** В текущей MVP-версии идентификация пользователя происходит через передачу `actorId` в параметрах/теле запроса. Внедрение Spring Security/JWT запланировано на следующем этапе.

---

## Пользователи (Users)

### `POST /api/users` — Создать пользователя
Создает нового сотрудника. Пароль сохраняется в БД, но **никогда** не возвращается в ответе.

**Тело запроса:**
| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `name` | string | Да | Имя пользователя |
| `email` | string | Да | Уникальный email |
| `password` | string | Да | Пароль |
| `role` | string | Да | `EMPLOYEE`, `ORGANIZER`, `MANAGER`, `HR`, `ADMIN` |
| `balance` | number | Нет | Начальный баланс (по умолчанию `0.00`) |

**Пример запроса:**
```http
POST /api/users
Content-Type: application/json

{
  "name": "Иван Иванов",
  "email": "ivan@tms.local",
  "password": "securepassword",
  "role": "EMPLOYEE",
  "balance": 0
}
```

**Ответ `200 OK`:**
```json
{
  "id": 1,
  "name": "Иван Иванов",
  "email": "ivan@tms.local",
  "role": "EMPLOYEE",
  "rating": 0.0,
  "balance": 0.00
}
```

---

### `GET /api/users/{id}` — Получить пользователя
**Ответ `200 OK`:** Возвращает объект пользователя (аналогично созданию).

### `GET /api/users/{id}/transactions` — История операций
Возвращает журнал движения монет пользователя с пагинацией.

**Query parameters:**
- `page` (default `0`) — номер страницы.
- `size` (default `20`) — размер страницы.

**Пример запроса:**
```http
GET /api/users/1/transactions?page=0&size=10
```

**Ответ `200 OK`:**
```json
{
  "content": [
    {
      "id": 42,
      "fromUserId": null,
      "toUserId": 1,
      "amount": 100.00,
      "type": "EVENT_REWARD",
      "comment": "Награда за событие #5: Java Meetup",
      "createdAt": "2026-10-18T15:30:00"
    },
    {
      "id": 41,
      "fromUserId": 1,
      "toUserId": 2,
      "amount": 50.00,
      "type": "PEER_THANKS",
      "comment": "Спасибо за помощь с релизом!",
      "createdAt": "2026-10-18T14:00:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1
}
```

---

## Переводы и Начисления (Economy)

### `POST /api/transfers` — Перевод между сотрудниками
Перевод монет от одного пользователя другому. Работают дневные лимиты.

**Тело запроса:**
| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `fromUserId` | long | Да | Кто переводит |
| `toUserId` | long | Да | Кому переводят |
| `amount` | number | Да | Сумма перевода (> 0) |
| `comment` | string | Да | Причина перевода («Спасибо!») |

**Пример запроса:**
```http
POST /api/transfers
Content-Type: application/json

{
  "fromUserId": 1,
  "toUserId": 2,
  "amount": 30.00,
  "comment": "Спасибо за помощь!"
}
```

**Ответ `200 OK`:**
```json
{
  "id": 43,
  "fromUserId": 1,
  "toUserId": 2,
  "amount": 30.00,
  "type": "PEER_THANKS",
  "comment": "Спасибо за помощь!",
  "createdAt": "2026-10-18T15:35:00"
}
```

**Ошибки:**
- `400` — недостаточно средств.
- `400` — превышен дневной лимит (50 монет одному коллеге, 200 в сумме).
- `400` — перевод самому себе.

---

### `POST /api/transfers/grant` — Начисление бонусов (MANAGER/ADMIN)
Прямое начисление монет (эмиссия). Только для ролей `MANAGER` и `ADMIN`.

**Тело запроса:**
| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `actorId` | long | Да | Кто начисляет (MANAGER или ADMIN) |
| `toUserId` | long | Да | Кому начисляют |
| `amount` | number | Да | Сумма |
| `comment` | string | Нет | Причина (например, «Квартальная премия») |

**Пример запроса:**
```http
POST /api/transfers/grant
Content-Type: application/json

{
  "actorId": 5, 
  "toUserId": 1,
  "amount": 500.00,
  "comment": "Бонус за успешный релиз"
}
```

**Ответ `200 OK`:**
```json
{
  "id": 44,
  "fromUserId": null, 
  "toUserId": 1,
  "amount": 500.00,
  "type": "MANAGER_BONUS",
  "comment": "Бонус за успешный релиз",
  "createdAt": "2026-10-18T16:00:00"
}
```
> *Примечание: Если начисляет ADMIN, тип операции будет `ADMIN_ADJUSTMENT`.*

**Ошибки:**
- `403` — у пользователя нет прав на начисление.
- `400` — превышен лимит начислений менеджера.

---

## События (Events)

### `POST /api/events` — Создать событие
Создает мероприятие в статусе `NEW`.

**Тело запроса:**
| Поле | Тип | Обязательно | Описание |
|---|---|---|---|
| `organizerId` | long | Да | ID создателя |
| `fromTime` | datetime | Да | Начало |
| `toTime` | datetime | Да | Конец (> `fromTime`) |
| `category` | string | Да | Категория |
| `nameEvent` | string | Да | Название |
| `paidInCoins` | boolean | Нет | Начислять ли награду |
| `rewardCoins` | number | Если `paidInCoins` | Монет каждому участнику |
| `rewardRating` | number | Нет | Рейтинга каждому участнику |

**Пример запроса:**
```http
POST /api/events
Content-Type: application/json

{
  "organizerId": 3,
  "fromTime": "2026-12-01T10:00:00",
  "toTime": "2026-12-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0
}
```

**Ответ `201 Created`:**
```json
{
  "id": 1,
  "organizerId": 3,
  "organizerName": "Иван Организатор",
  "fromTime": "2026-12-01T10:00:00",
  "toTime": "2026-12-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "eventStatus": "NEW",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0,
  "rewardsGranted": false
}
```

---

### `PUT /api/events/{id}/assign` — Одобрить событие
Переводит событие `NEW → ASSIGNED`. **Требует права `EVENT_APPROVE` (доступно HR и ADMIN).**

**Тело запроса:**
```json
{ "actorId": 2 }
```
**Ответ `200 OK`:** Возвращает обновленный объект события со статусом `"eventStatus": "ASSIGNED"`.
**Ошибки:**
- `403` — если `actorId` не имеет прав (например, EMPLOYEE или ORGANIZER).

---

### `PUT /api/events/{id}/cancel` — Отменить событие
**Ответ `200 OK`:** Возвращает объект события со статусом `"eventStatus": "CANCELLED"`.

---

### `GET /api/events` — Получить список событий
Возвращает список событий. Видимость зависит от роли (`actorId`):
- **Без `actorId` или обычный сотрудник:** не видит события в статусе `NEW`.
- **HR / ADMIN (с `EVENT_VIEW_ANY`):** видят все события.

**Query parameters:**
- `actorId` (optional) — ID того, кто запрашивает.

**Пример ответа:**
```json
[
  {
    "id": 1,
    "nameEvent": "Spring Boot Workshop",
    "eventStatus": "ASSIGNED",
    "organizerId": 3,
    ...
  }
]
```

---

## Участники событий (Participants)

### `POST /api/events/{eventId}/participants` — Записаться на событие
Записаться может любой пользователь, кроме организатора этого события. Событие должно быть в статусе `ASSIGNED`.

**Тело запроса:**
```json
{ "userId": 4 }
```

**Ответ `201 Created`:**
```json
{
  "userId": 4,
  "name": "Мария Сотрудник",
  "email": "maria@tms.local"
}
```

**Ошибки:**
- `400` — событие не в статусе `ASSIGNED`.
- `400` — организатор не может быть участником.
- `400` — пользователь уже записан.

---

### `DELETE /api/events/{eventId}/participants/{userId}` — Отписаться
**Ответ `204 No Content`.** Удаляет запись об участии.

---

### `GET /api/events/{eventId}/participants` — Список участников
**Ответ `200 OK`:** Массив объектов `{ userId, name, email }`.

---

## Системные процессы (Фоновые задачи)

### Автоматическое завершение и начисление наград
1. **Планировщик (`EventSchedulerService`)** просыпается каждые 30 секунд.
2. Находит все события, где `status = ASSIGNED` и `toTime < текущего времени`.
3. Вызывает логику завершения (`EventRewardService`):
    - Если `paidInCoins = true`, всем участникам начисляются `rewardCoins` (в баланс) и `rewardRating` (в рейтинг).
    - Создается запись в журнале транзакций с типом `EVENT_REWARD`.
    - Событие переводится в статус `COMPLETED`, а флаг `rewardsGranted` становится `true` (защита от повторного начисления).