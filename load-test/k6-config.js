// Конфигурация нагрузочного тестирования
export const config = {
    // Общие настройки
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    summaryTimeUnit: 'ms',

    // Настройки для разных типов тестов
    scenarios: {
        smoke: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
            gracefulStop: '30s',
        },
        load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '1m', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '1m', target: 500 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 1000 },
                { duration: '2m', target: 3000 },
                { duration: '1m', target: 5000 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '1m',
        },
        soak: {
            executor: 'constant-arrival-rate',
            rate: 100,
            timeUnit: '1s',
            duration: '1h',
            preAllocatedVUs: 50,
            maxVUs: 200,
        },
    },

    // Пороговые значения
    thresholds: {
        // HTTP метрики
        'http_req_duration{type:api}': ['p(95)<500', 'p(99)<1000'],
        'http_req_duration{type:static}': ['p(95)<200'],
        'http_req_failed': ['rate<0.01'],

        // WebSocket метрики
        'ws_connecting': ['p(95)<100'],
        'ws_msgs_received': ['count>0'],

        // Системные метрики
        'vus': ['value>0'],
        'vus_max': ['value>0'],
    },

    // Теги для группировки метрик
    tags: {
        environment: '${ENV:-local}',
        test_type: '${TEST_TYPE:-load}',
        application: 'platform',
    },

    // Настройки вывода
    output: {
        // JSON файл для анализа
        json: 'results/${TEST_TYPE}-${DATE}.json',

        // CSV для Excel
        csv: 'results/${TEST_TYPE}-${DATE}.csv',

        // InfluxDB для Grafana
        influxdb: {
            url: '${INFLUXDB_URL:-http://localhost:8086}',
            token: '${INFLUXDB_TOKEN}',
            org: '${INFLUXDB_ORG:-platform}',
            bucket: '${INFLUXDB_BUCKET:-k6}',
            precision: 'ms',
        },
    },

    // Настройки запросов
    http: {
        timeout: '30s',
        maxRedirects: 5,
        responseCallback: {
            '2xx': (res) => {
                console.log(`Success: ${res.url} - ${res.status}`);
            },
            '4xx': (res) => {
                console.warn(`Client error: ${res.url} - ${res.status}`);
            },
            '5xx': (res) => {
                console.error(`Server error: ${res.url} - ${res.status}`);
            },
        },
    },

    // Настройки WebSocket
    ws: {
        timeout: '10s',
        maxPayloadSize: 10240,
    },
};

// Утилиты для тестов
export const utils = {
    // Генерация случайных данных
    randomString: (length) => {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    },

    // Генерация случайного email
    randomEmail: () => {
        return `test${Date.now()}${Math.floor(Math.random() * 1000)}@example.com`;
    },

    // Генерация случайного пользователя
    randomUser: () => {
        return {
            username: `user${Date.now()}${Math.floor(Math.random() * 1000)}`,
            email: utils.randomEmail(),
            password: 'TestPassword123!',
        };
    },

    // Задержка с рандомизацией
    randomSleep: (min, max) => {
        const delay = Math.floor(Math.random() * (max - min + 1)) + min;
        return sleep(delay / 1000);
    },

    // Проверка ответа
    checkResponse: (res, expectedStatus) => {
        return check(res, {
            [`status ${expectedStatus}`]: (r) => r.status === expectedStatus,
            'response time OK': (r) => r.timings.duration < 1000,
        });
    },
};

// Экспорт для использования в тестах
export default { config, utils };