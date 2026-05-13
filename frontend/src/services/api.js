import axios from 'axios';

// Создаём базовый экземпляр axios
const api = axios.create({
    baseURL: '/api',
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Интерцептор для обработки ошибок
api.interceptors.response.use(
    response => response,
    error => {
        console.error('API Error:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);

// ==================== API СЕКЦИИ ====================

export const eventsApi = {
    getAll: () => api.get('/events'),
    getUpcoming: () => api.get('/events/upcoming'),
    getPast: () => api.get('/events/past'),
    getNext: () => api.get('/events/next'),
    create: (data) => api.post('/events', data),
    update: (id, data) => api.put(`/events/${id}`, data),
    delete: (id) => api.delete(`/events/${id}`)
};

export const archiveApi = {
    getAll: () => api.get('/archives'),
    create: (data) => api.post('/archives', data),
    update: (id, data) => api.put(`/archives/${id}`, data),
    delete: (id) => api.delete(`/archives/${id}`)
};

export const chatApi = {
    getGeneralMessages: (limit = 100) => api.get('/chat/general', { params: { limit } }),
    sendToGeneralChat: (content, mediaId = null) =>
        api.post('/chat/general', { content, mediaId })
};

export const mediaApi = {
    upload: (file, type = 'file') => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', type);
        return api.post('/media/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },
    stream: (mediaId) => api.get(`/media/stream/${mediaId}`)
};

export const postsApi = {
    getFeed: (limit = 20) => api.get('/posts/feed', { params: { limit } }),
    getById: (id) => api.get(`/posts/${id}`),
    create: (data) => api.post('/posts', data),
    update: (id, data) => api.put(`/posts/${id}`, data),
    delete: (id) => api.delete(`/posts/${id}`)
};

export const usersApi = {
    getCurrentUser: () => api.get('/users/me'),
    updateCurrentUser: (data) => api.put('/users/me', data)
};

export const contactsApi = {
    getAll: (search = '') => api.get('/contacts', { params: { search } })
};

// Экспорт всего API
export default {
    eventsApi,
    archiveApi,
    chatApi,
    mediaApi,
    postsApi,
    usersApi,
    contactsApi,
    // Для удобства можно использовать напрямую api
    api
};