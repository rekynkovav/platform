// Имитация задержки сети
const delay = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

// Моковые данные
const mockUser = {
    username: 'Тестовый Пользователь',
    roles: ['USER_2'],
    id: 'mock-user-123'
};

export const mockAuth = {
    init: async () => {
        await delay(500);
        return true;
    },
    getUsername: () => mockUser.username,
    getUserRoles: () => mockUser.roles,
    logout: () => {
        window.location.reload();
    }
};

// Мок для событий (используется в ScheduledEvents и EventsHub)
export const mockEventsApi = {
    getAll: async () => {
        await delay();
        const saved = localStorage.getItem('scheduledEvents');
        const events = saved ? JSON.parse(saved) : [];
        return { data: events };
    },
    getUpcoming: async () => {
        await delay();
        const saved = localStorage.getItem('scheduledEvents');
        const events = saved ? JSON.parse(saved) : [];
        const now = new Date();
        const upcoming = events.filter(e => new Date(e.startTime || `${e.date}T${e.time}`) > now);
        return { data: upcoming };
    },
    getPast: async () => {
        await delay();
        const saved = localStorage.getItem('scheduledEvents');
        const events = saved ? JSON.parse(saved) : [];
        const now = new Date();
        const past = events.filter(e => new Date(e.startTime || `${e.date}T${e.time}`) <= now);
        return { data: past };
    },
    getNext: async () => {
        await delay();
        const saved = localStorage.getItem('scheduledEvents');
        const events = saved ? JSON.parse(saved) : [];
        const now = new Date();
        const upcoming = events
            .filter(e => new Date(e.startTime || `${e.date}T${e.time}`) > now)
            .sort((a, b) => new Date(a.startTime || `${a.date}T${a.time}`) - new Date(b.startTime || `${b.date}T${b.time}`));
        return { data: upcoming[0] || null };
    },
    create: async (data) => {
        await delay(300);
        const saved = localStorage.getItem('scheduledEvents');
        const events = saved ? JSON.parse(saved) : [];
        const newEvent = { id: Date.now().toString(), ...data, status: 'scheduled' };
        events.unshift(newEvent);
        localStorage.setItem('scheduledEvents', JSON.stringify(events));
        return { data: newEvent };
    },
    update: async (id, data) => { /* ... */ await delay(300); return { data }; },
    delete: async (id) => { /* ... */ await delay(200); return { data: { success: true } }; }
};

// Мок для архива
export const mockArchiveApi = {
    getAll: async () => {
        await delay();
        const saved = localStorage.getItem('archives');
        return { data: saved ? JSON.parse(saved) : [] };
    },
    create: async (data) => {
        await delay(300);
        const saved = localStorage.getItem('archives');
        const archives = saved ? JSON.parse(saved) : [];
        const newArchive = { id: Date.now().toString(), ...data };
        archives.unshift(newArchive);
        localStorage.setItem('archives', JSON.stringify(archives));
        return { data: newArchive };
    }
};

// Мок для чата (упрощённый)
export const mockChatApi = {
    getGeneralMessages: async () => {
        await delay();
        return { data: [] };
    },
    sendToGeneralChat: async (content, mediaId) => {
        await delay(200);
        return { data: { id: Date.now(), content, mediaId, author: mockUser.username, timestamp: new Date().toISOString() } };
    }
};

export default {
    mockAuth,
    mockEventsApi,
    mockArchiveApi,
    mockChatApi
};