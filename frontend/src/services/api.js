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
            window.location.href = window.location.origin + '/login';
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
    sendMessage: (content, mediaId = null) => {
        const payload = { content };
        if (mediaId) payload.mediaId = mediaId;
        return api.post('/api/chat/general', payload);
    },
    getAdminGroupMessages: (limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get('/api/chat/admin-group', { params });
    },
    sendToAdminGroup: (content, mediaId = null) => {
        const payload = { content };
        if (mediaId) payload.mediaId = mediaId;
        return api.post('/api/chat/admin-group', payload);
    },
    getPrivateMessages: (userId, limit = 50, before = null) => {
        const params = before ? { limit, before } : { limit };
        return api.get(`/api/chat/private/${userId}`, { params });
    },
    sendPrivateMessage: (userId, content, mediaId = null) => {
        const payload = { content };
        if (mediaId) payload.mediaId = mediaId;
        return api.post(`/api/chat/private/${userId}`, payload);
    },
    getUnreadCount: () => api.get('/api/chat/unread'),
    markAsRead: (messageId) => api.put(`/api/chat/messages/${messageId}/read`)
};

// API для медиа
export const mediaApi = {
    // Загрузка файла
    upload: (file, type) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', type);
        return api.post('/api/media/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },

    // Получение потокового URL (presigned)
    getStreamUrl: (fileId) => api.get(`/api/media/stream/${fileId}`),

    // Получение ключа шифрования (для расшифровки на клиенте)
    getEncryptionKey: (fileId) => api.get(`/api/media/key/${fileId}`, {
        responseType: 'arraybuffer'
    }),

    // Информация о файле
    getInfo: (fileId) => api.get(`/api/media/${fileId}`),

    // Скачивание
    download: (fileId) => api.get(`/api/media/download/${fileId}`, {
        responseType: 'arraybuffer'
    }),

    // HLS плейлист для видео
    getHLSPlaylist: (fileId) => api.get(`/api/media/hls/${fileId}/playlist.m3u8`),

    // Список моих файлов
    getMyFiles: () => api.get('/api/media/my-files'),

    // Удаление
    delete: (fileId) => api.delete(`/api/media/${fileId}`)
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

// API для пользователей
export const usersApi = {
    getCurrentUser: () => api.get('/api/users/me'),
    updateCurrentUser: (data) => api.put('/api/users/me', data),
    getUserById: (userId) => api.get(`/api/users/${userId}`)
};

// API для трансляций (live broadcasts)
export const broadcastApi = {
    create: (title, description, streamType = 'video') =>
        api.post('/api/broadcast/create', { title, description, streamType }),

    start: (broadcastId) =>
        api.post(`/api/broadcast/${broadcastId}/start`, {}),

    stop: (broadcastId) =>
        api.post(`/api/broadcast/${broadcastId}/stop`, {}),

    getActive: () =>
        api.get('/api/broadcast/active'),

    getById: (broadcastId) =>
        api.get(`/api/broadcast/${broadcastId}`),

    delete: (broadcastId) =>
        api.delete(`/api/broadcast/${broadcastId}`)
};

export default api;