import { renderHook, act, waitFor } from '@testing-library/react';
import useChatRoom from '../features/chat/hooks/useChatRoom';
import useChatWs from '../features/chat/hooks/useChatWs';
import { chatApi } from '../services/api';

jest.mock('../services/api', () => ({
    chatApi: {
        getMessages: jest.fn(),
        sendMessage: jest.fn(),
        editMessage: jest.fn(),
        deleteMessage: jest.fn(),
    },
}));

jest.mock('../features/chat/hooks/useChatWs');

describe('useChatRoom', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        chatApi.getMessages.mockResolvedValue({ data: [] });
        chatApi.sendMessage.mockResolvedValue({ id: 'msg-1', content: 'hi' });
        chatApi.editMessage.mockResolvedValue({});
        chatApi.deleteMessage.mockResolvedValue({});
        useChatWs.mockReturnValue({ isConnected: true, send: jest.fn() });
    });

    it('returns initial state', () => {
        const { result } = renderHook(() => useChatRoom('test-room'));

        expect(result.current.messages).toEqual([]);
        expect(result.current.loading).toBe(true);
        expect(result.current.error).toBeNull();
        expect(result.current.currentSlug).toBe('test-room');
        expect(result.current.isConnected).toBe(true);
        // username is set at module scope from keycloak — cannot be reliably mocked in CRA
        expect(typeof result.current.username).toBe('string');
    });

    it('loads messages on mount', async () => {
        chatApi.getMessages.mockResolvedValue({
            data: [
                { id: '1', content: 'hello', author: 'user1' },
                { id: '2', content: 'world', author: 'user2' },
            ],
        });

        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.messages).toHaveLength(2);
        expect(chatApi.getMessages).toHaveBeenCalledWith('test-room', 0, 50);
    });

    it('sets error when loadMessages fails', async () => {
        chatApi.getMessages.mockRejectedValue(new Error('Network error'));
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.error).toBe('Не удалось загрузить сообщения');
    });

    it('sendMessage sends via API', async () => {
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.sendMessage('Hello!');
        });

        expect(chatApi.sendMessage).toHaveBeenCalledWith('test-room', {
            content: 'Hello!',
            mediaId: null,
        });
    });

    it('sendMessage with empty content does nothing', async () => {
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.sendMessage('   ');
        });

        expect(chatApi.sendMessage).not.toHaveBeenCalled();
    });

    it('sendMessage sets error when not connected', async () => {
        useChatWs.mockReturnValue({ isConnected: false, send: jest.fn() });
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.sendMessage('Hello!');
        });

        expect(result.current.error).toBe('Отключён от сервера. Обновите страницу.');
        expect(chatApi.sendMessage).not.toHaveBeenCalled();
    });

    it('sendMessage removes temp message on API error', async () => {
        chatApi.sendMessage.mockRejectedValue(new Error('Send failed'));
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.sendMessage('fail');
        });

        expect(result.current.error).toBe('Не удалось отправить сообщение');
        expect(result.current.messages.find((m) => m.id?.startsWith('temp_'))).toBeUndefined();
    });

    it('editMessage updates message content', async () => {
        chatApi.getMessages.mockResolvedValue({
            data: [{ id: 'msg-1', content: 'old', author: 'user1' }],
        });
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => {
            expect(result.current.messages).toHaveLength(1);
        });

        await act(async () => {
            await result.current.editMessage('msg-1', 'new content');
        });

        expect(chatApi.editMessage).toHaveBeenCalledWith('test-room', 'msg-1', {
            content: 'new content',
        });
        expect(result.current.messages[0].content).toBe('new content');
        expect(result.current.messages[0].edited).toBe(true);
    });

    it('editMessage does nothing with empty content', async () => {
        const { result } = renderHook(() => useChatRoom('test-room'));
        await waitFor(() => { expect(result.current.loading).toBe(false); });

        await act(async () => {
            await result.current.editMessage('msg-1', '  ');
        });
        expect(chatApi.editMessage).not.toHaveBeenCalled();
    });

    it('editMessage sets error on failure', async () => {
        chatApi.editMessage.mockRejectedValue(new Error('Edit failed'));
        const { result } = renderHook(() => useChatRoom('test-room'));
        await waitFor(() => { expect(result.current.loading).toBe(false); });

        await act(async () => {
            await result.current.editMessage('msg-1', 'new');
        });
        expect(result.current.error).toBe('Не удалось отредактировать сообщение');
    });

    it('deleteMessage calls confirm and removes message', async () => {
        chatApi.getMessages.mockResolvedValue({
            data: [{ id: 'msg-1', content: 'hi', author: 'user1' }],
        });
        window.confirm = jest.fn().mockReturnValue(true);
        const { result } = renderHook(() => useChatRoom('test-room'));

        await waitFor(() => { expect(result.current.messages).toHaveLength(1); });

        let deleteResult;
        await act(async () => {
            deleteResult = await result.current.deleteMessage('msg-1');
        });
        expect(window.confirm).toHaveBeenCalledWith('Удалить это сообщение?');
        expect(chatApi.deleteMessage).toHaveBeenCalledWith('test-room', 'msg-1');
        expect(result.current.messages).toHaveLength(0);
        expect(deleteResult).toBe(true);
    });

    it('deleteMessage returns false when user cancels', async () => {
        window.confirm = jest.fn().mockReturnValue(false);
        const { result } = renderHook(() => useChatRoom('test-room'));
        await waitFor(() => { expect(result.current.loading).toBe(false); });

        let deleteResult;
        await act(async () => {
            deleteResult = await result.current.deleteMessage('msg-1');
        });
        expect(deleteResult).toBe(false);
        expect(chatApi.deleteMessage).not.toHaveBeenCalled();
    });

    it('deleteMessage sets error on failure', async () => {
        chatApi.deleteMessage.mockRejectedValue(new Error('Delete failed'));
        window.confirm = jest.fn().mockReturnValue(true);
        const { result } = renderHook(() => useChatRoom('test-room'));
        await waitFor(() => { expect(result.current.loading).toBe(false); });

        let deleteResult;
        await act(async () => {
            deleteResult = await result.current.deleteMessage('msg-1');
        });
        expect(result.current.error).toBe('Не удалось удалить сообщение');
        expect(deleteResult).toBe(false);
    });

    it('clearError removes error state', async () => {
        chatApi.getMessages.mockRejectedValue(new Error('fail'));
        const { result } = renderHook(() => useChatRoom('test-room'));
        await waitFor(() => { expect(result.current.error).toBe('Не удалось загрузить сообщения'); });

        act(() => { result.current.clearError(); });
        expect(result.current.error).toBeNull();
    });

    it('exposes isConnected from useChatWs', () => {
        const { result } = renderHook(() => useChatRoom('test-room'));
        expect(result.current.isConnected).toBe(true);
    });
});
