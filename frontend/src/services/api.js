import axios from 'axios';
import { getToken, updateToken, isTokenExpired } from '../keycloak';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';
const USE_MOCK = process.env.REACT_APP_USE_MOCK_AUTH === 'true';

class ApiClient {
    constructor() {
        this.client = axios.create({
            baseURL: API_URL,
            timeout: 30000,
            headers: {
                'Content-Type': 'application/json'
            }
        });

        // Request interceptor — добавляем JWT
        this.client.interceptors.request.use(async (config) => {
            let token = getToken();

            // Обновляем только если истекает (избегаем лишних вызовов updateToken)
            if (token && isTokenExpired() && await updateToken()) {
                token = getToken();
            }

            if (token && !USE_MOCK) {
                config.headers.Authorization = `Bearer ${token}`;
            }

            return config;
        });

        // Response interceptor — обработка ошибок
        this.client.interceptors.response.use(
            (response) => response.data,
            (error) => {
                if (error.response?.status === 401) {
                    window.location.href = '/';
                }
                if (error.response?.status === 429) {
                    console.warn('Rate limit exceeded');
                }
                return Promise.reject(error);
            }
        );
    }

    get(url, params) {
        return this.client.get(url, { params });
    }

    post(url, data) {
        return this.client.post(url, data);
    }

    put(url, data) {
        return this.client.put(url, data);
    }

    delete(url) {
        return this.client.delete(url);
    }

    upload(url, file, onProgress) {
        const formData = new FormData();
        formData.append('file', file);

        return this.client.post(url, formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            onUploadProgress: (progressEvent) => {
                if (onProgress) {
                    const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    onProgress(percent);
                }
            }
        });
    }
}

export const api = new ApiClient();

// API endpoints
export const broadcastApi = {
    getActive: () => api.get('/broadcast/active'),
    getById: (id) => api.get(`/broadcast/${id}`),
    start: (data) => api.post('/broadcast/start', data),
    end: (id) => api.post(`/broadcast/${id}/end`),
    getStats: (id) => api.get(`/broadcast/${id}/stats`)
};

export const chatApi = {
    getRooms: () => api.get('/chat/rooms'),
    getMessages: (roomSlug, page = 0, size = 50) =>
        api.get(`/chat/${roomSlug}/messages`, { page, size }),
    sendMessage: (roomSlug, data) =>
        api.post(`/chat/${roomSlug}/messages`, data),
    getUsers: (roomSlug) => api.get(`/chat/${roomSlug}/users`),
    getQuestions: (mode) => api.get(`/chat/questions/${mode}`),
    sendQuestion: (mode, data) => api.post(`/chat/questions/${mode}`, data),
    getQuestionsDto: (mode) => api.get(`/chat/questions-dto/${mode}`),
    sendQuestionDto: (mode, data) => api.post(`/chat/questions-dto/${mode}`, data),
    answerQuestionDto: (questionId, data) => api.post(`/chat/questions-dto/${questionId}/answer`, data),
    deleteQuestionDto: (questionId) => api.delete(`/chat/questions-dto/${questionId}`),
    archiveAnsweredQuestionsDto: (mode) => api.post(`/chat/questions-dto/${mode}/archive`)
};

export const paymentApi = {
    create: (data) => api.post('/payments', data),
    getById: (id) => api.get(`/payments/${id}`),
    getUserPayments: () => api.get('/payments/user'),
    confirm: (id, data) => api.post(`/payments/${id}/confirm`, data),
    getPaymentQR: (id) => api.get(`/payments/${id}/qr`)
};

export const userApi = {
    getMe: () => api.get('/users/me'),
    updateMe: (data) => api.put('/users/me', data),
    getById: (id) => api.get(`/users/${id}`)
};

export const mediaApi = {
    upload: (file, onProgress) => api.upload('/media/upload', file, onProgress),
    getStreamUrl: (id) => `${API_URL}/media/stream/${id}`,
    delete: (id) => api.delete(`/media/${id}`)
};

export const adminApi = {
    getStats: () => api.get('/admin/stats'),
    getUsers: (page, size) => api.get('/admin/users', { page, size }),
    updateUserRole: (userId, role) => api.put(`/admin/users/${userId}/role`, { role })
};

export const dashboardApi = {
    getMenuData: () => api.get('/dashboard/menu'),
    saveMenuData: (data) => api.post('/dashboard/menu', data)
};

export const feedApi = {
    getMainFeed: () => api.get('/feed/main'),
    getNutritionFeed: () => api.get('/feed/nutrition'),
    createPost: (data) => api.post('/feed/posts', data),
    updatePost: (id, data) => api.put(`/feed/posts/${id}`, data),
    deletePost: (id) => api.delete(`/feed/posts/${id}`)
};

// Удалены неиспользуемые demoApi (ранее дублировали функциональность)
// Все необходимые эндпоинты доступны через основные API.

// Events & Materials API (EventsHub)
export const eventsApi = {
    getUpcoming: () => api.get('/events/upcoming'),
    getPast: () => api.get('/events/past'),
    getNext: () => api.get('/events/next'),
    getById: (id) => api.get(`/events/${id}`),
    create: (data) => api.post('/events', data),
    update: (id, data) => api.put(`/events/${id}`, data),
    delete: (id) => api.delete(`/events/${id}`),
    getMaterialsBySection: (sectionId) => api.get(`/events/materials/${sectionId}`),
    getMaterial: (id) => api.get(`/events/materials/item/${id}`),
    createMaterial: (data) => api.post('/events/materials', data),
    updateMaterial: (id, data) => api.put(`/events/materials/${id}`, data),
    deleteMaterial: (id) => api.delete(`/events/materials/${id}`)
};

// Feedback API (отзывы и предложения в EventsHub)
export const feedbackApi = {
    getAll: () => api.get('/events/materials/feedback'),
    create: (data) => api.post('/events/materials', { ...data, sectionId: 'feedback', type: 'text' }),
    delete: (id) => api.delete(`/events/materials/${id}`)
};

// Notifications API
export const notificationApi = {
    getUserNotifications: () => api.get('/notifications'),
    getUnread: () => api.get('/notifications/unread'),
    markAsRead: (id) => api.put(`/notifications/${id}/read`),
    markAllAsRead: () => api.put('/notifications/read-all'),
    updateNotifications: (notifications) => api.post('/notifications/sync', notifications),
    getUpcomingEvents: () => api.get('/events/upcoming')
};

export default api;
