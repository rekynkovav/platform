import { api, broadcastApi } from './api';

global.fetch = jest.fn();

describe('ApiClient', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('GET request should fetch with correct headers', async () => {
        const mockResponse = { data: 'test' };
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockResponse
        });

        const result = await api.get('/test');

        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8081/test',
            expect.objectContaining({
                method: 'GET',
                headers: expect.objectContaining({
                    'Content-Type': 'application/json'
                })
            })
        );
        expect(result).toEqual(mockResponse);
    });

    test('POST request should send body', async () => {
        const mockResponse = { id: '123' };
        const postData = { title: 'Test' };

        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockResponse
        });

        const result = await api.post('/test', postData);

        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8081/test',
            expect.objectContaining({
                method: 'POST',
                body: JSON.stringify(postData)
            })
        );
        expect(result).toEqual(mockResponse);
    });

    test('should handle 404 error', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 404,
            text: async () => 'Not Found'
        });

        await expect(api.get('/not-found')).rejects.toThrow('Not Found');
    });

    test('broadcastApi.create should call correct endpoint', async () => {
        const mockBroadcast = { id: '123', title: 'Test' };
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockBroadcast
        });

        const result = await broadcastApi.create('Test', 'Desc', 'video');

        expect(result).toEqual(mockBroadcast);
        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8081/api/broadcast/create',
            expect.objectContaining({
                method: 'POST',
                body: JSON.stringify({
                    title: 'Test',
                    description: 'Desc',
                    streamType: 'video'
                })
            })
        );
    });

    test('broadcastApi.getActive should call correct endpoint', async () => {
        const mockBroadcasts = [{ id: '123' }];
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockBroadcasts
        });

        const result = await broadcastApi.getActive();

        expect(result).toEqual(mockBroadcasts);
        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8081/api/broadcast/active',
            expect.objectContaining({ method: 'GET' })
        );
    });
});