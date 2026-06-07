import { useEffect, useRef, useState, useCallback } from 'react';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';

const useWebSocket = (channel, onMessage) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState(null);
    const wsRef = useRef(null);
    const onMessageRef = useRef(onMessage);
    onMessageRef.current = onMessage;

    const send = useCallback((data) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify(data));
        }
    }, []);

    useEffect(() => {
        const wsBase = API_URL.replace(/^http/, 'ws').replace(/\/api$/, '');
        const url = `${wsBase}/ws/${channel}`;

        const connect = () => {
            if (wsRef.current && wsRef.current.readyState !== WebSocket.CLOSED) {
                wsRef.current.close();
            }

            const ws = new WebSocket(url);
            wsRef.current = ws;

            ws.onopen = () => {
                setIsConnected(true);
                setError(null);
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    if (onMessageRef.current) {
                        onMessageRef.current(data);
                    }
                } catch (e) {
                    console.error('Failed to parse WS message:', e);
                }
            };

            ws.onerror = (err) => {
                console.error('WebSocket error:', err);
                setError('WebSocket connection error');
            };

            ws.onclose = () => {
                setIsConnected(false);
                // Attempt reconnection after 3 seconds
                setTimeout(() => {
                    connect();
                }, 3000);
            };
        };

        connect();

        return () => {
            if (wsRef.current) {
                wsRef.current.onclose = null; // prevent reconnect on unmount
                wsRef.current.close();
                wsRef.current = null;
            }
        };
    }, [channel]);

    return { isConnected, send, error };
};

export default useWebSocket;
