import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 1000 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        ws_connecting: ['p(95)<100'],
        ws_msgs_received: ['count>0'],
    },
};

export default function() {
    const url = 'ws://localhost:8081/ws/chat';
    const response = ws.connect(url, {}, function(socket) {
        socket.on('open', () => {
            socket.send(JSON.stringify({
                roomSlug: 'general',
                userId: `user_${__VU}`,
                content: 'Test message from load test'
            }));
        });

        socket.on('message', (msg) => {
            console.log(`VU ${__VU} received: ${msg}`);
        });

        socket.on('close', () => {
            console.log(`VU ${__VU} disconnected`);
        });

        socket.on('error', (e) => {
            console.error(`VU ${__VU} error: ${e}`);
        });

        socket.setTimeout(() => {
            socket.close();
        }, 10000);
    });

    sleep(1);
}

export function setup() {
    console.log('Starting WebSocket load test...');
}

export function teardown() {
    console.log('WebSocket load test completed');
}