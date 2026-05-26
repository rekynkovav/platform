#!/bin/bash
echo "=== ПРОВЕРКА СБОРКИ С ГРУППОЙ io.github.bucket4j ==="
echo ""
echo "1. Проверяем версию bucket4j в pom.xml:"
grep -A2 "bucket4j" pom.xml
echo ""
echo "2. Запускаем mvn clean compile:"
apt-get update && apt-get install -y maven
mvn clean compile -DskipTests 2>&1 | tail -50
echo ""
echo "3. Проверяем результат:"
if [ -f "target/classes/com/platform/PlatformApplication.class" ]; then
    echo "✅ КОМПИЛЯЦИЯ УСПЕШНА"
else
    echo "❌ КОМПИЛЯЦИЯ НЕ УДАЛАСЬ"
fi