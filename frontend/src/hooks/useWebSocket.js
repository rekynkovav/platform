import { useState, useEffect, useRef, useCallback } from 'react';
import { getToken, updateToken } from '../keycloak';

const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8081/ws';

export const useWebSocket = (roomSlug, onMessage, options = {}) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState(null);
    const wsRef = useRef(null);
    const reconnectAttemptsRef = useRef(0);
    const reconnectTimeoutRef = useRef(null);
    const maxReconnectAttempts = options.maxReconnectAttempts || 10;
    const reconnectDelay = options.reconnectDelay || 1000;

    const connect = useCallback(async () => {
        if (wsRef.current?.readyState === WebSocket.OPEN) return;

        try {
            let token = getToken();
            if (token && await updateToken()) {
                token = getToken();
            }

            const wsUrl = `${WS_URL}/chat?room=${roomSlug}&token=${token}`;
            const ws = new WebSocket(wsUrl);
            wsRef.current = ws;

            ws.onopen = () => {
                console.log(`WebSocket connected to room: ${roomSlug}`);
                setIsConnected(true);
                setError(null);
                reconnectAttemptsRef.current = 0;
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    onMessage?.(data);
                } catch (err) {
                    console.error('Failed to parse WebSocket message:', err);
                }
            };

            ws.onerror = (event) => {
                console.error('WebSocket error:', event);
                setError('Connection error');
            };

            ws.onclose = (event) => {
                console.log(`WebSocket disconnected: ${event.code} - ${event.reason}`);
                setIsConnected(false);

                if (event.code !== 1000) { // Normal closure
                    scheduleReconnect();
                }
            };
        } catch (err) {
            console.error('Failed to create WebSocket:', err);
            setError(err.message);
            scheduleReconnect();
        }
    }, [roomSlug, onMessage]);

    const scheduleReconnect = useCallback(() => {
        if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
            console.error('Max reconnect attempts reached');
            setError('Unable to reconnect after multiple attempts');
            return;
        }

        if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);

        const delay = Math.min(reconnectDelay * Math.pow(2, reconnectAttemptsRef.current), 30000);
        console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current + 1})`);

        reconnectTimeoutRef.current = setTimeout(() => {
            reconnectAttemptsRef.current++;
            connect();
        }, delay);
    }, [connect, maxReconnectAttempts, reconnectDelay]);

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }
        if (wsRef.current) {
            wsRef.current.close(1000, 'Normal closure');
            wsRef.current = null;
        }
        setIsConnected(false);
    }, []);

    const send = useCallback((data) => {
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify(data));
            return true;
        }
        console.warn('WebSocket is not connected');
        return false;
    }, []);

    useEffect(() => {
        connect();
        return () => disconnect();
    }, [connect, disconnect]);

    return { isConnected, error, send, disconnect, reconnect: connect };
};

export default useWebSocket;