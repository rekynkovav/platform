import { useEffect, useRef, useState, useCallback } from 'react';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';
const MAX_RETRIES = 10;

const useWebSocket = (channel, onMessage) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState(null);
    const wsRef = useRef(null);
    const onMessageRef = useRef(onMessage);
    const retryCountRef = useRef(0);
    const retryTimeoutRef = useRef(null);
    const mountedRef = useRef(true);
    onMessageRef.current = onMessage;

    const send = useCallback((data) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify(data));
        }
    }, []);

    useEffect(() => {
        mountedRef.current = true;
        const wsBase = API_URL.replace(/^http/, 'ws').replace(/\/api$/, '');
        const url = `${wsBase}/ws/${channel}`;

        const connect = () => {
            if (!mountedRef.current) return;
            if (wsRef.current && wsRef.current.readyState !== WebSocket.CLOSED) {
                wsRef.current.close();
            }

            const ws = new WebSocket(url);
            wsRef.current = ws;

            ws.onopen = () => {
                setIsConnected(true);
                setError(null);
                retryCountRef.current = 0;
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
                if (!mountedRef.current) return;
                retryCountRef.current++;
                if (retryCountRef.current <= MAX_RETRIES) {
                    const delay = Math.min(1000 * Math.pow(2, retryCountRef.current - 1), 30000);
                    retryTimeoutRef.current = setTimeout(() => {
                        connect();
                    }, delay);
                } else {
                    setError('WebSocket connection failed after max retries');
                }
            };
        };

        connect();

        return () => {
            mountedRef.current = false;
            if (retryTimeoutRef.current) {
                clearTimeout(retryTimeoutRef.current);
                retryTimeoutRef.current = null;
            }
            if (wsRef.current) {
                wsRef.current.onclose = null;
                wsRef.current.close();
                wsRef.current = null;
            }
        };
    }, [channel]);

    return { isConnected, send, error };
};

export default useWebSocket;
