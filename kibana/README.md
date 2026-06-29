# Kibana Dashboards - Путь Сердца Platform

This directory contains pre-configured Kibana saved objects for the Путь Сердца (Path of the Heart) platform backend logs.

## Contents

- **saved_objects.json** - NDJSON file containing all dashboard components

## Dashboard Components

The "Platform Backend Logs" dashboard includes:

### Visualizations

1. **Log Level Distribution** (Pie Chart)
   - Shows distribution of log levels (INFO, WARN, ERROR, DEBUG)
   - Based on `log_level` field

2. **Error Rate Over Time** (Line Chart)
   - Tracks count of ERROR logs over time
   - Filtered to show only ERROR level logs

3. **Top Loggers** (Bar Chart)
   - Displays top 10 logger classes by log count
   - Based on `logger_name` field

4. **Host/Service Distribution** (Bar Chart)
   - Shows distribution of applications/services
   - Based on `application` field

5. **Error Messages Table** (Data Table)
   - Lists top 20 error messages
   - Filtered to show only ERROR level logs

## Index Pattern

- **Pattern:** `platform-logs-*`
- **Time Field:** `@timestamp`
- **Description:** Matches all daily platform log indices (e.g., `platform-logs-2026.06.18`)

## Log Fields Reference

The dashboard expects the following fields from Spring Boot logs:

| Field | Type | Description |
|-------|------|-------------|
| `@timestamp` | date | Log timestamp |
| `log_level` | keyword | Log level (INFO, WARN, ERROR, DEBUG) |
| `logger_name` | keyword | Logger class name |
| `message` | text | Log message content |
| `traceId` | keyword | Distributed trace ID |
| `spanId` | keyword | Trace span ID |
| `application` | keyword | Application name (auto-added: platform-backend) |
| `environment` | keyword | Environment (auto-added: prod) |

## Import Instructions

### Method 1: Kibana UI

1. Open Kibana at `http://localhost:5601`
2. Navigate to **Stack Management** → **Saved Objects**
3. Click **Import** button
4. Select `saved_objects.json` from this directory
5. Click **Import** and confirm

### Method 2: Kibana API

```bash
# Import saved objects via API
curl -X POST "http://localhost:5601/api/saved_objects/_import" \
  -H "kbn-xsrf: true" \
  --form file=@saved_objects.json
```

### Method 3: Docker Compose Setup

If Kibana is running in Docker, copy the file into the container first:

```bash
# Copy to Kibana container
docker cp saved_objects.json platform-kibana:/tmp/saved_objects.json

# Import via API
docker exec platform-kibana curl -X POST "http://localhost:5601/api/saved_objects/_import" \
  -H "kbn-xsrf: true" \
  --form file=@/tmp/saved_objects.json
```

## Accessing the Dashboard

After import:

1. Go to **Analytics** → **Dashboard**
2. Find "Platform Backend Logs" in the list
3. Click to open

The dashboard will automatically show logs from the last 24 hours. You can adjust the time range using the time picker in the top-right corner.

## Troubleshooting

### No Data Appears

1. Verify Logstash is running: `docker ps | grep logstash`
2. Check Elasticsearch has data: `curl http://localhost:9200/platform-logs-*/_count`
3. Ensure backend is sending logs to Logstash on port 5000

### Import Errors

If you get errors during import:

1. Ensure Kibana version matches (8.11.0)
2. Check Kibana is fully started and healthy
3. Verify you have admin access to Kibana

### Missing Fields

If visualizations show "No data":

1. Check index pattern matches: `platform-logs-*`
2. Verify fields exist: `curl http://localhost:9200/platform-logs-*/_mapping | grep log_level`
3. Re-index logs if needed

## Customization

To modify the dashboard:

1. Open the dashboard in Kibana
2. Edit individual visualizations as needed
3. Save changes
4. Export updated saved objects via Stack Management → Saved Objects → Export
