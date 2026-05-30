#!/bin/bash

# Ожидание запуска Grafana
echo "Waiting for Grafana to start..."
sleep 15

GRAFANA_URL="http://admin:admin@localhost:3001"

# Создать источник данных Prometheus
echo "Creating Prometheus datasource..."
curl -X POST "${GRAFANA_URL}/api/datasources" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prometheus",
    "type": "prometheus",
    "url": "http://prometheus:9090",
    "access": "proxy",
    "isDefault": true
  }'

# Импортировать JVM dashboard (ID: 4701)
echo "Importing JVM dashboard..."
curl -X POST "${GRAFANA_URL}/api/dashboards/db" \
  -H "Content-Type: application/json" \
  -d '{
    "dashboard": {
      "id": null,
      "title": "JVM Monitoring - Platform"
    },
    "overwrite": true,
    "inputs": [{
      "name": "DS_PROMETHEUS",
      "type": "datasource",
      "pluginId": "prometheus",
      "value": "Prometheus"
    }]
  }'

# Импортировать Spring Boot dashboard (ID: 12900)
echo "Importing Spring Boot dashboard..."
curl -X POST "${GRAFANA_URL}/api/dashboards/db" \
  -H "Content-Type: application/json" \
  -d '{
    "dashboard": {
      "id": null,
      "title": "Spring Boot Monitoring"
    },
    "overwrite": true,
    "inputs": [{
      "name": "DS_PROMETHEUS",
      "type": "datasource",
      "pluginId": "prometheus",
      "value": "Prometheus"
    }]
  }'

echo "Grafana setup complete!"