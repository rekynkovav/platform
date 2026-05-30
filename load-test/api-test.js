import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// Тестовые данные
const users = new SharedArray('users', function() {
    return JSON.parse(open('./test-users.json'));
});

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function() {
    const user = users[Math.floor(Math.random() * users.length)];
    const baseUrl = 'http://localhost:8081/api';

    // 1. Получить токен
    const authRes = http.post(`${baseUrl}/auth/login`, JSON.stringify({
        username: user.username,
        password: user.password
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(authRes, {
        'auth successful': (r) => r.status === 200,
    });

    const token = authRes.json('access_token');
    if (!token) {
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    // 2. Получить профиль
    const profileRes = http.get(`${baseUrl}/users/me`, { headers });
    check(profileRes, {
        'profile fetched': (r) => r.status === 200,
    });

    // 3. Получить список постов
    const postsRes = http.get(`${baseUrl}/posts?page=0&size=10`, { headers });
    check(postsRes, {
        'posts fetched': (r) => r.status === 200,
    });

    // 4. Отправить сообщение в чат (с вероятностью 30%)
    if (Math.random() < 0.3) {
        const chatRes = http.post(`${baseUrl}/chat/messages`, JSON.stringify({
            roomSlug: 'general',
            content: `Test message from ${user.username} at ${Date.now()}`
        }), { headers });

        check(chatRes, {
            'message sent': (r) => r.status === 201,
        });
    }

    // 5. Получить активные трансляции
    const broadcastsRes = http.get(`${baseUrl}/broadcasts/active`, { headers });
    check(broadcastsRes, {
        'broadcasts fetched': (r) => r.status === 200,
    });

    sleep(1);
}

export function setup() {
    console.log('Starting API load test...');
    // Создаем тестовых пользователей если файла нет
    const testUsers = [];
    for (let i = 0; i < 100; i++) {
        testUsers.push({
            username: `testuser${i}`,
            password: 'password123',
            email: `test${i}@example.com`
        });
    }

    // Сохраняем в файл для использования в тесте
    const fs = require('fs');
    fs.writeFileSync('./test-users.json', JSON.stringify(testUsers));
}

export function teardown() {
    console.log('API load test completed');
}