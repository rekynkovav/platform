import axios from 'axios';
import { getToken, updateToken } from '../keycloak';

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

            // Обновляем если истекает
            if (token && await updateToken()) {
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
                    // Token expired — logout
                    localStorage.clear();
                    sessionStorage.clear();
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
    getQuestions: (mode) => api.get(`/chat/questions/${mode}`),
    sendQuestion: (mode, data) => api.post(`/chat/questions/${mode}`, data)
};

export const paymentApi = {
    create: (data) => api.post('/payments', data),
    getById: (id) => api.get(`/payments/${id}`),
    getUserPayments: () => api.get('/payments/user'),
    confirm: (id, data) => api.post(`/payments/${id}/confirm`, data)
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

export default api;