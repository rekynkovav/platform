// Моковые данные для фронта
export const mockUser = {
    username: 'Тестовый Пользователь',
    roles: ['ROLE_USER'],
    id: 'mock-user-123'
};

export const mockEvents = [
    { id: 1, title: 'Еженедельный митинг', date: '2024-03-25T10:00:00', status: 'upcoming' },
    { id: 2, title: 'Релиз новой версии', date: '2024-03-28T14:00:00', status: 'upcoming' },
    { id: 3, title: 'Командный обед', date: '2024-03-20T12:30:00', status: 'completed' }
];

export const mockChatMessages = [
    { id: 1, author: 'Анна', content: 'Всем привет! 👋', timestamp: '2024-03-20T09:00:00' },
    { id: 2, author: 'Петр', content: 'Как дела у всех?', timestamp: '2024-03-20T09:15:00' },
    { id: 3, author: 'Мария', content: 'Отлично! Готовимся к релизу', timestamp: '2024-03-20T09:20:00' }
];

export const mockArchiveItems = [
    { id: 1, title: 'Прошедшее мероприятие 1', date: '2024-03-01', description: 'Описание прошедшего события' },
    { id: 2, title: 'Прошедшее мероприятие 2', date: '2024-02-15', description: 'Еще одно интересное событие' }
];

export const mockContacts = [
    { id: 1, username: 'Иван Иванов', status: 'online', lastSeen: '2024-03-20T10:30:00' },
    { id: 2, username: 'Елена Петрова', status: 'offline', lastSeen: '2024-03-19T15:20:00' },
    { id: 3, username: 'Сергей Сидоров', status: 'online', lastSeen: '2024-03-20T11:00:00' }
];
