import { renderHook, act } from '@testing-library/react';
import useWebSocket from '../../hooks/useWebSocket';

// Мок для WebSocket
class MockWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = WebSocket.OPEN;
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
    }

    send(data) {
        this.lastSentData = data;
    }

    close() {
        this.readyState = WebSocket.CLOSED;
        if (this.onclose) this.onclose({});
    }

    // Вспомогательные методы для тестов
    _triggerOpen() {
        if (this.onopen) this.onopen({});
    }

    _triggerMessage(data) {
        if (this.onmessage) this.onmessage({ data: JSON.stringify(data) });
    }

    _triggerError(err) {
        if (this.onerror) this.onerror(err);
    }
}

global.WebSocket = jest.fn(() => new MockWebSocket());

describe('useWebSocket', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should connect to WebSocket', () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        expect(WebSocket).toHaveBeenCalledWith('ws://localhost:8081/ws/test-channel');
        expect(result.current.isConnected).toBe(true);
        expect(result.current.error).toBeNull();
    });

    it('should send data via send function', () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        act(() => {
            result.current.send({ type: 'test', content: 'hello' });
        });

        // Получаем последний созданный WebSocket
        const ws = WebSocket.mock.results[0].value;
        expect(ws.lastSentData).toBe(JSON.stringify({ type: 'test', content: 'hello' }));
    });

    it('should call onMessage when receiving a message', () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        const ws = WebSocket.mock.results[0].value;
        act(() => {
            ws._triggerMessage({ type: 'chat', content: 'Hello' });
        });

        expect(onMessage).toHaveBeenCalledWith({ type: 'chat', content: 'Hello' });
    });

    it('should set error on WebSocket error', () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        const ws = WebSocket.mock.results[0].value;
        act(() => {
            ws._triggerError(new Error('Connection failed'));
        });

        expect(result.current.error).toBe('WebSocket connection error');
    });
});
