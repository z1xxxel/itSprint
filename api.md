# TMS Backend API — Полная документация

**Базовый URL:** `http://localhost:8080`  
**Формат:** JSON (`Content-Type: application/json`)  
**Формат дат:** ISO-8601 (`2026-11-01T10:00:00`)

> **Важно:** Сейчас используется упрощённая авторизация — `actorId` передаётся в query/body каждого запроса. Для продакшена нужно внедрить JWT.

---

## Содержание

1. [Роли и права](#1-роли-и-права)
2. [Аутентификация](#2-аутентификация)
3. [Пользователи](#3-пользователи)
4. [Профиль](#4-профиль)
5. [Переводы монет](#5-переводы-монет)
6. [События](#6-события)
7. [Участники событий](#7-участники-событий)
8. [Благодарности](#8-благодарности)
9. [Магазин](#9-магазин)
10. [Заказы](#10-заказы)
11. [Избранное](#11-избранное)
12. [Отзывы](#12-отзывы)
13. [Квесты](#13-квесты)
14. [Опросы](#14-опросы)
15. [Шаблоны опросов](#15-шаблоны-опросов)
16. [Поддержка](#16-поддержка)
17. [Чаты сотрудников](#17-чаты-сотрудников)
18. [Сезоны](#18-сезоны)
19. [Ачивки](#19-ачивки)
20. [Подразделения](#20-подразделения)
21. [Казна](#21-казна)
22. [Админ-панель](#22-админ-панель)
23. [Файлы](#23-файлы)
24. [Системные процессы](#24-системные-процессы)
25. [Коды ошибок](#25-коды-ошибок)
26. [SQL-миграции](#26-sql-миграции)

---

## 1. Роли и права

### Роли

| Роль | Описание |
|---|---|
| `EMPLOYEE` | Сотрудник — базовый доступ |
| `ORGANIZER` | Организатор мероприятий |
| `MANAGER` | Руководитель — премии, модерация заказов |
| `HR` | HR — модерация событий, админ-доступ |
| `SUPPORT` | Техподдержка — управление тикетами |
| `ADMIN` | Полный доступ |

### Права (Permissions)

| Право | EMPLOYEE | ORGANIZER | MANAGER | HR | SUPPORT | ADMIN |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `EVENT_CREATE` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `EVENT_EDIT_ANY` | ❌ | ✅ | ❌ | ✅ | ❌ | ✅ |
| `EVENT_APPROVE` | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| `EVENT_VIEW_ANY` | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| `COIN_TRANSFER` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `COIN_GRANT_SUBORDINATE` | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| `COIN_GRANT_ANY` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `ORDER_MANAGE` | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ |
| `CATALOG_MANAGE` | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| `CATALOG_VIEW` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `QUEST_MANAGE` | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ |
| `QUEST_PARTICIPATE` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `TICKET_MANAGE` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| `TICKET_CREATE` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `USER_MANAGE` | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| `ROLE_ASSIGN` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 2. Аутентификация

### POST `/api/auth/login`

Вход по email и паролю. Публичный эндпоинт.

**Тело запроса:**

| Поле | Тип | Обязательно |
|---|---|---|
| `email` | string | да |
| `password` | string | да |

```json
{
  "email": "admin@tms.local",
  "password": "admin123"
}
```

**Ответ `200 OK`:**

```json
{
  "message": "Успешный вход",
  "userId": 1,
  "name": "Сергей Админов",
  "role": "ADMIN",
  "balance": 10000.00,
  "rating": 0.0
}
```

**Ошибки:**
- `400` — неверный email или пароль
- `400` — аккаунт заблокирован

---

## 3. Пользователи

### Таблица эндпоинтов

| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/users?actorId={id}` | Создать пользователя | ADMIN, HR, MANAGER |
| `GET` | `/api/users` | Список всех пользователей | Все |
| `GET` | `/api/users/{id}` | Пользователь по ID | Все |
| `PUT` | `/api/users/{id}?actorId={id}` | Обновить данные | ADMIN, HR |
| `PUT` | `/api/users/{id}/role?role=X&actorId={id}` | Сменить роль | ADMIN |
| `PUT` | `/api/users/{id}/block?actorId={id}` | Блок/разблок | ADMIN |
| `PUT` | `/api/users/{id}/password?actorId={id}` | Сбросить пароль | ADMIN |

### POST `/api/users` — создать пользователя

**Query:** `actorId` (опционально, если база пуста — можно без него для первого ADMIN)

**Тело запроса:**

| Поле | Тип | Обязательно |
|---|---|---|
| `name` | string | да |
| `email` | string | да (уникальный) |
| `password` | string | да (мин. 4 символа) |
| `role` | enum | да |
| `balance` | number | нет (0 по умолчанию) |

```json
{
  "name": "Новый Сотрудник",
  "email": "employee@tms.local",
  "password": "secret123",
  "role": "EMPLOYEE",
  "balance": 0
}
```

**Ответ `200 OK`:**

```json
{
  "id": 10,
  "name": "Новый Сотрудник",
  "email": "employee@tms.local",
  "role": "EMPLOYEE",
  "rating": 0.0,
  "lifetimeRating": 0.0,
  "balance": 0.00,
  "isBlocked": false
}
```

**Ошибки:**
- `400` — email уже существует
- `403` — у `actorId` нет прав

### GET `/api/users` — список пользователей

**Ответ `200 OK`** — массив объектов:

```json
[
  {
    "id": 1,
    "name": "Сергей Админов",
    "email": "admin@tms.local",
    "role": "ADMIN",
    "rating": 0.0,
    "lifetimeRating": 0.0,
    "balance": 10000.00,
    "isBlocked": false
  }
]
```

### PUT `/api/users/{id}/role` — смена роли

**Query:** `role` (новое значение), `actorId`

```http
PUT /api/users/5/role?role=MANAGER&actorId=1
```

**Ответ `200 OK`** — обновлённый пользователь.

### PUT `/api/users/{id}/block` — блокировка

При блокировке пользователь не может войти (получит `400` при логине).

```http
PUT /api/users/5/block?actorId=1
```

### PUT `/api/users/{id}/password` — сброс пароля

**Query:** `actorId`

**Тело:**
```json
{
  "newPassword": "newStrongPass2026!"
}
```

**Ответ `200 OK`:** `"Пароль успешно изменен"`

---

## 4. Профиль

### GET `/api/users/{id}/profile`

Единый эндпоинт для дашборда профиля.

**Ответ `200 OK`:**

```json
{
  "id": 5,
  "name": "Петр Кодеров",
  "email": "emp_back@tms.local",
  "role": "EMPLOYEE",
  "balance": 500.00,
  "currentRating": 45.5,
  "lifetimeRating": 320.5,
  "level": "Senior",
  "currentPlace": 3,
  "totalUsers": 10,
  "departmentId": 1,
  "departmentName": "Backend Team",
  "departmentPlace": 1,
  "organizedEventsCount": 2,
  "participatedEventsCount": 8,
  "recentRatingTransactions": [
    {
      "id": 42,
      "amount": 5.0,
      "reason": "Награда за событие #1",
      "createdAt": "2026-10-25T14:00:00"
    }
  ],
  "recentBalanceTransactions": [
    {
      "id": 43,
      "fromUserId": null,
      "toUserId": 5,
      "amount": 100.00,
      "type": "EVENT_REWARD",
      "comment": "Награда за событие",
      "tag": null,
      "isCancelled": false,
      "cancellationReason": null,
      "createdAt": "2026-10-25T14:00:00"
    }
  ],
  "recentOrders": [],
  "achievements": [
    {
      "groupCode": "EVENT_JOINER",
      "name": "Участник",
      "description": "Посетил события",
      "iconUrl": "medal1.png",
      "isEarned": true,
      "levels": [
        {
          "level": 1,
          "title": "Участник I",
          "isEarned": true,
          "earnedAt": "2026-10-20T10:00:00"
        },
        {
          "level": 2,
          "title": "Участник II",
          "isEarned": false,
          "earnedAt": null
        }
      ]
    }
  ]
}
```

### GET `/api/users/{id}/transactions` — история баланса

**Query:** `page` (0), `size` (20)

**Ответ `200 OK`** — пагинация:

```json
{
  "content": [
    {
      "id": 43,
      "fromUserId": null,
      "toUserId": 5,
      "amount": 100.00,
      "type": "EVENT_REWARD",
      "comment": "Награда за событие #1",
      "tag": null,
      "isCancelled": false,
      "cancellationReason": null,
      "createdAt": "2026-10-25T14:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### GET `/api/users/{id}/balance-history` — для графиков

Возвращает историю транзакций с большим `size` (по умолчанию 50).

### GET `/api/users/{id}/rating-history` — для графиков рейтинга

```json
{
  "content": [
    {
      "id": 42,
      "amount": 5.0,
      "reason": "Награда за событие #1",
      "createdAt": "2026-10-25T14:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 5. Переводы монет

### POST `/api/transfers` — P2P перевод

Спасибо коллеге.

**Тело:**

| Поле | Тип | Обязательно |
|---|---|---|
| `fromUserId` | long | да |
| `toUserId` | long | да |
| `amount` | number | да (> 0) |
| `comment` | string | да |
| `tag` | enum | нет |

**Enum `ThanksTag`:** `GRATITUDE`, `FOR_HELP`, `INITIATIVE`, `MENTORING`, `TRANSFER`, `OTHER`

```json
{
  "fromUserId": 5,
  "toUserId": 6,
  "amount": 50.00,
  "comment": "Спасибо за помощь с релизом!",
  "tag": "FOR_HELP"
}
```

**Ответ `200 OK`:**

```json
{
  "id": 100,
  "fromUserId": 5,
  "toUserId": 6,
  "amount": 50.00,
  "type": "PEER_THANKS",
  "comment": "Спасибо за помощь с релизом!",
  "tag": "FOR_HELP",
  "isCancelled": false,
  "createdAt": "2026-10-26T10:00:00"
}
```

**Ошибки:**
- `400` — недостаточно средств
- `400` — превышен дневной лимит (1000 монет одному коллеге, 1000 в сумме)
- `400` — перевод самому себе

### POST `/api/transfers/grant` — премия

Начисление бонусов. Только MANAGER (подчинённым, до 2000/5000 в день) и ADMIN (без лимита).

**Тело:**

```json
{
  "actorId": 3,
  "toUserId": 5,
  "amount": 500.00,
  "comment": "Квартальная премия"
}
```

**Ответ `200 OK`:**

```json
{
  "id": 101,
  "fromUserId": null,
  "toUserId": 5,
  "amount": 500.00,
  "type": "MANAGER_BONUS",
  "comment": "Квартальная премия",
  "isCancelled": false,
  "createdAt": "2026-10-26T10:30:00"
}
```

**Ошибки:**
- `403` — нет прав
- `400` — недостаточно средств в Казне
- `400` — превышен дневной лимит
- `400` — превышен месячный лимит заработка (10000 монет)

---

## 6. События

### Таблица эндпоинтов

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/events` | Все |
| `PUT` | `/api/events/{id}/assign` | HR, ADMIN |
| `PUT` | `/api/events/{id}/cancel` | Все |
| `GET` | `/api/events` | Все |
| `GET` | `/api/events/organizer/{id}` | Все |

### POST `/api/events` — создать событие

**Тело:**

| Поле | Тип | Обязательно |
|---|---|---|
| `organizerId` | long | да |
| `fromTime` | datetime | да |
| `toTime` | datetime | да (> fromTime) |
| `category` | string | да |
| `nameEvent` | string | да |
| `description` | string | нет |
| `eventPlace` | string | нет |
| `commentToAdmin` | string | нет |
| `paidInCoins` | bool | нет (false) |
| `rewardCoins` | number | если paidInCoins |
| `rewardRating` | number | нет |

```json
{
  "organizerId": 3,
  "fromTime": "2026-11-01T10:00:00",
  "toTime": "2026-11-01T12:00:00",
  "category": "IT",
  "nameEvent": "Spring Boot Workshop",
  "description": "Воркшоп",
  "eventPlace": "Москва",
  "paidInCoins": true,
  "rewardCoins": 100.00,
  "rewardRating": 5.0
}
```

**Ответ `201 Created`** — объект события со статусом `NEW`.

### PUT `/api/events/{id}/assign` — одобрить

**Тело:** `{ "actorId": 2 }`

**Ответ `200 OK`** — событие со статусом `ASSIGNED`.

**Ошибки:** `403` если у актора нет `EVENT_APPROVE`.

### PUT `/api/events/{id}/cancel`

**Ответ `200 OK`** — статус `CANCELLED`.

### GET `/api/events?actorId={id}`

- Без `actorId` или без `EVENT_VIEW_ANY` — не видит `NEW`.
- HR/ADMIN — видит всё.

---

## 7. Участники событий

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/events/{eventId}/participants` | Записаться |
| `DELETE` | `/api/events/{eventId}/participants/{userId}` | Отписаться |
| `GET` | `/api/events/{eventId}/participants` | Список |

### POST — записаться

**Тело:** `{ "userId": 5 }`

**Ошибки:**
- `400` — событие не в `ASSIGNED`
- `400` — организатор не может быть участником
- `400` — уже записан

---

## 8. Благодарности

### GET `/api/thanks/feed`

Последние 50 благодарностей (публичная лента).

**Ответ `200 OK`:**

```json
[
  {
    "transactionId": 100,
    "fromUserName": "Петр Кодеров",
    "toUserName": "Мария Верстальщикова",
    "amount": 50.00,
    "tag": "FOR_HELP",
    "comment": "Спасибо за помощь!",
    "createdAt": "2026-10-26T10:00:00"
  }
]
```

### GET `/api/thanks/user/{userId}?direction={all|received|sent}`

История благодарностей пользователя.

### GET `/api/thanks/user/{userId}/stats`

Статистика благодарностей:

```json
{
  "totalReceived": 23,
  "totalSent": 15,
  "totalCoinsReceived": 850.00,
  "totalCoinsSent": 500.00,
  "topThankers": [
    { "userName": "Мария Верстальщикова", "count": 8 }
  ]
}
```

---

## 9. Магазин

### Категории

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/shop/categories?actorId={id}` | MANAGER, ADMIN |
| `GET` | `/api/shop/categories` | Все |
| `GET` | `/api/shop/categories/{categoryId}/products` | Все |

**POST `/api/shop/categories`:**

```json
{
  "name": "Мерч",
  "description": "Футболки и кружки",
  "imageUrl": "https://example.com/merch.png",
  "sortOrder": 1
}
```

### Товары

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/shop/products?actorId={id}` | MANAGER, ADMIN |
| `PUT` | `/api/shop/products/{id}?actorId={id}` | MANAGER, ADMIN |
| `DELETE` | `/api/shop/products/{id}?actorId={id}` | MANAGER, ADMIN |
| `GET` | `/api/shop/products` | Все |
| `GET` | `/api/shop/products/search` | Все |
| `POST` | `/api/shop/products/{id}/discount?actorId={id}` | MANAGER, ADMIN |
| `POST` | `/api/shop/products/{id}/write-off?actorId={id}` | MANAGER, ADMIN, HR, ORGANIZER |
| `GET` | `/api/shop/audit-logs?actorId={id}` | MANAGER, ADMIN |

**POST `/api/shop/products`:**

```json
{
  "name": "Футболка",
  "description": "Брендированная футболка",
  "price": 2000.00,
  "priceAdjustmentPercent": 10,
  "stock": 20,
  "imageUrl": "https://example.com/tshirt.png",
  "categoryId": 1
}
```

**GET `/api/shop/products`** — ответ:

```json
[
  {
    "id": 1,
    "name": "Футболка",
    "description": "Брендированная футболка",
    "basePrice": 2000.00,
    "priceAdjustmentPercent": 10,
    "adjustedPrice": 2200.00,
    "discountPercent": 20,
    "finalPrice": 1760.00,
    "isOnDiscount": true,
    "discountValidUntil": "2026-11-01T23:59:59",
    "stock": 20,
    "imageUrl": "https://example.com/tshirt.png",
    "categoryId": 1,
    "categoryName": "Мерч",
    "averageRating": 4.5,
    "reviewCount": 10
  }
]
```

**GET `/api/shop/products/search`** — фильтры:

**Query:** `query`, `categoryId`, `minPrice`, `maxPrice`, `sortBy` (`name`|`price`), `sortDir` (`asc`|`desc`)

**POST `/api/shop/products/{id}/discount`:**

```json
{
  "percent": 20,
  "validFrom": "2026-10-26T00:00:00",
  "validTo": "2026-11-01T23:59:59"
}
```

**POST `/api/shop/products/{id}/write-off`** — списание (подарки, брак):

```json
{
  "quantity": 2,
  "reason": "GIFT",
  "recipientId": 5,
  "comment": "Подарок за активность"
}
```

**Причины списания (`reason`):** `DAMAGED`, `GIFT`, `LOST`, `OTHER`

**GET `/api/shop/audit-logs`** — логи:

```json
{
  "content": [
    {
      "id": 1,
      "action": "STOCK_WRITTEN_OFF",
      "actorId": 1,
      "actorName": "Админ",
      "productId": 1,
      "productName": "Футболка",
      "orderId": null,
      "details": "Списано 2 шт. Причина: GIFT. Получатель: Петр",
      "createdAt": "2026-10-26T11:00:00"
    }
  ]
}
```

---

## 10. Заказы

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/shop/purchase` | Все |
| `GET` | `/api/shop/orders/{userId}` | Все |
| `GET` | `/api/shop/orders/pending?actorId={id}` | MANAGER, HR, ADMIN |
| `PUT` | `/api/shop/orders/{id}/approve?actorId={id}` | MANAGER, HR, ADMIN |
| `PUT` | `/api/shop/orders/{id}/reject?actorId={id}&reason=X` | MANAGER, HR, ADMIN |
| `PUT` | `/api/shop/orders/{id}/complete?actorId={id}` | MANAGER, HR, ADMIN |

**POST `/api/shop/purchase`:**

```json
{
  "userId": 5,
  "productId": 1,
  "quantity": 1
}
```

**Ответ `201 Created`:**

```json
{
  "id": 42,
  "userId": 5,
  "userName": "Петр",
  "productId": 1,
  "productName": "Футболка",
  "quantity": 1,
  "totalPrice": 1760.00,
  "status": "PENDING",
  "adminComment": null,
  "createdAt": "2026-10-26T11:00:00"
}
```

**При отклонении** (`reject`):
- Монеты возвращаются пользователю
- Товар возвращается на склад
- Создаётся транзакция `REFUND`

---

## 11. Избранное

| Метод | Путь |
|---|---|
| `POST` | `/api/shop/favorites/{productId}?userId={id}` |
| `DELETE` | `/api/shop/favorites/{productId}?userId={id}` |
| `GET` | `/api/shop/favorites?userId={id}` |

---

## 12. Отзывы

| Метод | Путь |
|---|---|
| `POST` | `/api/shop/products/{productId}/reviews` |
| `GET` | `/api/shop/products/{productId}/reviews` |

**POST:**

```json
{
  "userId": 5,
  "rating": 5,
  "comment": "Отличный товар!"
}
```

**Ошибки:** `400` если не покупал (нужен заказ со статусом `COMPLETED`), `400` если уже оставлял.

---

## 13. Квесты

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/quests` | MANAGER, HR, ADMIN |
| `DELETE` | `/api/quests/{id}?actorId={id}` | MANAGER, HR, ADMIN |
| `GET` | `/api/quests?userId={id}` | Все |
| `POST` | `/api/quests/{id}/start?userId={id}` | Все |
| `POST` | `/api/quests/{id}/submit?userId={id}` | Все |
| `GET` | `/api/quests/review?actorId={id}` | MANAGER, HR, ADMIN |
| `PUT` | `/api/quests/review/{id}/approve?actorId={id}` | MANAGER, HR, ADMIN |
| `PUT` | `/api/quests/review/{id}/reject?actorId={id}&reason=X` | MANAGER, HR, ADMIN |

**POST `/api/quests`:**

```json
{
  "creatorId": 3,
  "title": "Пробежать 10 км",
  "description": "Спортивный челлендж",
  "rewardCoins": 300.00
}
```

**Статусы квеста:** `IN_PROGRESS`, `PENDING_REVIEW`, `COMPLETED`, `REJECTED`, `CANCELLED`

При `approve` монеты списываются из Казны и начисляются пользователю (с проверкой месячного лимита).

---

## 14. Опросы

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/surveys` | HR, ADMIN |
| `GET` | `/api/surveys` | Все |
| `GET` | `/api/surveys?isActive={true|false}` | Все |
| `POST` | `/api/surveys/{id}/submit` | Все |
| `GET` | `/api/surveys/{id}/results` | HR, ADMIN |
| `GET` | `/api/surveys/{id}/stats?actorId={id}` | HR, ADMIN |
| `GET` | `/api/surveys/{id}/responses?actorId={id}` | HR, ADMIN |
| `DELETE` | `/api/surveys/{id}?actorId={id}` | HR, ADMIN |
| `POST` | `/api/surveys/{id}/save-as-template?actorId={id}&templateName=X` | HR, ADMIN |

**POST `/api/surveys`:**

```json
{
  "creatorId": 2,
  "title": "Ежеквартальный опрос Q4",
  "description": "Помогите нам стать лучше",
  "deadline": "2026-11-01T23:59:59",
  "rewardRating": 2.0,
  "penaltyRating": -1.0,
  "questions": [
    {
      "text": "Оцените удовлетворенность работой",
      "options": [
        { "text": "1 - Очень плохо", "value": 1 },
        { "text": "2 - Плохо", "value": 2 },
        { "text": "3 - Нормально", "value": 3 },
        { "text": "4 - Хорошо", "value": 4 },
        { "text": "5 - Отлично", "value": 5 }
      ]
    }
  ]
}
```

**POST `/api/surveys/{id}/submit`:**

```json
{
  "userId": 5,
  "answers": {
    "1": 5
  }
}
```

**GET `/api/surveys/{id}/stats`** — сводная статистика:

```json
{
  "surveyId": 1,
  "title": "Ежеквартальный опрос Q4",
  "deadline": "2026-11-01T23:59:59",
  "isActive": true,
  "totalUsers": 10,
  "respondedUsers": 7,
  "responseRatePercent": 70.0,
  "questions": [
    {
      "questionId": 1,
      "questionText": "Оцените удовлетворенность",
      "totalAnswers": 7,
      "averageValue": 4.2,
      "options": [
        {
          "optionId": 1,
          "optionText": "1 - Очень плохо",
          "value": 1,
          "count": 0,
          "percentage": 0.0
        }
      ]
    }
  ]
}
```

**GET `/api/surveys/{id}/responses`** — ответы конкретных пользователей:

```json
[
  {
    "userId": 5,
    "userName": "Петр Кодеров",
    "userEmail": "emp_back@tms.local",
    "answeredAt": "2026-10-26T10:00:00",
    "answers": [
      {
        "questionText": "Оцените удовлетворенность",
        "optionText": "5 - Отлично",
        "value": 5
      }
    ]
  }
]
```

---

## 15. Шаблоны опросов

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/templates` | HR, ADMIN |
| `GET` | `/api/templates` | Все |
| `GET` | `/api/templates/{id}` | Все |
| `DELETE` | `/api/templates/{id}?actorId={id}` | HR, ADMIN |

---

## 16. Поддержка

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/support/tickets` | Все |
| `GET` | `/api/support/tickets/my/{userId}` | Все |
| `GET` | `/api/support/tickets/pending` | SUPPORT, ADMIN |
| `PUT` | `/api/support/tickets/{id}/take?supportId={id}` | SUPPORT, ADMIN |
| `PUT` | `/api/support/tickets/{id}/complete?supportId={id}` | SUPPORT, ADMIN |
| `POST` | `/api/support/tickets/{id}/messages` | Все (участники) |
| `GET` | `/api/support/tickets/{id}/chat?actorId={id}` | Участники, SUPPORT |
| `POST` | `/api/support/tickets/{id}/review` | Автор тикета |
| `POST` | `/api/support/tickets/{id}/grant-achievement?supportId={id}&achievementCode=X` | SUPPORT |

**Статусы:** `PENDING`, `IN_PROGRESS`, `COMPLETED`

**POST `/api/support/tickets`:**

```json
{
  "userId": 5,
  "topic": "Не работает VPN",
  "description": "Не могу подключиться"
}
```

**POST `/api/support/tickets/{id}/messages`:**

```json
{
  "senderId": 5,
  "text": "Вот скриншот проблемы",
  "attachmentUrl": "http://localhost:8080/uploads/abc.jpg"
}
```

**POST `/api/support/tickets/{id}/review`:**

```json
{
  "userId": 5,
  "rating": 5,
  "comment": "Быстро помогли!"
}
```

Отзыв начисляет рейтинг сотруднику поддержки.

---

## 17. Чаты сотрудников

| Метод | Путь |
|---|---|
| `POST` | `/api/chats/start` |
| `GET` | `/api/chats/my?userId={id}` |
| `GET` | `/api/chats/{chatId}?userId={id}` |
| `POST` | `/api/chats/{chatId}/messages` |
| `DELETE` | `/api/chats/{chatId}/messages/{messageId}?userId={id}` |

**POST `/api/chats/start`:**

```json
{
  "myId": 5,
  "otherUserId": 6
}
```

**Ответ:**

```json
{
  "chatId": 42,
  "otherUserId": 6,
  "otherUserName": "Мария Верстальщикова",
  "lastMessageText": "Нет сообщений",
  "lastMessageAt": "2026-10-26T11:00:00",
  "unreadCount": 0
}
```

**POST `/api/chats/{chatId}/messages`:**

```json
{
  "senderId": 5,
  "text": "Привет!",
  "attachmentUrl": null
}
```

**GET `/api/chats/{chatId}?userId={id}`** — открывает чат и помечает входящие сообщения как прочитанные.

---

## 18. Сезоны

| Метод | Путь |
|---|---|
| `GET` | `/api/seasons/current?actorId={id}` |
| `GET` | `/api/seasons/leaderboard` |
| `GET` | `/api/seasons/history` |
| `GET` | `/api/seasons/{id}/results` |

**GET `/api/seasons/current`:**

```json
{
  "id": 1,
  "name": "Q4 2026",
  "startDate": "2026-10-01T00:00:00",
  "endDate": "2026-12-31T23:59:59",
  "isActive": true,
  "currentLeaderboard": [
    {
      "place": 1,
      "userId": 5,
      "userName": "Петр Кодеров",
      "currentRating": 87.5,
      "lifetimeRating": 320.5
    }
  ],
  "previousSeasonResults": [
    {
      "place": 1,
      "userName": "Петр Кодеров",
      "finalRating": 150.0,
      "rewardCoins": 7000.00
    }
  ],
  "myDepartmentName": "Backend Team",
  "myDepartmentPlace": 1,
  "myDepartmentLeaderboard": [
    {
      "place": 1,
      "userId": 5,
      "userName": "Петр Кодеров",
      "currentRating": 87.5,
      "lifetimeRating": 320.5
    }
  ],
  "previousDepartmentResults": [
    {
      "place": 1,
      "departmentName": "Backend Team",
      "totalRating": 250.0,
      "memberCount": 3,
      "rewardCoinsPerMember": 5000.00,
      "topMembers": [
        {
          "placeInDepartment": 1,
          "userName": "Петр Кодеров",
          "finalRating": 150.0,
          "rewardCoins": 0.00
        }
      ]
    }
  ]
}
```

**Награды при закрытии сезона:**
- 1 место: **7000** монет + ачивка `SEASON_CHAMPION`
- 2 место: **4000** монет + ачивка `SEASON_TOP_3`
- 3 место: **3000** монет + ачивка `SEASON_TOP_3`
- Победа отдела: **5000** монет каждому участнику
- Топ-3 внутри отдела: ачивки `DEPT_TOP_1`, `DEPT_TOP_2`, `DEPT_TOP_3`

**Сброс рейтинга:** при закрытии сезона `rating` обнуляется, но `lifetimeRating` **никогда не сгорает**.

---

## 19. Ачивки

| Метод | Путь |
|---|---|
| `GET` | `/api/achievements/user/{userId}` |
| `POST` | `/api/achievements/grant` |

**POST `/api/achievements/grant`:**

```json
{
  "actorId": 1,
  "userId": 5,
  "achievementCode": "BEST_EMPLOYEE"
}
```

**Типы ачивок:** `AUTOMATIC` (система), `MANUAL` (выдаёт админ/менеджер)

**Коды ачивок:**
- `EVENT_JOINER_5`, `EVENT_JOINER_10`, `EVENT_JOINER_20` — автоматические
- `SEASON_CHAMPION`, `SEASON_TOP_3` — при закрытии сезона
- `DEPT_TOP_1`, `DEPT_TOP_2`, `DEPT_TOP_3` — топ в подразделении
- `BEST_EMPLOYEE` — ручная (выдаёт админ)

---

## 20. Подразделения

| Метод | Путь | Доступ |
|---|---|---|
| `POST` | `/api/departments` | ADMIN |
| `GET` | `/api/departments` | Все |
| `PUT` | `/api/departments/{deptId}/users/{userId}` | ADMIN |
| `DELETE` | `/api/departments/users/{userId}` | ADMIN |

**POST `/api/departments`:**

```json
{
  "name": "Backend Team",
  "description": "Серверная разработка"
}
```

---

## 21. Казна

| Метод | Путь | Доступ |
|---|---|---|
| `GET` | `/api/treasury` | ADMIN, HR |
| `POST` | `/api/treasury/fund?actorId={id}&amount=X` | ADMIN, HR |

**GET `/api/treasury`:**

```json
{
  "id": 1,
  "balance": 50000.00,
  "lastFundedAt": "2026-10-26T09:00:00"
}
```

**POST `/api/treasury/fund?actorId=1&amount=50000`** — пополнить казну.

Все награды (бонусы, квесты, события, сезонные) списываются из Казны. Если средств нет — операция блокируется с `400`.

---

## 22. Админ-панель

| Метод | Путь | Доступ |
|---|---|---|
| `GET` | `/api/admin/dashboard?actorId={id}` | ADMIN, HR |
| `GET` | `/api/admin/settings?actorId={id}` | ADMIN, HR |
| `PUT` | `/api/admin/settings/{key}?value=X&actorId={id}` | ADMIN |
| `GET` | `/api/admin/tickets?actorId={id}` | ADMIN, HR |
| `GET` | `/api/admin/quests?actorId={id}` | ADMIN, HR |
| `GET` | `/api/admin/quests/pending?actorId={id}` | ADMIN, HR |
| `PUT` | `/api/admin/quests/{id}/approve?actorId={id}` | ADMIN, HR |
| `PUT` | `/api/admin/quests/{id}/reject?actorId={id}&reason=X` | ADMIN, HR |
| `PUT` | `/api/admin/quests/{id}/cancel-completed?actorId={id}&reason=X` | ADMIN |
| `GET` | `/api/admin/transactions?actorId={id}` | ADMIN |
| `PUT` | `/api/admin/transactions/{id}/cancel?actorId={id}&reason=X` | ADMIN |
| `POST` | `/api/admin/penalties` | ADMIN, HR, MANAGER |

**GET `/api/admin/dashboard`:**

```json
{
  "totalUsers": 10,
  "totalProducts": 4,
  "totalCoinsInSystem": 15250.00,
  "pendingOrders": 3,
  "pendingEvents": 1,
  "pendingTickets": 2,
  "pendingQuests": 5
}
```

**POST `/api/admin/penalties`** — штраф:

```json
{
  "actorId": 1,
  "userId": 5,
  "coins": 100.00,
  "rating": 2.0,
  "reason": "Нарушение правил"
}
```

**PUT `/api/admin/transactions/{id}/cancel`** — отмена перевода:

- Средства возвращаются отправителю
- Если это была эмиссия (бонус/награда) — возвращаются в Казну
- Оригинальная транзакция помечается `isCancelled = true`

**GET `/api/admin/settings`:**

```json
[
  { "settingKey": "limit.peer.daily", "settingValue": "1000" },
  { "settingKey": "limit.peer.per_recipient", "settingValue": "1000" },
  { "settingKey": "maintenance_mode", "settingValue": "false" }
]
```

---

## 23. Файлы

### POST `/api/files/upload`

Загрузка изображений (для товаров, категорий, сообщений в тикетах и чатах).

**Content-Type:** `multipart/form-data`

**Поле:** `file` (обязательно, до 5 МБ)

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@photo.jpg"
```

**Ответ `200 OK`** — строка с URL:

```
http://localhost:8080/uploads/170c3d67-b82a-4f8e-bfc0-9153be13eff1.jpg
```

### GET `/uploads/{filename}`

Отдача загруженного файла.

---

## 24. Системные процессы

### Планировщик (каждые 30 секунд)

1. **Завершение событий** — ищет события `ASSIGNED` с `toTime < now`:
    - Списывает из Казны
    - Начисляет `rewardCoins` всем участникам (с проверкой лимита)
    - Начисляет `rewardRating` каждому
    - Создаёт транзакцию `EVENT_REWARD`
    - Статус → `COMPLETED`
    - Флаг `rewardsGranted = true` (идемпотентность)

2. **Штрафы за просроченные опросы** — ищет опросы с `deadline < now`:
    - Находит всех, кто не прошёл
    - Списывает `penaltyRating` (не ниже 0)
    - Создаёт `RatingTransaction`
    - Статус → `isActive = false`

3. **Закрытие сезонов** — ищет `Season.isActive = true` с `endDate < now`:
    - Начисляет награды топ-3 (7000/4000/3000 монет)
    - Награждает победивший отдел (5000 каждому)
    - Выдаёт ачивки
    - Обнуляет `rating`, переносит в `lifetimeRating`
    - Создаёт новый сезон на следующий квартал

### Месячный сброс лимитов

При первом начислении в новом месяце обнуляется `monthlyEarnedCoins`.

---

## 25. Коды ошибок

| Код | Значение | Пример |
|---|---|---|
| `200` | OK | Успешный GET/PUT |
| `201` | Created | Успешный POST |
| `204` | No Content | Успешный DELETE |
| `400` | Bad Request | Ошибка валидации, бизнес-логики |
| `403` | Forbidden | Недостаточно прав |
| `404` | Not Found | Ресурс не найден |
| `500` | Internal Server Error | Непредвиденная ошибка |

**Формат ошибки** — строка:

```json
"Только администратор может присвоить статус ASSIGNED"
```

---

## 26. SQL-миграции

Актуальная схема — в `docker/init.sql` или через `ddl-auto=update`. Ключевые таблицы:

```sql
-- Пользователи
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    rating DOUBLE PRECISION DEFAULT 0.0,
    lifetime_rating DOUBLE PRECISION DEFAULT 0.0,
    balance NUMERIC(19,2) DEFAULT 0,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_earned_coins NUMERIC(19,2) DEFAULT 0,
    last_monthly_reset TIMESTAMP,
    department_id BIGINT REFERENCES departments(id)
);

-- Транзакции монет
CREATE TABLE coin_transactions (
    id BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT REFERENCES users(id),
    to_user_id BIGINT REFERENCES users(id),
    amount NUMERIC(19,2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    comment VARCHAR(500),
    tag VARCHAR(30),
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Рейтинг
CREATE TABLE rating_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount DOUBLE PRECISION NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Казна
CREATE TABLE treasury (
    id BIGINT PRIMARY KEY DEFAULT 1,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    last_funded_at TIMESTAMP
);

-- И другие таблицы: events, quests, surveys, support_tickets, chats, seasons, achievements, departments...
```

Полная схема — в файле `docker/init.sql`.

---

## Быстрый старт

```bash
# 1. Запустить бэкенд
cd E:\ITsprint\tms-backend
docker compose up -d --build

# 2. Запустить фронтенд
cd E:\ITsprint\tms-frontend
npm run dev

# 3. Открыть
http://localhost:5173

# Логин по умолчанию
admin@tms.local / admin123 (или ваш пароль из БД)
```

---

**Последнее обновление:** 2026-10-26  
**Версия API:** 1.0  
**Контакты:** TMS Team