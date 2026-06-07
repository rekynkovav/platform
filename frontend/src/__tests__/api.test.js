import axios from 'axios';
import { api, broadcastApi, chatApi, paymentApi, userApi, mediaApi, adminApi, dashboardApi, feedApi, eventsApi, feedbackApi, notificationApi } from '../services/api';
import { getToken, updateToken, isTokenExpired, logout } from '../keycloak';

// Моки
jest.mock('axios');
jest.mock('../keycloak', () => ({
    getToken: jest.fn(),
    updateToken: jest.fn(),
    isTokenExpired: jest.fn(),
    logout: jest.fn(),
}));

describe('ApiClient', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Настройка axios.create для возврата мока
        axios.create.mockReturnValue({
            interceptors: {
                request: { use: jest.fn() },
                response: { use: jest.fn() },
            },
            get: jest.fn(),
            post: jest.fn(),
            put: jest.fn(),
            delete: jest.fn(),
        });
    });

    it('should create axios instance with correct baseURL', () => {
        expect(axios.create).toHaveBeenCalledWith({
            baseURL: 'http://localhost:8081/api',
            timeout: 30000,
            headers: { 'Content-Type': 'application/json' },
        });
    });

    it('should add Authorization header when token exists', async () => {
        getToken.mockReturnValue('test-token');
        isTokenExpired.mockReturnValue(false);

        // Получаем экземпляр axios из api.client
        const client = api.client;
        // Вызываем request interceptor
        const config = { headers: {} };
        const interceptor = client.interceptors.request.use.mock.calls[0][0];
        const result = await interceptor(config);

        expect(result.headers.Authorization).toBe('Bearer test-token');
    });

    it('should update token if expired', async () => {
        getToken.mockReturnValue('old-token');
        isTokenExpired.mockReturnValue(true);
        updateToken.mockResolvedValue(true);
        getToken.mockReturnValue('new-token');

        const client = api.client;
        const config = { headers: {} };
        const interceptor = client.interceptors.request.use.mock.calls[0][0];
        const result = await interceptor(config);

        expect(updateToken).toHaveBeenCalled();
        expect(result.headers.Authorization).toBe('Bearer new-token');
    });

    it('should call logout on 401 response', async () => {
        const error = { response: { status: 401 } };
        const client = api.client;
        const errorInterceptor = client.interceptors.response.use.mock.calls[0][1];
        await expect(errorInterceptor(error)).rejects.toEqual(error);
        expect(logout).toHaveBeenCalled();
    });

    it('should not call logout on other errors', async () => {
        const error = { response: { status: 500 } };
        const client = api.client;
        const errorInterceptor = client.interceptors.response.use.mock.calls[0][1];
        await expect(errorInterceptor(error)).rejects.toEqual(error);
        expect(logout).not.toHaveBeenCalled();
    });
});

describe('API endpoints', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        axios.create.mockReturnValue({
            interceptors: {
                request: { use: jest.fn() },
                response: { use: jest.fn() },
            },
            get: jest.fn().mockResolvedValue({ data: 'response' }),
            post: jest.fn().mockResolvedValue({ data: 'response' }),
            put: jest.fn().mockResolvedValue({ data: 'response' }),
            delete: jest.fn().mockResolvedValue({ data: 'response' }),
        });
    });

    it('broadcastApi.getActive calls correct URL', async () => {
        await broadcastApi.getActive();
        expect(api.client.get).toHaveBeenCalledWith('/broadcast/active');
    });

    it('chatApi.getMessages calls correct URL with params', async () => {
        await chatApi.getMessages('general', 0, 50);
        expect(api.client.get).toHaveBeenCalledWith('/chat/general/messages', { page: 0, size: 50 });
    });

    it('paymentApi.create calls correct URL', async () => {
        await paymentApi.create({ subscriptionMonths: 1 });
        expect(api.client.post).toHaveBeenCalledWith('/payments', { subscriptionMonths: 1 });
    });

    it('userApi.getMe calls correct URL', async () => {
        await userApi.getMe();
        expect(api.client.get).toHaveBeenCalledWith('/users/me');
    });

    it('mediaApi.upload calls correct URL with FormData', async () => {
        const file = new File(['test'], 'test.mp3', { type: 'audio/mp3' });
        await mediaApi.upload(file);
        expect(api.client.post).toHaveBeenCalledWith('/media/upload', expect.any(FormData), {
            headers: { 'Content-Type': 'multipart/form-data' },
            onUploadProgress: undefined,
        });
    });

    it('adminApi.getStats calls correct URL', async () => {
        await adminApi.getStats();
        expect(api.client.get).toHaveBeenCalledWith('/admin/stats');
    });

    it('dashboardApi.getMenuData calls correct URL', async () => {
        await dashboardApi.getMenuData();
        expect(api.client.get).toHaveBeenCalledWith('/dashboard/menu');
    });

    it('feedApi.getMainFeed calls correct URL', async () => {
        await feedApi.getMainFeed();
        expect(api.client.get).toHaveBeenCalledWith('/feed/main');
    });

    it('eventsApi.getUpcoming calls correct URL', async () => {
        await eventsApi.getUpcoming();
        expect(api.client.get).toHaveBeenCalledWith('/events/upcoming');
    });

    it('feedbackApi.getAll calls correct URL', async () => {
        await feedbackApi.getAll();
        expect(api.client.get).toHaveBeenCalledWith('/events/materials/feedback');
    });

    it('notificationApi.getUserNotifications calls correct URL', async () => {
        await notificationApi.getUserNotifications();
        expect(api.client.get).toHaveBeenCalledWith('/notifications');
    });
});
