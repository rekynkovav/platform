import { renderHook, act } from '@testing-library/react';
import useChatWs from '../features/chat/hooks/useChatWs';
import { getToken } from '../keycloak';

jest.mock('../keycloak', () => ({
    getToken: jest.fn(),
}));

// Mock WebSocket with static constants
class MockWebSocket {
    static instances = [];
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;

    constructor(url, protocols) {
        this.url = url;
        this.protocols = protocols || [];
        this.readyState = 0; // CONNECTING
        this.onopen = null;
        this.onmessage = null;
        this.onclose = null;
        this.onerror = null;
        this.sentMessages = [];
        MockWebSocket.instances.push(this);
    }

    send(data) {
        this.sentMessages.push(data);
    }

    close() {
        this.readyState = MockWebSocket.CLOSED;
        if (this.onclose) this.onclose({ code: 1000 });
    }

    simulateOpen() {
        this.readyState = MockWebSocket.OPEN;
        if (this.onopen) this.onopen();
    }

    simulateMessage(data) {
        if (this.onmessage) {
            this.onmessage({ data: JSON.stringify(data) });
        }
    }

    simulateClose() {
        this.readyState = MockWebSocket.CLOSED;
        if (this.onclose) this.onclose({ code: 1000 });
    }
}

global.WebSocket = MockWebSocket;
jest.useFakeTimers();

describe('useChatWs', () => {
    beforeEach(() => {
        MockWebSocket.instances = [];
        jest.clearAllMocks();
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('returns isConnected=false and send function initially', () => {
        getToken.mockReturnValue('test-token');
        const { result } = renderHook(() => useChatWs('test-room', jest.fn()));
        expect(result.current.isConnected).toBe(false);
        expect(typeof result.current.send).toBe('function');
    });

    it('creates a WebSocket on mount with correct URL and token in protocols', () => {
        getToken.mockReturnValue('my-token');
        renderHook(() => useChatWs('my-room', jest.fn()));

        expect(MockWebSocket.instances.length).toBe(1);
        expect(MockWebSocket.instances[0].url).toContain('/ws/my-room');
        expect(MockWebSocket.instances[0].url).not.toContain('token=');
        expect(MockWebSocket.instances[0].protocols).toContain('my-token');
    });

    it('sets isConnected to true when WebSocket opens', () => {
        getToken.mockReturnValue('token');
        const { result } = renderHook(() => useChatWs('room', jest.fn()));

        act(() => {
            MockWebSocket.instances[0].simulateOpen();
        });

        expect(result.current.isConnected).toBe(true);
    });

    it('calls onMessage when WebSocket receives a message', () => {
        getToken.mockReturnValue('token');
        const onMessage = jest.fn();
        renderHook(() => useChatWs('room', onMessage));

        act(() => {
            MockWebSocket.instances[0].simulateOpen();
        });

        const testMsg = { id: '1', content: 'hello' };
        act(() => {
            MockWebSocket.instances[0].simulateMessage(testMsg);
        });

        expect(onMessage).toHaveBeenCalledWith(testMsg);
    });

    it('does not call onMessage for invalid JSON', () => {
        getToken.mockReturnValue('token');
        const onMessage = jest.fn();
        renderHook(() => useChatWs('room', onMessage));

        act(() => {
            MockWebSocket.instances[0].simulateOpen();
        });

        act(() => {
            if (MockWebSocket.instances[0].onmessage) {
                MockWebSocket.instances[0].onmessage({ data: 'not-json' });
            }
        });

        expect(onMessage).not.toHaveBeenCalled();
    });

    it('send sends JSON stringified data when connected', () => {
        getToken.mockReturnValue('token');
        const { result } = renderHook(() => useChatWs('room', jest.fn()));

        act(() => {
            MockWebSocket.instances[0].simulateOpen();
        });

        act(() => {
            result.current.send({ text: 'hi' });
        });

        expect(MockWebSocket.instances[0].sentMessages).toEqual([JSON.stringify({ text: 'hi' })]);
    });

    it('send does nothing when WebSocket is not open', () => {
        getToken.mockReturnValue('token');
        const { result } = renderHook(() => useChatWs('room', jest.fn()));

        // WebSocket created but not opened (readyState = 0, CONNECTING)
        act(() => {
            result.current.send({ text: 'hi' });
        });

        expect(MockWebSocket.instances[0].sentMessages).toEqual([]);
    });

    it('sets isConnected to false when WebSocket closes', () => {
        getToken.mockReturnValue('token');
        const { result } = renderHook(() => useChatWs('room', jest.fn()));

        act(() => {
            MockWebSocket.instances[0].simulateOpen();
        });

        expect(result.current.isConnected).toBe(true);

        act(() => {
            MockWebSocket.instances[0].simulateClose();
        });

        expect(result.current.isConnected).toBe(false);
    });

    it('retries connection on close with exponential backoff', () => {
        getToken.mockReturnValue('token');
        renderHook(() => useChatWs('room', jest.fn()));

        expect(MockWebSocket.instances.length).toBe(1);

        // Close first connection
        act(() => {
            MockWebSocket.instances[0].simulateClose();
        });

        // After retry delay (1000ms for first retry)
        act(() => {
            jest.advanceTimersByTime(1100);
        });

        expect(MockWebSocket.instances.length).toBe(2);
    });

    it('does not retry if no token available', () => {
        getToken.mockReturnValue(null);
        renderHook(() => useChatWs('room', jest.fn()));

        // No WebSocket should be created if no token
        expect(MockWebSocket.instances.length).toBe(0);
    });

    it('cleans up on unmount', () => {
        getToken.mockReturnValue('token');
        const { unmount } = renderHook(() => useChatWs('room', jest.fn()));

        expect(MockWebSocket.instances.length).toBe(1);
        const ws = MockWebSocket.instances[0];

        unmount();

        // Should have closed the WebSocket (readyState set to CLOSED)
        expect(ws.readyState).toBe(MockWebSocket.CLOSED);
    });
});
