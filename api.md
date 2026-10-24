# API Documentation — TMS Backend

Базовый URL: `http://localhost:8080`
Все запросы и ответы передаются в формате `application/json`.

> **Примечание по безопасности (MVP):** На текущем этапе идентификация пользователя (проверка прав) происходит через передачу `actorId` в query-параметрах или теле запроса. Внедрение Spring Security и JWT запланировано на следующую итерацию.

---

## Содержание
1. [Общие сведения и Enums](#1-общие-сведения-и-enums)
2. [Ролевая модель (RBAC)](#2-ролевая-модель-rbac)
3. [Пользователи (Users)](#3-пользователи-users)
4. [Экономика (Transfers & Grants)](#4-экономика-transfers--grants)
5. [События (Events)](#5-события-events)
6. [Участники событий (Participants)](#6-участники-событий-participants)
7. [Магазин и Заказы (Shop)](#7-магазин-и-заказы-shop)
8. [Фоновые процессы](#8-фоновые-процессы)

---

## 1. Общие сведения и Enums

### Формат даты
Все даты передаются в стандарте ISO-8601 без таймзоны: `2026-11-01T10:00:00`.

### Перечисления (Enums)

| Enum | Значения | Описание |
|---|---|---|
| **EventStatus** | `NEW`, `ASSIGNED`, `COMPLETED`, `CANCELLED` | Жизненный цикл события |
| **CoinTransactionType** | `PEER_THANKS`, `EVENT_REWARD`, `MANAGER_BONUS`, `PURCHASE`, `ADMIN_ADJUSTMENT`, `REFUND` | Тип движения монет в журнале |
| **ProductType** | `PHYSICAL`, `PERK`, `DIGITAL` | Тип товара (физический, услуга, цифровой) |
| **OrderStatus** | `PENDING`, `APPROVED`, `COMPLETED`, `REJECTED`, `CANCELLED` | Жизненный цикл заказа в магазине |

---

## 2. Ролевая модель (RBAC)

В системе используются роли, каждая из которых имеет набор прав (`Permission`). Проверки в коде идут по правам, а не по названию роли.

| Роль | Ключевые права (Permissions) |
|---|---|
| `EMPLOYEE` | Создание событий, переводы монет (`PEER_THANKS`), покупки в магазине |
| `ORGANIZER` | Всё как `EMPLOYEE` + редактирование любых событий |
| `MANAGER` | Всё как `EMPLOYEE` + начисление бонусов (`MANAGER_BONUS`), управление заказами (`ORDER_MANAGE`) |
| `HR` | Одобрение событий (`EVENT_APPROVE`), просмотр всех `NEW`, управление заказами |
| `ADMIN` | Полный доступ ко всем функциям, управление каталогом (`CATALOG_MANAGE`), ручная правка баланса |

---

## 3. Пользователи (Users)

### Таблица эндпоинтов
| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/users` | Создать пользователя | Все |
| `GET` | `/api/users` | Список всех пользователей | Все |
| `GET` | `/api/users/{id}` | Профиль пользователя | Все |
| `GET` | `/api/users/{id}/transactions` | История операций (пагинация) | Все |

### `POST /api/users`
Создает пользователя. Пароль сохраняется, но не возвращается в ответе.
**Тело запроса:**
```json
{
  "name": "Иван Иванов",
  "email": "ivan@tms.local",
  "password": "securepassword",
  "role": "EMPLOYEE",
  "balance": 0
}
```

### `GET /api/users/{id}/transactions`
Возвращает историю движения монет (журнал `coin_transactions`).
**Query parameters:** `page` (default `0`), `size` (default `20`).
**Пример ответа:**
```json
{
  "content": [
    {
      "id": 42,
      "fromUserId": null,
      "toUserId": 1,
      "amount": 100.00,
      "type": "EVENT_REWARD",
      "comment": "Награда за событие #5",
      "createdAt": "2026-10-18T15:30:00"
    }
  ],
  "totalElements": 1
}
```

---

## 4. Экономика (Transfers & Grants)

### Таблица эндпоинтов
| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/transfers` | Перевод между коллегами | `EMPLOYEE`+ |
| `POST` | `/api/transfers/grant` | Начисление бонусов (эмиссия) | `MANAGER`, `ADMIN` |

### `POST /api/transfers`
Перевод монет от сотрудника к сотруднику. Действуют лимиты: 50 монет одному коллеге в день, 200 монет в сумме.
**Тело запроса:**
```json
{
  "fromUserId": 1,
  "toUserId": 2,
  "amount": 30.00,
  "comment": "Спасибо за помощь с релизом!"
}
```

### `POST /api/transfers/grant`
Начисление премий. У менеджеров есть дневные лимиты (500 одному, 2000 в сумме).
**Тело запроса:**
```json
{
  "actorId": 5,
  "toUserId": 1,
  "amount": 500.00,
  "comment": "Квартальная премия"
}
```

---

## 5. События (Events)

### Таблица эндпоинтов
| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/events` | Создать событие | `EMPLOYEE`+ |
| `PUT` | `/api/events/{id}/assign` | Одобрить (NEW → ASSIGNED) | `HR`, `ADMIN` |
| `PUT` | `/api/events/{id}/cancel` | Отменить событие | `EMPLOYEE`+ |
| `GET` | `/api/events` | Список событий | Все (скрывает `NEW` от обычных) |
| `GET` | `/api/events/organizer/{organizerId}`| События организатора | Все |

### `POST /api/events`
Создает мероприятие. Если `paidInCoins: true`, после завершения участникам начислятся монеты и рейтинг.
**Тело запроса:**
```json
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

### `PUT /api/events/{id}/assign`
**Тело запроса:** `{ "actorId": 2 }` (требует права `EVENT_APPROVE`).

---

## 6. Участники событий (Participants)

### Таблица эндпоинтов
| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/events/{eventId}/participants` | Записаться на событие | `EMPLOYEE`+ |
| `DELETE` | `/api/events/{eventId}/participants/{userId}` | Отписаться | `EMPLOYEE`+ |
| `GET` | `/api/events/{eventId}/participants` | Список участников | Все |

### `POST /api/events/{eventId}/participants`
Записаться можно только на событие в статусе `ASSIGNED`. Организатор не может быть участником.
**Тело запроса:** `{ "userId": 4 }`

---

## 7. Магазин и Заказы (Shop)

### Таблица эндпоинтов
| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/shop/products?actorId={id}` | Создать товар | `ADMIN` |
| `GET` | `/api/shop/products` | Витрина товаров | Все |
| `POST` | `/api/shop/purchase` | Купить товар | Все |
| `GET` | `/api/shop/orders/{userId}` | История заказов | Все |
| `GET` | `/api/shop/orders/pending?actorId={id}` | Заказы на модерации | `MANAGER`, `HR`, `ADMIN` |
| `PUT` | `/api/shop/orders/{orderId}/approve?actorId={id}` | Подтвердить заказ | `MANAGER`, `HR`, `ADMIN` |
| `PUT` | `/api/shop/orders/{orderId}/reject?actorId={id}&reason={text}` | Отклонить (возврат) | `MANAGER`, `HR`, `ADMIN` |
| `PUT` | `/api/shop/orders/{orderId}/complete?actorId={id}` | Завершить заказ | `MANAGER`, `HR`, `ADMIN` |

### `POST /api/shop/products`
Только для администратора.
**Тело запроса:**
```json
{
  "name": "Отгул на полдня",
  "description": "Возможность уйти домой пораньше",
  "price": 1000.00,
  "stock": 10,
  "imageUrl": "https://example.com/perk.png"
}
```

### `POST /api/shop/purchase`
Списывает монеты (создает транзакцию `PURCHASE`) и резервирует товар. Заказ получает статус `PENDING`.
**Тело запроса:**
```json
{
  "userId": 4,
  "productId": 1,
  "quantity": 1
}
```

### `GET /api/shop/orders/pending?actorId=5`
Менеджер получает список заказов, которые нужно выдать или одобрить (статус `PENDING`).

### `PUT /api/shop/orders/{orderId}/approve?actorId=5`
Менеджер подтверждает заказ. Статус меняется на `APPROVED`.
*Опционально: `&comment=Согласовано`*.

### `PUT /api/shop/orders/{orderId}/reject?actorId=5&reason=Нет%20слотов`
Менеджер отклоняет заказ. Статус меняется на `REJECTED`.
**Логика:**
1. Товар возвращается на склад (stock += quantity).
2. Монеты возвращаются на баланс пользователя.
3. Создается транзакция типа `REFUND`.

### `PUT /api/shop/orders/{orderId}/complete?actorId=5`
Менеджер подтверждает, что услуга оказана / товар выдан. Статус меняется на `COMPLETED`.

---

## 8. Фоновые процессы

### Автоматическое завершение событий (Scheduler)
Каждые 30 секунд планировщик (`EventSchedulerService`) проверяет базу данных:
1. Находит события со статусом `ASSIGNED`, у которых `toTime` уже в прошлом.
2. Если у события `paidInCoins = true`, всем участникам начисляются `rewardCoins` (в баланс) и `rewardRating` (в рейтинг).
3. Создается запись в журнале транзакций с типом `EVENT_REWARD`.
4. Событие переводится в статус `COMPLETED`, а флаг `rewardsGranted` становится `true` (защита от повторного начисления).