#!/bin/bash

# Настройка Keycloak с 9 ролями для платформы
# Запуск: ./init-keycloak.sh

KEYCLOAK_URL="http://localhost:8080"
KEYCLOAK_ADMIN="admin"
KEYCLOAK_ADMIN_PASSWORD="admin123"
REALM_NAME="platform"

echo "=== Настройка Keycloak для платформы ==="

# Получение токена администратора
echo "1. Получение токена администратора..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=$KEYCLOAK_ADMIN" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
  -d "grant_type=password" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "Ошибка: Не удалось получить токен администратора"
    exit 1
fi
echo "Токен получен"

# Создание Realm
echo "2. Создание Realm: $REALM_NAME..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"realm\": \"$REALM_NAME\",
    \"enabled\": true,
    \"displayName\": \"Platform\",
    \"accessTokenLifespan\": 300,
    \"ssoSessionIdleTimeout\": 1800
  }"

# Получение токена для нового Realm
echo "3. Получение токена для Realm $REALM_NAME..."
REALM_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM_NAME/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=$KEYCLOAK_ADMIN" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
  -d "grant_type=password" | jq -r '.access_token')

# Создание 9 ролей
echo "4. Создание 9 ролей..."

ROLES=(
  "USER_0:Зритель без прав:Только просмотр help и profile"
  "USER_1:Новичок:Базовый доступ"
  "USER_2:Продвинутый фанат:Расширенный доступ (ручное назначение)"
  "CONSULTANT_PITANIE:Консультант по питанию:Может создавать контент по питанию"
  "CONSULTANT_SPORT:Консультант по спорту:Может создавать спортивный контент"
  "MODERATOR_ADMIN:Модератор/Админ:Управление контентом и чатом"
  "DEVELOPER:Разработчик:Технический доступ"
  "VYACHESLAV:Вячеслав:Владелец платформы"
  "SUPER_ADMIN:Главный Админ:Полный доступ"
)

for role in "${ROLES[@]}"; do
  IFS=':' read -r name display desc <<< "$role"
  echo "  Создание роли: $name"
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
    -H "Authorization: Bearer $REALM_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"$desc\"
    }"
done

# Создание тестовых пользователей
echo "5. Создание тестовых пользователей..."

# USER_0 - без прав
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"user0\",
    \"email\": \"user0@platform.com\",
    \"firstName\": \"Зритель\",
    \"lastName\": \"Без прав\",
    \"enabled\": true,
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"password\",
      \"temporary\": false
    }]
  }"

# USER_1 - новичок
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"user1\",
    \"email\": \"user1@platform.com\",
    \"firstName\": \"Новичок\",
    \"lastName\": \"Пользователь\",
    \"enabled\": true,
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"password\",
      \"temporary\": false
    }]
  }"

# USER_2 - продвинутый
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"user2\",
    \"email\": \"user2@platform.com\",
    \"firstName\": \"Продвинутый\",
    \"lastName\": \"Фанат\",
    \"enabled\": true,
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"password\",
      \"temporary\": false
    }]
  }"

# SUPER_ADMIN
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"superadmin\",
    \"email\": \"admin@platform.com\",
    \"firstName\": \"Главный\",
    \"lastName\": \"Админ\",
    \"enabled\": true,
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"admin123\",
      \"temporary\": false
    }]
  }"

echo ""
echo "=== Назначение ролей пользователям ==="

# Функция для назначения роли пользователю
assign_role() {
  local username=$1
  local role_name=$2

  # Получение ID пользователя
  USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=$username" \
    -H "Authorization: Bearer $REALM_TOKEN" | jq -r '.[0].id')

  # Получение ID роли
  ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/$role_name" \
    -H "Authorization: Bearer $REALM_TOKEN" | jq -r '.id')

  # Назначение роли
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
    -H "Authorization: Bearer $REALM_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[{
      \"id\": \"$ROLE_ID\",
      \"name\": \"$role_name\"
    }]"

  echo "  $username -> $role_name"
}

assign_role "user0" "USER_0"
assign_role "user1" "USER_1"
assign_role "user2" "USER_2"
assign_role "superadmin" "SUPER_ADMIN"
assign_role "superadmin" "VYACHESLAV"

echo ""
echo "=== Создание клиента для фронтенда ==="
CLIENT_ID=$(curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"platform-frontend\",
    \"name\": \"Platform Frontend\",
    \"publicClient\": true,
    \"directAccessGrantsEnabled\": true,
    \"redirectUris\": [\"http://localhost:3000/*\"],
    \"webOrigins\": [\"http://localhost:3000\"]
  }" -v 2>&1 | grep -o '<.*>' || echo "Клиент создан")

echo ""
echo "=== Создание клиента для бэкенда ==="
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
  -H "Authorization: Bearer $REALM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"platform-backend\",
    \"name\": \"Platform Backend\",
    \"publicClient\": false,
    \"serviceAccountsEnabled\": true,
    \"directAccessGrantsEnabled\": true
  }"

echo ""
echo "=== НАСТРОЙКА KEYCLOAK ЗАВЕРШЕНА ==="
echo ""
echo "Пользователи:"
echo "  user0 / password - роль USER_0 (только помощь)"
echo "  user1 / password - роль USER_1 (новичок)"
echo "  user2 / password - роль USER_2 (продвинутый)"
echo "  superadmin / admin123 - роль SUPER_ADMIN + VYACHESLAV"
echo ""
echo "Realm: $REALM_NAME"
echo "URL: $KEYCLOAK_URL/realms/$REALM_NAME"
echo "Frontend client: platform-frontend"
echo "Backend client: platform-backend"