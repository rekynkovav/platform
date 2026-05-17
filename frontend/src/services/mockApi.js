import { mockUser, mockEvents, mockChatMessages, mockArchiveItems, mockContacts } from '../mocks/mockData';

// Имитация задержки сети
const delay = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

export const mockAuth = {
    init: async () => {
        await delay(500);
        console.log('✅ Мок-авторизация: успешный вход');
        return true;
    },
    getUsername: () => mockUser.username,
    getUserRoles: () => mockUser.roles,
    logout: () => {
        console.log('🚪 Мок-выход из системы');
        window.location.reload();
    }
};

export const mockEventsApi = {
    getNext: async () => {
        await delay();
        return { data: mockEvents.filter(e => e.status === 'upcoming')[0] };
    },
    getAll: async () => {
        await delay();
        return { data: mockEvents };
    },
    getById: async (id) => {
        await delay();
        return { data: mockEvents.find(e => e.id === parseInt(id)) };
    }
};

export const mockChatApi = {
    getMessages: async (limit = 50, before = null) => {
        await delay();
        let messages = [...mockChatMessages];
        if (before) {
            messages = messages.filter(m => m.timestamp < before);
        }
        return { data: messages.slice(0, limit) };
    },
    sendMessage: async (content) => {
        await delay(200);
        const newMessage = {
            id: Date.now(),
            author: mockUser.username,
            content,
            timestamp: new Date().toISOString()
        };
        mockChatMessages.push(newMessage);
        return { data: newMessage };
    }
};

export const mockArchiveApi = {
    getAll: async () => {
        await delay();
        return { data: mockArchiveItems };
    }
};

export const mockContactsApi = {
    getAll: async () => {
        await delay();
        return { data: mockContacts };
    }
};