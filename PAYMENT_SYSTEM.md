# 💳 Система оплаты через СБП - ГОТОВО!

## ✅ Что реализовано

### Frontend (React)
- ✅ Новые тарифы с прогрессивными скидками
- ✅ Генерация уникального ID платежа
- ✅ QR-код для оплаты через СБП
- ✅ Копирование реквизитов в буфер обмена
- ✅ Проверка статуса платежа
- ✅ Красивый UI с инструкциями

### Backend (Spring Boot)
- ✅ Payment entity и repository
- ✅ PaymentService с бизнес-логикой
- ✅ PaymentController с REST API
- ✅ KeycloakService для обновления ролей
- ✅ Liquibase миграция для таблицы payments

### Интеграция с Keycloak
- ✅ Автоматическое обновление роли USER → PREMIUM
- ✅ Keycloak Admin Client API

## 💰 Тарифы

| Период | Цена | Цена/мес | Скидка | Экономия |
|--------|------|----------|--------|----------|
| 1 месяц | 10 000 ₽ | 10 000 ₽ | - | - |
| 3 месяца | 27 000 ₽ | 9 000 ₽ | 10% | 3 000 ₽ |
| 6 месяцев | 48 000 ₽ | 8 000 ₽ | 20% | 12 000 ₽ |
| 12 месяцев | 84 000 ₽ | 7 000 ₽ | 30% | 36 000 ₽ |

## 🔄 Процесс оплаты

### 1. Пользователь выбирает тариф
```
User (user0) → Личный кабинет → Выбор тарифа → Нажимает "Оплатить"
```

### 2. Генерация платежа
```javascript
// Frontend создает запрос
POST /api/payments/create
{
  "subscriptionMonths": 3
}

// Backend генерирует:
- Уникальный ID: PAY-1712559600000-A1B2C3D4
- QR-код для СБП
- Срок действия: 24 часа
```

### 3. Оплата
**Вариант А: Через QR-код (рекомендуется)**
1. Пользователь сканирует QR-код в приложении банка
2. Подтверждает платеж
3. Деньги поступают на счет

**Вариант Б: По реквизитам**
1. Пользователь копирует реквизиты
2. Делает перевод в банке
3. В назначении платежа указывает ID: PAY-1712559600000-A1B2C3D4

### 4. Подтверждение платежа

**Автоматическое (будущее):**
- Webhook от банка
- Автоматическая проверка
- Мгновенное обновление роли

**Ручное (текущее):**
1. Админ заходит в админ-панель
2. Видит список ожидающих платежей
3. Проверяет в банке по ID платежа
4. Подтверждает через API
5. Роль автоматически обновляется в Keycloak

## 📡 API Endpoints

### Для пользователей:

**Создать платеж:**
```bash
POST /api/payments/create
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "subscriptionMonths": 3
}

# Ответ:
{
  "id": "PAY-1712559600000-A1B2C3D4",
  "amount": 27000,
  "currency": "RUB",
  "subscriptionMonths": 3,
  "status": "PENDING",
  "qrCodeData": "ST00012|Name=БФАДД|...",
  "expiresAt": "2026-04-09T10:30:00",
  "createdAt": "2026-04-08T10:30:00"
}
```

**Проверить статус:**
```bash
GET /api/payments/{paymentId}/status
Authorization: Bearer {JWT}

# Ответ:
{
  "id": "PAY-1712559600000-A1B2C3D4",
  "status": "PENDING" | "CONFIRMED" | "EXPIRED"
}
```

**Мои платежи:**
```bash
GET /api/payments/my
Authorization: Bearer {JWT}
```

### Для админов:

**Список ожидающих платежей:**
```bash
GET /api/payments/pending
Authorization: Bearer {ADMIN_JWT}

# Ответ:
[
  {
    "id": "PAY-1712559600000-A1B2C3D4",
    "userId": "user-123",
    "amount": 27000,
    "subscriptionMonths": 3,
    "status": "PENDING",
    "createdAt": "2026-04-08T10:30:00"
  }
]
```

**Подтвердить платеж:**
```bash
POST /api/payments/{paymentId}/confirm
Authorization: Bearer {ADMIN_JWT}
Content-Type: application/json

{
  "notes": "Проверено в Сбербанк Онлайн, платеж поступил"
}

# Результат:
- Статус платежа: PENDING → CONFIRMED
- Роль пользователя в Keycloak: USER → PREMIUM
- Пользователь получает доступ к контенту
```

## 🗄️ База данных

### Таблица payments:

```sql
CREATE TABLE payments (
  id VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'RUB',
  subscription_months INTEGER NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  payment_method VARCHAR(50),
  bank_transaction_id VARCHAR(255),
  qr_code_data TEXT,
  expires_at TIMESTAMP,
  paid_at TIMESTAMP,
  confirmed_by VARCHAR(255),
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
```

### Статусы платежа:

- **PENDING** - Ожидает оплаты
- **PAID** - Оплачен (автоматически через webhook)
- **CONFIRMED** - Подтвержден админом, роль обновлена
- **EXPIRED** - Истек срок (24 часа)
- **CANCELLED** - Отменен
- **REFUNDED** - Возврат

## 🔐 Безопасность

### Проверки:
- ✅ JWT аутентификация на всех endpoints
- ✅ Пользователь видит только свои платежи
- ✅ Только админы могут подтверждать платежи
- ✅ Уникальный ID для каждого платежа
- ✅ Срок действия платежа 24 часа

### Логирование:
```java
log.info("Payment created: {} for user: {}, amount: {}", paymentId, userId, amount);
log.info("Payment confirmed: {} by admin: {}", paymentId, adminId);
log.info("User role updated: {} -> PREMIUM", userId);
```

## 🎯 Как использовать

### 1. Настроить Keycloak

В `application.yaml`:
```yaml
keycloak:
  auth-server-url: http://localhost:8080
  realm: platform-realm
  resource: platform-backend
  credentials:
    secret: your-client-secret
```

### 2. Запустить миграции

```bash
# Включить Liquibase
spring.liquibase.enabled: true

# Запустить приложение
mvn spring-boot:run
```

### 3. Тестирование

**Создать платеж:**
```bash
curl -X POST http://localhost:8081/api/payments/create \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"subscriptionMonths": 1}'
```

**Подтвердить платеж (админ):**
```bash
curl -X POST http://localhost:8081/api/payments/PAY-XXX/confirm \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Платеж проверен"}'
```

**Проверить роль пользователя:**
```sql
SELECT * FROM users WHERE id = 'user-123';
-- role должна измениться на PREMIUM
```

## 📱 Frontend интеграция

```javascript
// Создание платежа
const createPayment = async (months) => {
  const response = await fetch('/api/payments/create', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ subscriptionMonths: months })
  });
  
  const payment = await response.json();
  
  // Генерируем QR-код
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(payment.qrCodeData)}`;
  
  return { payment, qrUrl };
};

// Проверка статуса
const checkStatus = async (paymentId) => {
  const response = await fetch(`/api/payments/${paymentId}/status`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const payment = await response.json();
  
  if (payment.status === 'CONFIRMED') {
    // Роль обновлена, перезагружаем страницу
    window.location.reload();
  }
};
```

## 🚀 Что дальше

### Для MVP (текущее):
✅ Ручное подтверждение админом
✅ QR-коды СБП
✅ Автоматическое обновление ролей

### Для автоматизации (будущее):
- [ ] Интеграция с банковским API
- [ ] Webhook от банка
- [ ] Автоматическая проверка платежей
- [ ] Email уведомления
- [ ] История платежей в личном кабинете

### Для масштабирования:
- [ ] Интеграция с ЮKassa/CloudPayments
- [ ] Поддержка банковских карт
- [ ] Рекуррентные платежи (автопродление)
- [ ] Промокоды и скидки
- [ ] Реферальная программа

## ⚠️ Важно

1. **QR-код СБП** - упрощенный формат для демонстрации
   - В продакшене нужен официальный формат СБП
   - Или интеграция с платежным агрегатором

2. **Ручное подтверждение** - временное решение
   - Админ должен проверять платежи в банке
   - Указывать ID платежа в поиске

3. **Безопасность реквизитов**
   - Реквизиты хранятся в коде (bankDetails)
   - Лучше вынести в конфигурацию или БД

4. **Срок действия платежа** - 24 часа
   - После истечения статус меняется на EXPIRED
   - Нужно создать новый платеж

## 📞 Поддержка

При проблемах с оплатой:
1. Пользователь сообщает ID платежа
2. Админ ищет в БД: `SELECT * FROM payments WHERE id = 'PAY-XXX'`
3. Проверяет в банке по ID
4. Подтверждает через API

---

**Готово к использованию!** 🎉

Система оплаты работает, роли обновляются автоматически после подтверждения админом.
