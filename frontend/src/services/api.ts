import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { getToken, updateToken } from '../keycloak';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';
const USE_MOCK = process.env.REACT_APP_USE_MOCK_AUTH === 'true';

interface ApiResponse<T = any> {
  data: T;
  status: number;
  statusText: string;
  headers: any;
  config: AxiosRequestConfig;
}

interface ApiError extends AxiosError {
  response?: {
    status: number;
    data: any;
  };
}

interface UploadProgressEvent {
  loaded: number;
  total: number;
}

interface UploadProgressCallback {
  (percent: number): void;
}

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // Request interceptor — добавляем JWT
    this.client.interceptors.request.use(async (config: AxiosRequestConfig) => {
      let token = getToken();

      // Обновляем если истекает
      if (token && await updateToken()) {
        token = getToken();
      }

      if (token && !USE_MOCK) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    });

    // Response interceptor — обработка ошибок
    this.client.interceptors.response.use(
      (response: AxiosResponse) => response.data,
      (error: ApiError) => {
        const status = error.response?.status;
        const message = error.response?.data?.message || error.message;

        if (status === 401) {
          // Token expired — logout
          if (process.env.NODE_ENV === 'development') {
            sessionStorage.clear();
          }
          sessionStorage.clear();
          window.location.href = '/';
        } else if (status === 403) {
          console.error('Access forbidden:', message);
          throw new Error('Доступ запрещен. У вас недостаточно прав.');
        } else if (status === 404) {
          console.error('Resource not found:', message);
          throw new Error('Ресурс не найден.');
        } else if (status === 429) {
          console.warn('Rate limit exceeded');
          throw new Error('Превышен лимит запросов. Попробуйте позже.');
        } else if (status >= 500) {
          console.error('Server error:', message);
          throw new Error('Ошибка сервера. Попробуйте позже.');
        } else if (error.code === 'ECONNABORTED') {
          console.error('Request timeout');
          throw new Error('Таймаут запроса. Проверьте подключение к интернету.');
        } else if (error.code === 'NETWORK_ERROR') {
          console.error('Network error');
          throw new Error('Ошибка сети. Проверьте подключение к интернету.');
        }

        // Общая ошибка
        console.error('API error:', error);
        throw new Error(message || 'Произошла ошибка при выполнении запроса.');
      }
    );
  }

  get<T = any>(url: string, params?: any): Promise<T> {
    return this.client.get(url, { params });
  }

  post<T = any>(url: string, data?: any): Promise<T> {
    return this.client.post(url, data);
  }

  put<T = any>(url: string, data?: any): Promise<T> {
    return this.client.put(url, data);
  }

  delete<T = any>(url: string): Promise<T> {
    return this.client.delete(url);
  }

  upload<T = any>(url: string, file: File, onProgress?: UploadProgressCallback): Promise<T> {
    const formData = new FormData();
    formData.append('file', file);

    return this.client.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent: any) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      }
    });
  }
}

export const api = new ApiClient();

// API endpoints
export interface Broadcast {
  id: string;
  title: string;
  description?: string;
  streamType: string;
  ownerId: string;
  status: 'IDLE' | 'LIVE' | 'ENDED';
  rtmpStreamKey: string;
  startedAt?: string;
  endedAt?: string;
  viewerCount?: number;
  maxViewers?: number;
}

export interface BroadcastStats {
  broadcastId: string;
  totalViewers: number;
  peakViewers: number;
  averageViewTime: number;
  messagesCount: number;
}

export const broadcastApi = {
  getActive: (): Promise<Broadcast[]> => api.get('/broadcast/active'),
  getById: (id: string): Promise<Broadcast> => api.get(`/broadcast/${id}`),
  start: (data: { title: string; description?: string; streamType: string }): Promise<Broadcast> =>
    api.post('/broadcast/start', data),
  end: (id: string): Promise<void> => api.post(`/broadcast/${id}/end`),
  getStats: (id: string): Promise<BroadcastStats> => api.get(`/broadcast/${id}/stats`)
};

export interface ChatRoom {
  id: string;
  slug: string;
  title: string;
  description?: string;
  type: 'general' | 'admin' | 'private' | 'questions' | 'nutrition' | 'workout';
  createdAt: string;
  updatedAt?: string;
  isActive: boolean;
  participantCount?: number;
  lastMessageAt?: string;
}

export interface ChatMessage {
  id: string;
  content: string;
  userId: string;
  userName: string;
  chatType: string;
  recipientId?: string;
  isRead: boolean;
  createdAt: string;
  mediaId?: string;
  mediaType?: string;
  fileName?: string;
  roomId?: string;
  roomSlug?: string;
}

export interface ChatQuestion {
  id: string;
  question: string;
  answer?: string;
  mode: 'nutrition' | 'workout' | 'general';
  userId: string;
  userName: string;
  createdAt: string;
  answeredAt?: string;
}

export const chatApi = {
  getRooms: (): Promise<ChatRoom[]> => api.get('/chat/rooms'),
  getMessages: (roomSlug: string, page: number = 0, size: number = 50): Promise<ChatMessage[]> =>
    api.get(`/chat/${roomSlug}/messages`, { page, size }),
  sendMessage: (roomSlug: string, data: { content: string; mediaId?: string }): Promise<ChatMessage> =>
    api.post(`/chat/${roomSlug}/messages`, data),
  getQuestions: (mode: 'nutrition' | 'workout' | 'general'): Promise<ChatQuestion[]> =>
    api.get(`/chat/questions/${mode}`),
  sendQuestion: (mode: 'nutrition' | 'workout' | 'general', data: { question: string }): Promise<ChatQuestion> =>
    api.post(`/chat/questions/${mode}`, data)
};

export interface Payment {
  id: string;
  userId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED';
  description?: string;
  notes?: string;
  createdAt: string;
  paidAt?: string;
  confirmedBy?: string;
}

export const paymentApi = {
  create: (data: { amount: number; description?: string }): Promise<Payment> => api.post('/payments', data),
  getById: (id: string): Promise<Payment> => api.get(`/payments/${id}`),
  getUserPayments: (): Promise<Payment[]> => api.get('/payments/user'),
  confirm: (id: string, data: { notes?: string }): Promise<Payment> => api.post(`/payments/${id}/confirm`, data)
};

export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  createdAt: string;
  updatedAt?: string;
  isActive: boolean;
}

export const userApi = {
  getMe: (): Promise<User> => api.get('/users/me'),
  updateMe: (data: Partial<User>): Promise<User> => api.put('/users/me', data),
  getById: (id: string): Promise<User> => api.get(`/users/${id}`)
};

export interface MediaFile {
  id: string;
  originalFilename: string;
  fileType: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
  url?: string;
}

export const mediaApi = {
  upload: (file: File, onProgress?: UploadProgressCallback): Promise<MediaFile> =>
    api.upload('/media/upload', file, onProgress),
  getStreamUrl: (id: string): string => `${API_URL}/media/stream/${id}`,
  delete: (id: string): Promise<void> => api.delete(`/media/${id}`)
};

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  totalPayments: number;
  totalRevenue: number;
  activeBroadcasts: number;
  totalMessages: number;
}

export interface UserWithRole extends User {
  lastLogin?: string;
  paymentCount?: number;
}

export const adminApi = {
  getStats: (): Promise<AdminStats> => api.get('/admin/stats'),
  getUsers: (page: number, size: number): Promise<{ users: UserWithRole[]; total: number }> =>
    api.get('/admin/users', { page, size }),
  updateUserRole: (userId: string, role: string): Promise<User> =>
    api.put(`/admin/users/${userId}/role`, { role })
};

export default api;