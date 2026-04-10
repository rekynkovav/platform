// services/api.js
import axios from 'axios';
import { getToken, updateToken } from '../keycloak';

const API_URL = process.env.REACT_APP_API_URL || 'https://api.platform.com';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Интерцептор для добавления токена
api.interceptors.request.use(async (config) => {
    try {
        await updateToken(30);
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
    } catch (error) {
        console.error('Token refresh failed:', error);
    }
    return config;
});

// Интерцептор для обработки ошибок
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// API для событий
export const eventsApi = {
    getNext: () => api.get('/api/events/next'),
    getAll: () => api.get('/api/events'),
    getById: (id) => api.get(`/api/events/${id}`),
    create: (data) => api.post('/api/events', data),
    update: (id, data) => api.put(`/api/events/${id}`, data),
    delete: (id) => api.delete(`/api/events/${id}`)
};

// API для постов
export const postsApi = {
    getFeed: (limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get('/api/posts/feed', { params });
    },
    getById: (id) => api.get(`/api/posts/${id}`),
    create: (data) => api.post('/api/posts', data),
    update: (id, data) => api.put(`/api/posts/${id}`, data),
    delete: (id) => api.delete(`/api/posts/${id}`),
    uploadMedia: (formData) => api.post('/api/posts/media', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
};

// API для комментариев
export const commentsApi = {
    getByPost: (postId, limit = 20) => api.get(`/api/posts/${postId}/comments`, { params: { limit } }),
    create: (postId, content) => api.post(`/api/posts/${postId}/comments`, { content }),
    delete: (postId, commentId) => api.delete(`/api/posts/${postId}/comments/${commentId}`)
};

// API для реакций
export const reactionsApi = {
    toggle: (postId, type) => api.post(`/api/posts/${postId}/reactions`, { type }),
    getByPost: (postId) => api.get(`/api/posts/${postId}/reactions`)
};

// API для админов (помощь)
export const adminsApi = {
    getList: () => api.get('/api/admins'),
    getStatus: (adminId) => api.get(`/api/admins/${adminId}/status`),
    sendMessage: (adminId, content) => api.post(`/api/admins/${adminId}/messages`, { content }),
    sendToAll: (content) => api.post('/api/admins/messages', { content })
};

// API для архива
export const archiveApi = {
    getAll: () => api.get('/api/archive'),
    getById: (id) => api.get(`/api/archive/${id}`)
};

// API для чата
export const chatApi = {
    getMessages: (roomId = 'general') => api.get(`/api/chat/${roomId}/messages`),
    sendMessage: (roomId, content) => api.post(`/api/chat/${roomId}/messages`, { content })
};

// API для контактов
export const contactsApi = {
    getList: () => api.get('/api/contacts'),
    getById: (id) => api.get(`/api/contacts/${id}`)
};

export default api;