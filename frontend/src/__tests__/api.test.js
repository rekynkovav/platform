import { broadcastApi, chatApi, paymentApi, userApi, adminApi, dashboardApi, feedApi, eventsApi, feedbackApi, notificationApi } from '../services/api';

const apiModule = require('../services/api');
const client = apiModule.default.client;

jest.mock('../keycloak', () => ({
    getToken: jest.fn(),
    updateToken: jest.fn(),
    isTokenExpired: jest.fn(),
    logout: jest.fn(),
}));

describe('API endpoints', () => {
    let spyGet, spyPost;

    beforeEach(() => {
        jest.restoreAllMocks();
        spyGet = jest.spyOn(client, 'get').mockResolvedValue({ data: 'response' });
        spyPost = jest.spyOn(client, 'post').mockResolvedValue({ data: 'response' });
    });

    afterEach(() => jest.restoreAllMocks());

    it('broadcastApi.getActive calls correct URL', async () => {
        await broadcastApi.getActive();
        expect(spyGet).toHaveBeenCalledWith('/broadcast/active', { params: undefined });
    });

    it('chatApi.getMessages calls correct URL with params', async () => {
        await chatApi.getMessages('general', 0, 50);
        expect(spyGet).toHaveBeenCalledWith('/chat/general/messages', { params: { page: 0, size: 50 } });
    });

    it('paymentApi.create calls correct URL', async () => {
        await paymentApi.create({ subscriptionMonths: 1 });
        expect(spyPost).toHaveBeenCalledWith('/payments', { subscriptionMonths: 1 });
    });

    it('userApi.getMe calls correct URL', async () => {
        await userApi.getMe();
        expect(spyGet).toHaveBeenCalledWith('/users/me', { params: undefined });
    });

    it('adminApi.getStats calls correct URL', async () => {
        await adminApi.getStats();
        expect(spyGet).toHaveBeenCalledWith('/admin/stats', { params: undefined });
    });

    it('dashboardApi.getMenuData calls correct URL', async () => {
        await dashboardApi.getMenuData();
        expect(spyGet).toHaveBeenCalledWith('/dashboard/menu', { params: undefined });
    });

    it('feedApi.getMainFeed calls correct URL', async () => {
        await feedApi.getMainFeed();
        expect(spyGet).toHaveBeenCalledWith('/feed/main', { params: { page: 0, size: 20 } });
    });

    it('eventsApi.getUpcoming calls correct URL', async () => {
        await eventsApi.getUpcoming();
        expect(spyGet).toHaveBeenCalledWith('/events/upcoming', { params: undefined });
    });

    it('feedbackApi.getAll calls correct URL', async () => {
        await feedbackApi.getAll();
        expect(spyGet).toHaveBeenCalledWith('/events/materials/feedback', { params: undefined });
    });

    it('notificationApi.getUserNotifications calls correct URL', async () => {
        await notificationApi.getUserNotifications();
        expect(spyGet).toHaveBeenCalledWith('/notifications', { params: undefined });
    });
});
