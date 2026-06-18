import { renderHook, act, waitFor } from '@testing-library/react';
import { useChat } from '../components/Chat/useChat';
import { chatApi, mediaApi } from '../services/api';
import chatLogger from '../utils/chatLogger';

// Моки
jest.mock('../services/api', () => ({
    chatApi: {
        getMessages: jest.fn(),
        sendMessage: jest.fn(),
        editMessage: jest.fn(),
        deleteMessage: jest.fn(),
        getUsers: jest.fn(),
    },
    mediaApi: {
        upload: jest.fn(),
        getStreamUrl: jest.fn(),
        delete: jest.fn(),
    },
}));
jest.mock('../utils/chatLogger', () => ({
    logTextMessage: jest.fn(),
    logFileUpload: jest.fn(),
    logVoiceMessage: jest.fn(),
    logMessageEdit: jest.fn(),
    logMessageDelete: jest.fn(),
}));

describe('useChat', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Mock sessionStorage
        sessionStorage.clear();
        // Prevent console.error noise
        jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        console.error.mockRestore();
    });

    describe('initialization', () => {
        it('initializes with default state', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            expect(result.current.loading).toBe(true);
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
        });

        it('sets canSwitch based on config', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true, canSwitchRooms: false })
            );
            await waitFor(() => {
                expect(result.current.canSwitch).toBe(false);
            });
        });

        it('defaults currentRoom to chatRoom param', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            const { result } = renderHook(() =>
                useChat('nutrition', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.currentRoom).toBe('nutrition');
            });
        });
    });

    describe('API mode', () => {
        it('loads messages from API', async () => {
            const mockMessages = [
                { id: '1', author: 'Система', content: 'Welcome' },
                { id: '2', author: 'User', content: 'Hello' },
            ];
            chatApi.getMessages.mockResolvedValue({ data: mockMessages });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.messages).toEqual(mockMessages);
            });
        });

        it('sends a message via API', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            chatApi.sendMessage.mockResolvedValue({ data: { id: 'new-1', content: 'Hi' } });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
            await act(async () => {
                await result.current.sendMessage('Hi');
            });
            expect(chatApi.sendMessage).toHaveBeenCalledWith('Hi', null);
        });

        it('does not send empty message', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
            await act(async () => {
                await result.current.sendMessage('');
            });
            expect(chatApi.sendMessage).not.toHaveBeenCalled();
        });

        it('sets error on API load failure', async () => {
            chatApi.getMessages.mockRejectedValue(new Error('Network error'));
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.error).toBeTruthy();
            });
        });

        it('sets error on send failure', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            chatApi.sendMessage.mockRejectedValue(new Error('Send failed'));
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
            await act(async () => {
                try { await result.current.sendMessage('Hi'); } catch (e) { /* expected */ }
            });
            expect(result.current.error).toBeTruthy();
        });
    });

    describe('storage mode (non-API)', () => {
        it('loads messages from sessionStorage in development', async () => {
            const saved = [{ id: '100', author: 'Saved', content: 'From storage' }];
            sessionStorage.setItem('chatMessages_general', JSON.stringify(saved));
            process.env.NODE_ENV = 'development';
            const { result } = renderHook(() =>
                useChat('general', { useApi: false, storageKeyPrefix: 'chatMessages' })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
        });

        it('creates message locally in storage mode', async () => {
            process.env.NODE_ENV = 'development';
            const { result } = renderHook(() =>
                useChat('general', {
                    useApi: false,
                    storageKeyPrefix: 'chatMessages',
                    canSwitchRooms: true,
                })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
            await act(async () => {
                await result.current.sendMessage('Local message');
            });
            expect(result.current.messages.length).toBeGreaterThanOrEqual(1);
        });
    });

    describe('utility functions', () => {
        it('clearError resets error state', async () => {
            chatApi.getMessages.mockRejectedValue(new Error('fail'));
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.error).toBeTruthy();
            });
            act(() => {
                result.current.clearError();
            });
            expect(result.current.error).toBeNull();
        });

        it('formats recording time correctly', async () => {
            chatApi.getMessages.mockResolvedValue({ data: [] });
            const { result } = renderHook(() =>
                useChat('general', { useApi: true })
            );
            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });
            expect(result.current.formatRecordingTime(0)).toBe('0:00');
            expect(result.current.formatRecordingTime(65)).toBe('1:05');
            expect(result.current.formatRecordingTime(125)).toBe('2:05');
        });
    });
});
