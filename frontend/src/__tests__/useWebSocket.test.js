import { renderHook, act } from '@testing-library/react';
import useWebSocket from '../hooks/useWebSocket';

jest.mock('../keycloak', () => ({
    getToken: jest.fn().mockReturnValue('test-token'),
}));

// Mock WebSocket
class MockWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = 1;
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
    }
    send(data) { this.lastSentData = data; }
    close() { this.readyState = 3; if (this.onclose) this.onclose({ type: 'close' }); }
    simulateOpen() { if (this.onopen) this.onopen({ type: 'open' }); }
    _triggerMessage(data) { if (this.onmessage) this.onmessage({ data: JSON.stringify(data) }); }
    _triggerError(err) { if (this.onerror) this.onerror(err); }
}

let lastWsInstance;
global.WebSocket = function (url) {
    const ws = new MockWebSocket(url);
    lastWsInstance = ws;
    return ws;
};
global.WebSocket.OPEN = 1;
global.WebSocket.CLOSED = 3;

describe('useWebSocket', () => {
    beforeEach(() => {
        lastWsInstance = null;
        // Access mocked getToken and re-set its return value
        const keycloak = require('../keycloak');
        keycloak.getToken.mockReturnValue('test-token');
    });

    it('should connect to WebSocket', async () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        expect(lastWsInstance).toBeTruthy();
        expect(lastWsInstance.url).toContain('/ws/test-channel');

        await act(async () => {
            lastWsInstance.simulateOpen();
        });

        expect(result.current.isConnected).toBe(true);
        expect(result.current.error).toBeNull();
    });

    it('should send data via send function', async () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        await act(async () => { lastWsInstance.simulateOpen(); });

        act(() => { result.current.send({ type: 'test', content: 'hello' }); });
        expect(lastWsInstance.lastSentData).toBe(JSON.stringify({ type: 'test', content: 'hello' }));
    });

    it('should call onMessage when receiving a message', async () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        await act(async () => { lastWsInstance.simulateOpen(); });

        act(() => { lastWsInstance._triggerMessage({ type: 'chat', content: 'Hello' }); });
        expect(onMessage).toHaveBeenCalledWith({ type: 'chat', content: 'Hello' });
    });

    it('should set error on WebSocket error', async () => {
        const onMessage = jest.fn();
        const { result } = renderHook(() => useWebSocket('test-channel', onMessage));

        await act(async () => { lastWsInstance.simulateOpen(); });

        act(() => { lastWsInstance._triggerError(new Error('fail')); });
        expect(result.current.error).toBe('WebSocket connection error');
    });
});
