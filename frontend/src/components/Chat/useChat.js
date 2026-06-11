import { useState, useEffect, useCallback, useRef } from 'react';
import { chatApi, mediaApi } from '../../services/api';
import chatLogger from '../../utils/chatLogger';

export const useChat = (chatRoom, config = {}) => {
    const {
        storageKeyPrefix = 'chatMessages',
        canSwitchRooms = true,
        availableRooms = [],
        useApi = true // Использовать API или sessionStorage (только в development)
    } = config;

    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentRoom, setCurrentRoom] = useState(chatRoom || 'default');
    const [canSwitch, setCanSwitch] = useState(canSwitchRooms);
    const [currentUserAuthor, setCurrentUserAuthor] = useState('');
    const [currentUserAvatar, setCurrentUserAvatar] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const [recordingInterval, setRecordingInterval] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState(null);

    // Кэш для URL медиа файлов
    const mediaUrlCache = useRef(new Map());
    const chunksRef = useRef([]);

    // Определяем текущего пользователя и права доступа
    useEffect(() => {
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        let author = 'Пользователь';
        let avatarUrl = '';

        if (demoRole === 'MODERATOR_ADMIN' || demoRole === 'DEVELOPER' || demoRole === 'SUPER_ADMIN' || demoRole === 'VYACHESLAV') {
            author = 'Админ';
            // Аватар для админа
            avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin&backgroundColor=dc3545&hairColor=000000&accessories=sunglasses';
        } else if (demoRole === 'USER_0') {
            author = 'Пользователь 1';
            // Аватар для USER_0 (демо)
            avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user0&backgroundColor=4a90e2&hairColor=2c1b18&accessories=round&facialHair=beardLight';
        } else if (demoRole === 'USER_1') {
            author = 'Пользователь 2';
            // Аватар для USER_1
            avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1&backgroundColor=28a745&hairColor=2c1b18&accessories=prescription02';
        } else {
            // Для других ролей - общий аватар
            avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + (demoRole || 'user') + '&backgroundColor=6c757d&hairColor=2c1b18';
        }

        setCurrentUserAuthor(author);
        setCurrentUserAvatar(avatarUrl);

        // Настройка прав переключения комнат
        // Только для общих чатов (user1, user2) применяем ограничения
        if (chatRoom === 'user1' || chatRoom === 'user2') {
            if (demoRole === 'USER_0') {
                setCurrentRoom('user1');
                setCanSwitch(false);
            } else if (demoRole === 'USER_1') {
                setCurrentRoom('user1'); // USER_1 тоже видит только чат новичков
                setCanSwitch(false); // Убираем переключатель для USER_1
            }
        } else {
            // Для специализированных чатов (nutrition и других) оставляем как есть
            setCurrentRoom(chatRoom);
        }
    }, []);

    // Определение типа медиа по MIME
    const getMediaType = (mimeType) => {
        if (mimeType?.startsWith('image/')) return 'image';
        if (mimeType?.startsWith('video/')) return 'video';
        if (mimeType?.startsWith('audio/')) return 'audio';
        return 'file';
    };

    // Загрузка сообщений с API
    const loadMessagesFromApi = useCallback(async (room) => {
        try {
            const response = await chatApi.getMessages(room, 0, 50);

            // Преобразуем сообщения с медиа
            const msgs = await Promise.all(response.data.map(async (msg) => {
                if (msg.mediaId) {
                    // ПолучаемPresigned URL для медиа
                    try {
                        const mediaResponse = await mediaApi.getStreamUrl(msg.mediaId);
                        return {
                            ...msg,
                            mediaUrl: mediaResponse.url,
                            mediaType: msg.mediaType || 'file'
                        };
                    } catch (e) {
                        console.error('Failed to load media URL:', e);
                        return msg;
                    }
                }
                return msg;
            }));

            setMessages(msgs);
            setLoading(false);
        } catch (error) {
            console.error('Failed to load messages from API:', error);
            setError('Не удалось загрузить сообщения. Проверьте подключение к интернету.');
            // Fallback к sessionStorage (только в development)
            loadMessagesFromStorage(room);
        }
    }, []);

    // Загрузка из sessionStorage (только для разработки)
    const loadMessagesFromStorage = useCallback((room) => {
        if (process.env.NODE_ENV !== 'development') {
            setMessages([]);
            setLoading(false);
            return;
        }

        const key = `${storageKeyPrefix}_${room}`;
        const savedMessages = sessionStorage.getItem(key);

        const FORCE_TEST_MESSAGES = process.env.NODE_ENV === 'development';

        if (savedMessages && !FORCE_TEST_MESSAGES) {
            try {
                let messages = JSON.parse(savedMessages);

                // Добавляем avatarUrl к старым сообщениям, если его нет
                messages = messages.map(msg => {
                    if (!msg.avatarUrl) {
                        // Генерируем аватар на основе имени автора
                        let avatarSeed = msg.author.toLowerCase().replace(/\s+/g, '');
                        let backgroundColor = '6c757d'; // серый по умолчанию

                        if (msg.author === 'Админ' || msg.author.includes('Админ')) {
                            backgroundColor = 'dc3545'; // красный для админов
                        } else if (msg.author === 'Система') {
                            backgroundColor = '0d6efd'; // синий для системы
                        } else if (msg.author.includes('1')) {
                            backgroundColor = '4a90e2'; // синий для пользователя 1
                        } else if (msg.author.includes('2')) {
                            backgroundColor = '28a745'; // зеленый для пользователя 2
                        }

                        return {
                            ...msg,
                            avatarUrl: `https://api.dicebear.com/7.x/avataaars/svg?seed=${avatarSeed}&backgroundColor=${backgroundColor}&hairColor=2c1b18`
                        };
                    }
                    return msg;
                });

                setMessages(messages);
            } catch (e) {
                console.warn('Corrupted sessionStorage data:', e);
                setMessages([]);
            }
        } else {
            // Тестовая переписка для разных чатов
            let initialMessages = [];

            if (room.includes('user1') || room.includes('general')) {
                // Общий чат / General Chat
                initialMessages = [
                    {
                        id: 1,
                        author: 'Система',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
                        content: 'Добро пожаловать в общий чат! Здесь вы можете задавать вопросы и общаться с другими пользователями.',
                        timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
                    },
                    {
                        id: 2,
                        author: 'Алексей',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=alexey&backgroundColor=4a90e2&hairColor=2c1b18&facialHair=beardLight',
                        content: 'Привет всем! Я новичок в спорте, с чего лучше начать?',
                        timestamp: new Date(Date.now() - 3600000 * 24).toISOString()
                    },
                    {
                        id: 3,
                        author: 'Мария',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=maria&backgroundColor=e91e63&hairColor=ffc0cb&accessories=round',
                        content: 'Привет! Я начала с утренней зарядки и прогулок. Главное - регулярность!',
                        timestamp: new Date(Date.now() - 3600000 * 23).toISOString()
                    },
                    {
                        id: 4,
                        author: 'Иван',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ivan&backgroundColor=ff9800&hairColor=2c1b18&accessories=prescription02',
                        content: 'Советую начать с базовых упражнений: приседания, отжимания, планка. Не торопитесь увеличивать нагрузку.',
                        timestamp: new Date(Date.now() - 3600000 * 22).toISOString()
                    },
                    {
                        id: 5,
                        author: 'Алексей',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=alexey&backgroundColor=4a90e2&hairColor=2c1b18&facialHair=beardLight',
                        content: 'Спасибо за советы! Буду пробовать. Кто-то занимается дома или только в зале?',
                        timestamp: new Date(Date.now() - 3600000 * 21).toISOString()
                    }
                ];
            } else if (room.includes('user2')) {
                // Чат продвинутых
                initialMessages = [
                    {
                        id: 1,
                        author: 'Система',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
                        content: 'Добро пожаловать в чат продвинутых! Здесь обсуждаем продвинутые техники и программы тренировок.',
                        timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
                    },
                    {
                        id: 2,
                        author: 'Сергей',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=sergey&backgroundColor=2196f3&hairColor=2c1b18&facialHair=beardMajestic',
                        content: 'Коллеги, кто пробовал программу 5/3/1? Какие результаты?',
                        timestamp: new Date(Date.now() - 3600000 * 20).toISOString()
                    },
                    {
                        id: 3,
                        author: 'Ольга',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=olga&backgroundColor=9c27b0&hairColor=ffc0cb&accessories=sunglasses',
                        content: 'Я на 5/3/1 уже 3 месяца. Присед +20кг, жим +15кг. Отличная программа для силовых!',
                        timestamp: new Date(Date.now() - 3600000 * 19).toISOString()
                    },
                    {
                        id: 4,
                        author: 'Дмитрий',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=dmitry&backgroundColor=4caf50&hairColor=2c1b18&accessories=prescription01',
                        content: 'А как с восстановлением? У меня после тяжелых тренировок крепатура на 3 дня.',
                        timestamp: new Date(Date.now() - 3600000 * 18).toISOString()
                    },
                    {
                        id: 5,
                        author: 'Ольга',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=olga&backgroundColor=9c27b0&hairColor=ffc0cb&accessories=sunglasses',
                        content: 'Важно питание и сон. Я пью BCAA, магний, и сплю минимум 8 часов. Помогает!',
                        timestamp: new Date(Date.now() - 3600000 * 17).toISOString()
                    }
                ];
            } else if (room.includes('nutrition')) {
                // Чат по питанию
                initialMessages = [
                    {
                        id: 1,
                        author: 'Система',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
                        content: 'Добро пожаловать в чат по питанию! Здесь обсуждаем диеты, БЖУ, добавки и здоровое питание.',
                        timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
                    },
                    {
                        id: 2,
                        author: 'Консультант Питание',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=nutrition_consultant&backgroundColor=fd7e14&hairColor=2c1b18&accessories=prescription02',
                        content: 'Приветствую! Я консультант по питанию. Задавайте вопросы по диетам, БЖУ, добавкам.',
                        timestamp: new Date(Date.now() - 3600000 * 15).toISOString()
                    },
                    {
                        id: 3,
                        author: 'Анна',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=anna&backgroundColor=ff5722&hairColor=ffc0cb&accessories=round',
                        content: 'Здравствуйте! Хочу набрать мышечную массу. Сколько белка нужно в день при весе 60кг?',
                        timestamp: new Date(Date.now() - 3600000 * 14).toISOString()
                    },
                    {
                        id: 4,
                        author: 'Консультант Питание',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=nutrition_consultant&backgroundColor=fd7e14&hairColor=2c1b18&accessories=prescription02',
                        content: 'При наборе массы рекомендуется 1.6-2.2г белка на кг веса. Для вас это 96-132г в день.',
                        timestamp: new Date(Date.now() - 3600000 * 13).toISOString()
                    },
                    {
                        id: 5,
                        author: 'Максим',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=maxim&backgroundColor=00bcd4&hairColor=2c1b18&facialHair=beardLight',
                        content: 'А креатин действительно работает? Стоит ли принимать?',
                        timestamp: new Date(Date.now() - 3600000 * 12).toISOString()
                    },
                    {
                        id: 6,
                        author: 'Консультант Питание',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=nutrition_consultant&backgroundColor=fd7e14&hairColor=2c1b18&accessories=prescription02',
                        content: 'Креатин - одна из самых исследованных добавок. Увеличивает силу и мышечную массу. Рекомендую 5г в день.',
                        timestamp: new Date(Date.now() - 3600000 * 11).toISOString()
                    }
                ];
            } else {
                // Общий чат по умолчанию
                initialMessages = [
                    {
                        id: 1,
                        author: 'Система',
                        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
                        content: 'Добро пожаловать в чат!',
                        timestamp: new Date().toISOString()
                    }
                ];
            }

            setMessages(initialMessages);
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem(key, JSON.stringify(initialMessages));
            }
        }
        setLoading(false);
    }, [storageKeyPrefix]);

    // Основная функция загрузки
    const loadMessages = useCallback((room) => {
        if (useApi) {
            loadMessagesFromApi(room);
        } else {
            loadMessagesFromStorage(room);
        }
    }, [useApi, loadMessagesFromApi, loadMessagesFromStorage]);

    useEffect(() => {
        loadMessages(currentRoom);
    }, [currentRoom, loadMessages]);

    // Смена комнаты
    const changeRoom = useCallback((room) => {
        if (!canSwitch) return;
        setCurrentRoom(room);
    }, [canSwitch]);

    // Отправка сообщения
    const sendMessage = useCallback(async (content, mediaId = null) => {
        if (!content?.trim() && !mediaId) return;

        // Логирование текстового сообщения
        if (content?.trim()) {
            const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
                ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
                : 'UNKNOWN';
            const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                           currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

            chatLogger.logTextMessage(chatType, currentRoom, demoRole, content);
        }

        const tempId = `temp_${Date.now()}`;
        const tempMessage = {
            id: tempId,
            author: currentUserAuthor,
            avatarUrl: currentUserAvatar,
            content: content?.trim() || '',
            mediaId,
            timestamp: new Date().toISOString(),
            pending: true
        };

        // Добавляем временное сообщение
        setMessages(prev => [...prev, tempMessage]);

        if (useApi) {
            try {
                const response = await chatApi.sendMessage(content, mediaId);
                // Заменяем временное сообщение реальным, сохраняя avatarUrl
                setMessages(prev =>
                    prev.map(msg =>
                        msg.id === tempId
                            ? { ...response.data, avatarUrl: currentUserAvatar, pending: false }
                            : msg
                    )
                );
                return response.data;
            } catch (error) {
                console.error('Failed to send message:', error);
                // Удаляем временное сообщение при ошибке
                setMessages(prev => prev.filter(msg => msg.id !== tempId));
                setError('Не удалось отправить сообщение. Проверьте подключение к интернету.');
                throw error;
            }
        } else {
            // LocalStorage режим
            const message = {
                id: Date.now(),
                author: currentUserAuthor,
                avatarUrl: currentUserAvatar,
                content: content.trim(),
                mediaId,
                timestamp: new Date().toISOString()
            };

            setMessages(prev =>
                prev.map(msg =>
                    msg.id === tempId
                        ? { ...message, pending: false }
                        : msg
                )
            );

            const key = `${storageKeyPrefix}_${currentRoom}`;
            let existing;
            try {
                const saved = process.env.NODE_ENV === 'development' ? sessionStorage.getItem(key) : null;
                existing = saved ? JSON.parse(saved) : [];
            } catch (e) {
                existing = [];
            }
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem(key, JSON.stringify([...existing, message]));
            }

            return message;
        }
    }, [currentUserAuthor, currentRoom, useApi, storageKeyPrefix]);

    // Загрузка файла на сервер
    const uploadMedia = useCallback(async (file) => {
        setUploading(true);

        // Логирование загрузки файла
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
            : 'UNKNOWN';
        const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                       currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

        chatLogger.logFileUpload(chatType, currentRoom, demoRole, file);

        try {
            const type = getMediaType(file.type);
            const response = await mediaApi.upload(file, type);
            const mediaFile = response.data;

            // Получаем presigned URL для предпросмотра
            let mediaUrl = null;
            try {
                const urlResponse = await mediaApi.getStreamUrl(mediaFile.id);
                mediaUrl = urlResponse.url;
            } catch (e) {
                console.warn('Could not get preview URL:', e);
            }

            return {
                mediaId: mediaFile.id,
                mediaType: type,
                mediaUrl,
                fileName: file.name
            };
        } catch (error) {
            console.error('Failed to upload media:', error);
            throw error;
        } finally {
            setUploading(false);
        }
    }, []);

    // Старая функция для совместимости (sessionStorage)
    const uploadFile = useCallback((file) => {
        // Логирование загрузки файла (sessionStorage режим)
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
            : 'UNKNOWN';
        const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                       currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

        chatLogger.logFileUpload(chatType, currentRoom, demoRole, file);

        return new Promise((resolve, reject) => {
            const reader = new FileReader();

            reader.onload = (event) => {
                const message = {
                    id: Date.now(),
                    author: currentUserAuthor,
                    content: file.name,
                    fileUrl: event.target.result,
                    fileType: file.type,
                    fileName: file.name,
                    timestamp: new Date().toISOString(),
                    type: getMediaType(file.type)
                };

                setMessages(prev => [...prev, message]);

                const key = `${storageKeyPrefix}_${currentRoom}`;
                let existing;
                try {
                    const saved = process.env.NODE_ENV === 'development'
                        ? sessionStorage.getItem(key)
                        : null;
                    existing = saved ? JSON.parse(saved) : [];
                } catch (e) {
                    existing = [];
                }
                if (process.env.NODE_ENV === 'development') {
                    sessionStorage.setItem(key, JSON.stringify([...existing, message]));
                }

                resolve(message);
            };

            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(file);
        });
    }, [currentUserAuthor, currentRoom, storageKeyPrefix]);

    // Редактирование сообщения
    const editMessage = useCallback((msgId, newContent) => {
        if (!newContent.trim()) return;

        // Логирование редактирования
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
            : 'UNKNOWN';
        const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                       currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

        // Находим старое сообщение для логирования
        const oldMessage = messages.find(msg => msg.id === msgId);
        if (oldMessage) {
            chatLogger.logMessageEdit(chatType, currentRoom, demoRole, msgId, oldMessage.content, newContent);
        }

        if (useApi) {
            // TODO: API для редактирования
            console.warn('Edit via API not implemented yet');
        }

        setMessages(prev => {
            const updated = prev.map(msg =>
                msg.id === msgId
                    ? { ...msg, content: newContent.trim(), edited: true }
                    : msg
            );
            if (!useApi) {
                const key = `${storageKeyPrefix}_${currentRoom}`;
                if (process.env.NODE_ENV === 'development') {
                    sessionStorage.setItem(key, JSON.stringify(updated));
                }
            }
            return updated;
        });
    }, [currentRoom, useApi, storageKeyPrefix]);

    // Удаление сообщения
    const deleteMessage = useCallback((msgId) => {
        if (!window.confirm('Удалить это сообщение?')) return false;

        // Логирование удаления
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
            : 'UNKNOWN';
        const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                       currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

        chatLogger.logMessageDelete(chatType, currentRoom, demoRole, msgId);

        if (useApi) {
            // TODO: API для удаления
            console.warn('Delete via API not implemented yet');
        }

        setMessages(prev => {
            const messageToDelete = prev.find(msg => msg.id === msgId);
            if (!messageToDelete || messageToDelete.author !== currentUserAuthor) {
                alert('Вы можете удалять только свои сообщения');
                return prev;
            }
            const updated = prev.filter(msg => msg.id !== msgId);
            if (!useApi) {
                const key = `${storageKeyPrefix}_${currentRoom}`;
                if (process.env.NODE_ENV === 'development') {
                    sessionStorage.setItem(key, JSON.stringify(updated));
                }
            }
            return updated;
        });

        return true;
    }, [currentUserAuthor, currentRoom, useApi, storageKeyPrefix]);

    // Запись голосового
    const startRecording = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            chunksRef.current = [];

            recorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunksRef.current.push(e.data);
                }
            };

            recorder.onstop = async () => {
                const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
                stream.getTracks().forEach(track => track.stop());

                if (useApi) {
                    // Логирование голосового сообщения (API режим)
                    const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
                ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
                : 'UNKNOWN';
                    const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                                   currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

                    chatLogger.logVoiceMessage(chatType, currentRoom, demoRole, recordingTime);

                    // Загружаем на сервер
                    try {
                        setUploading(true);
                        const type = 'audio';
                        const response = await mediaApi.upload(blob, type);
                        const mediaFile = response.data;

                        // Получаем URL для воспроизведения
                        let mediaUrl = null;
                        try {
                            const urlResponse = await mediaApi.getStreamUrl(mediaFile.id);
                            mediaUrl = urlResponse.url;
                        } catch (e) {
                            console.warn('Could not get audio URL:', e);
                        }

                        // Отправляем сообщение с медиа
                        await sendMessage('Голосовое сообщение', mediaFile.id);

                        // Добавляем в локальный кэш
                        mediaUrlCache.current.set(mediaFile.id, mediaUrl);
                    } catch (error) {
                        console.error('Failed to upload voice message:', error);
                        alert('Не удалось загрузить голосовое сообщение');
                    } finally {
                        setUploading(false);
                    }
                } else {
                    // Логирование голосового сообщения (sessionStorage режим)
                    const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
                ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
                : 'UNKNOWN';
                    const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                                   currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

                    chatLogger.logVoiceMessage(chatType, currentRoom, demoRole, recordingTime);

                    // LocalStorage - сохраняем как base64
                    const reader = new FileReader();
                    reader.onload = (event) => {
                        const message = {
                            id: Date.now(),
                            author: currentUserAuthor,
                            content: 'Голосовое сообщение',
                            fileUrl: event.target.result,
                            fileType: 'audio/webm',
                            timestamp: new Date().toISOString(),
                            type: 'voice'
                        };

                        setMessages(prev => {
                            const updated = [...prev, message];
                            const key = `${storageKeyPrefix}_${currentRoom}`;
                            if (process.env.NODE_ENV === 'development') {
                    sessionStorage.setItem(key, JSON.stringify(updated));
                }
                            return updated;
                        });
                    };
                    reader.readAsDataURL(blob);
                }
            };

            recorder.start();
            setMediaRecorder(recorder);
            setIsRecording(true);
            setRecordingTime(0);

            const interval = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
            setRecordingInterval(interval);
        } catch (error) {
            console.error('Ошибка доступа к микрофону:', error);
            alert('Не удалось получить доступ к микрофону');
        }
    }, [currentUserAuthor, currentRoom, useApi, sendMessage, storageKeyPrefix]);

    // Cleanup записи при размонтировании
    useEffect(() => {
        return () => {
            if (recordingInterval) {
                clearInterval(recordingInterval);
            }
        };
    }, [recordingInterval]);

    const stopRecording = useCallback(() => {
        if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.stop();
            setIsRecording(false);
            if (recordingInterval) {
                clearInterval(recordingInterval);
                setRecordingInterval(null);
            }
        }
    }, [mediaRecorder, recordingInterval]);

    // Получение URL для медиа (с кэшированием)
    const getMediaUrl = useCallback(async (mediaId) => {
        if (mediaUrlCache.current.has(mediaId)) {
            return mediaUrlCache.current.get(mediaId);
        }

        if (useApi) {
            try {
                const response = await mediaApi.getStreamUrl(mediaId);
                const url = response.url;
                mediaUrlCache.current.set(mediaId, url);
                return url;
            } catch (error) {
                console.error('Failed to get media URL:', error);
                return null;
            }
        }

        return null;
    }, [useApi]);

    // Форматирование времени
    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    return {
        // Состояние
        messages,
        loading,
        currentRoom,
        canSwitch,
        currentUserAuthor,
        isRecording,
        recordingTime,
        uploading,
        error,

        // Действия
        changeRoom,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadFile,
        uploadMedia,      // Новая функция для загрузки на сервер
        startRecording,
        stopRecording,
        formatRecordingTime,
        getMediaUrl,
        loadMessages,
        clearError: () => setError(null)
    };
};

export default useChat;