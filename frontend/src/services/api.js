// services/api.js
import axios from 'axios';
import { getToken, updateToken } from '../keycloak';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081';

const api = axios.create({
    baseURL: API_URL,
    headers: { 'Content-Type': 'application/json' }
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

// API для чата (исправлен - добавлены недостающие методы)
export const chatApi = {
    getMessages: (limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get('/api/chat/general', { params });
    },
    sendMessage: (content) => api.post('/api/chat/general', { content }),
    getGeneralMessages: (limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get('/api/chat/general', { params });
    },
    sendToGeneralChat: (content) => api.post('/api/chat/general', { content }),
    getAdminGroupMessages: (limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get('/api/chat/admin-group', { params });
    },
    sendToAdminGroup: (content) => api.post('/api/chat/admin-group', { content }),
    getPrivateMessages: (userId, limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get(`/api/chat/private/${userId}`, { params });
    },
    sendPrivateMessage: (userId, content) => api.post(`/api/chat/private/${userId}`, { content }),
    getUnreadCount: () => api.get('/api/chat/unread'),
    markAsRead: (messageId) => api.put(`/api/chat/messages/${messageId}/read`)
};

// API для архива
export const archiveApi = {
    getAll: () => api.get('/api/archives'),
    getById: (id) => api.get(`/api/archives/${id}`),
    getByEvent: (eventId) => api.get(`/api/archives/event/${eventId}`),
    create: (data) => api.post('/api/archives', data),
    update: (id, data) => api.put(`/api/archives/${id}`, data),
    delete: (id) => api.delete(`/api/archives/${id}`),
    incrementViews: (id) => api.post(`/api/archives/${id}/views`)
};

// API для контактов
export const contactsApi = {
    getAll: (search = null) => api.get('/api/contacts', { params: { search } }),
    add: (contactUserId, nickname = null) => api.post('/api/contacts', { contactUserId, nickname }),
    remove: (contactId) => api.delete(`/api/contacts/${contactId}`),
    updateNickname: (contactId, nickname) => api.put(`/api/contacts/${contactId}`, { nickname })
};

// API для админов (исправлен - добавлены недостающие методы)
export const adminsApi = {
    getList: () => api.get('/api/admins'),
    getMessages: (adminId) => api.get(`/api/admins/${adminId}/messages`),
    getGroupMessages: () => api.get('/api/admins/group/messages'),
    getStatus: (adminId) => api.get(`/api/admins/${adminId}/status`),
    sendMessage: (adminId, content) => api.post(`/api/admins/${adminId}/messages`, { content }),
    sendToAll: (content) => api.post('/api/admins/messages', { content })
};

export default api;