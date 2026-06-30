import { useEffect, useRef, useState, useCallback } from 'react';
import { getToken } from '../../../keycloak';

/**
 * useChatWs — WebSocket connection for a single chat room slug.
 *
 * Returns { isConnected, send } and calls onMessage(data) for every
 * incoming parseable frame.
 */
const MAX_RETRIES = 10;

const useChatWs = (slug, onMessage) => {
  const [isConnected, setIsConnected] = useState(false);
  const wsRef = useRef(null);
  const onMessageRef = useRef(onMessage);
  const retryCountRef = useRef(0);
  const retryTimeoutRef = useRef(null);
  const mountedRef = useRef(true);

  // Always call latest callback without re-connecting
  onMessageRef.current = onMessage;

  const send = useCallback((data) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data));
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    const API_URL = process.env.REACT_APP_API_URL || '/api';
    const wsBase = API_URL.replace(/^http/, 'ws').replace(/\/api$/, '');
    const url = `${wsBase}/ws/${slug}`;

    const connect = () => {
      if (!mountedRef.current) return;
      if (wsRef.current && wsRef.current.readyState !== WebSocket.CLOSED) {
        wsRef.current.close();
      }

      const token = getToken();
      if (!token) {
        return;
      }

      const ws = new WebSocket(`${url}`, token ? [token] : []);
      wsRef.current = ws;

      ws.onopen = () => {
        setIsConnected(true);
        retryCountRef.current = 0;
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessageRef.current?.(data);
        } catch (e) {
        }
      };

      ws.onerror = () => {};

      ws.onclose = () => {
        setIsConnected(false);
        if (!mountedRef.current) return;
        retryCountRef.current++;
        if (retryCountRef.current <= MAX_RETRIES) {
          const delay = Math.min(1000 * 2 ** (retryCountRef.current - 1), 30000);
          retryTimeoutRef.current = setTimeout(connect, delay);
        }
      };
    };

    connect();

    return () => {
      mountedRef.current = false;
      clearTimeout(retryTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [slug]);

  return { isConnected, send };
};

export default useChatWs;
