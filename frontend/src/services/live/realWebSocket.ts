/**
 * Real WebSocket service for broadcast stats.
 * Connects to backend /ws/broadcast/{broadcastId}/stats.
 * Replaces mockWebSocket.ts for production use.
 *
 * Backend events:
 *   { type: "stats", broadcastId, viewerCount, timestamp }
 *
 * Frontend events dispatched:
 *   VIEWER_COUNT_UPDATED — real viewer count from backend
 *   CHAT_MESSAGE — local-only (no backend broadcast chat endpoint yet)
 *   REACTION_ADDED — local-only
 *   BROADCAST_STATUS_CHANGED — polled from REST API
 */

import { EventCallback, UnsubscribeFn, WSEventType } from './types';

type SubscriberMap = Map<string, Set<EventCallback>>;

const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000]; // exponential backoff
const MAX_RECONNECT = RECONNECT_DELAYS.length;

export class RealWebSocketService {
  private static instance: RealWebSocketService;
  private subscribers: SubscriberMap = new Map();
  private connections: Map<string, WebSocket> = new Map();
  private reconnectAttempts: Map<string, number> = new Map();
  private reconnectTimers: Map<string, ReturnType<typeof setTimeout>> = new Map();
  private closed = false; // true after disconnect() — prevents reconnect

  private constructor() {}

  static getInstance(): RealWebSocketService {
    if (!RealWebSocketService.instance) {
      RealWebSocketService.instance = new RealWebSocketService();
    }
    return RealWebSocketService.instance;
  }

  /**
   * Subscribe to a broadcast event type.
   * Returns unsubscribe function.
   */
  subscribe(eventType: string, callback: EventCallback): UnsubscribeFn {
    if (!this.subscribers.has(eventType)) {
      this.subscribers.set(eventType, new Set());
    }
    this.subscribers.get(eventType)!.add(callback);

    return () => {
      this.subscribers.get(eventType)?.delete(callback);
    };
  }

  /**
   * Emit event to all subscribers of a given type.
   */
  private emit(eventType: string, data: any): void {
    const subs = this.subscribers.get(eventType);
    if (subs) {
      subs.forEach(cb => {
        try {
          cb(data);
        } catch (err) {
          console.error(`[RealWS] Subscriber error for ${eventType}:`, err);
        }
      });
    }
  }

  /**
   * Connect to broadcast stats WebSocket.
   * URL: ws(s)://{host}/ws/broadcast/{broadcastId}/stats
   */
  async connect(broadcastId: string): Promise<boolean> {
    if (this.connections.has(broadcastId)) {
      return true; // Already connected
    }

    this.closed = false;
    this.reconnectAttempts.set(broadcastId, 0);

    return this.establishConnection(broadcastId);
  }

  private async establishConnection(broadcastId: string): Promise<boolean> {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      const url = `${protocol}//${host}/ws/broadcast/${broadcastId}/stats`;

      // Add JWT token as query param (backend interceptor reads it)
      const token = localStorage.getItem('keycloak-token');
      const wsUrl = token ? `${url}?token=${encodeURIComponent(token)}` : url;

      const ws = new WebSocket(wsUrl);

      // ALL handlers assigned ONCE inside Promise — no duplicate overwrite
      return new Promise<boolean>((resolve) => {
        const timeout = setTimeout(() => {
          ws.close();
          resolve(false);
        }, 5000);

        ws.onopen = () => {
          clearTimeout(timeout);
          console.log(`[RealWS] Connected to broadcast ${broadcastId}`);
          this.connections.set(broadcastId, ws);
          this.reconnectAttempts.set(broadcastId, 0);
          resolve(true);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            this.handleMessage(broadcastId, data);
          } catch (err) {
            console.warn('[RealWS] Failed to parse message:', event.data);
          }
        };

        ws.onclose = (event) => {
          console.log(`[RealWS] Disconnected from ${broadcastId} (code: ${event.code})`);
          this.connections.delete(broadcastId);
          if (!this.closed && event.code !== 1000) {
            this.scheduleReconnect(broadcastId);
          }
        };

        ws.onerror = () => {
          clearTimeout(timeout);
          resolve(false);
        };
      });
    } catch (err) {
      console.error(`[RealWS] Connection failed for ${broadcastId}:`, err);
      return false;
    }
  }

  /**
   * Handle incoming WebSocket message from backend.
   */
  private handleMessage(broadcastId: string, data: any): void {
    switch (data.type) {
      case 'stats':
        // Backend: { type: "stats", broadcastId, viewerCount, timestamp }
        this.emit('VIEWER_COUNT_UPDATED', {
          broadcastId: data.broadcastId || broadcastId,
          viewerCount: data.viewerCount || 0,
          viewerChange: 0, // absolute count, not delta
          timestamp: data.timestamp
        });
        break;

      default:
        console.log('[RealWS] Unknown message type:', data.type);
    }
  }

  /**
   * Schedule reconnection with exponential backoff.
   */
  private scheduleReconnect(broadcastId: string): void {
    const attempts = this.reconnectAttempts.get(broadcastId) || 0;
    if (attempts >= MAX_RECONNECT) {
      console.warn(`[RealWS] Max reconnect attempts reached for ${broadcastId}`);
      this.emit('SYSTEM_MESSAGE', {
        type: 'reconnect_failed',
        broadcastId
      });
      return;
    }

    const delay = RECONNECT_DELAYS[attempts];
    this.reconnectAttempts.set(broadcastId, attempts + 1);

    console.log(`[RealWS] Reconnecting to ${broadcastId} in ${delay}ms (attempt ${attempts + 1})`);

    const timer = setTimeout(() => {
      this.reconnectTimers.delete(broadcastId);
      this.establishConnection(broadcastId);
    }, delay);

    this.reconnectTimers.set(broadcastId, timer);
  }

  /**
   * Disconnect from a specific broadcast.
   */
  disconnect(broadcastId: string): void {
    // Cancel pending reconnect
    const timer = this.reconnectTimers.get(broadcastId);
    if (timer) {
      clearTimeout(timer);
      this.reconnectTimers.delete(broadcastId);
    }

    // Close WebSocket
    const ws = this.connections.get(broadcastId);
    if (ws) {
      ws.close(1000, 'Client disconnect');
      this.connections.delete(broadcastId);
    }

    this.reconnectAttempts.delete(broadcastId);
  }

  /**
   * Send data through WebSocket (for chat messages, reactions).
   * Currently not supported by backend broadcast stats endpoint.
   */
  send(eventType: string, data: any, broadcastId: string): void {
    const ws = this.connections.get(broadcastId);
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: eventType, ...data }));
    }
  }

  /**
   * Disconnect all connections and prevent reconnects.
   */
  disconnectAll(): void {
    this.closed = true;

    Array.from(this.connections.keys()).forEach((broadcastId) => {
      this.disconnect(broadcastId);
    });

    Array.from(this.reconnectTimers.values()).forEach((timer) => {
      clearTimeout(timer);
    });
    this.reconnectTimers.clear();
  }

  /**
   * Check if connected to a broadcast.
   */
  isConnected(broadcastId: string): boolean {
    const ws = this.connections.get(broadcastId);
    return ws !== undefined && ws.readyState === WebSocket.OPEN;
  }
}

export const realWebSocket = RealWebSocketService.getInstance();
export type { RealWebSocketService as LiveWebSocketService };
