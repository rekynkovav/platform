# docker/nginx/ssl/generate-certs.sh
#!/bin/bash

# Создание самоподписанных сертификатов для разработки

# Для platform.local
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout platform.key -out platform.crt \
    -subj "/C=RU/ST=Moscow/L=Moscow/O=Platform/CN=platform.local"

# Для api.platform.local
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout api.key -out api.crt \
    -subj "/C=RU/ST=Moscow/L=Moscow/O=Platform/CN=api.platform.local"

# Для auth.platform.local
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout auth.key -out auth.crt \
    -subj "/C=RU/ST=Moscow/L=Moscow/O=Platform/CN=auth.platform.local"

echo "Certificates generated successfully"