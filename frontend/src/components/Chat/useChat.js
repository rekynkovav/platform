import { useState, useEffect, useCallback, useRef } from 'react';
import { chatApi, mediaApi } from '../../services/api';

export const useChat = (chatRoom, config = {}) => {
    const {
        storageKeyPrefix = 'chatMessages',
        canSwitchRooms = true,
        availableRooms = [],
        useApi = true // Использовать API или localStorage
    } = config;

    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentRoom, setCurrentRoom] = useState(chatRoom || 'default');
    const [canSwitch, setCanSwitch] = useState(canSwitchRooms);
    const [currentUserAuthor, setCurrentUserAuthor] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const [recordingInterval, setRecordingInterval] = useState(null);
    const [uploading, setUploading] = useState(false);

    // Кэш для URL медиа файлов
    const mediaUrlCache = useRef(new Map());
    const chunksRef = useRef([]);

    // Определяем текущего пользователя и права доступа
    useEffect(() => {
        const demoRole = localStorage.getItem('demoRole');
        let author = 'Пользователь';

        if (demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin') {
            author = 'Админ';
        } else if (demoRole === 'user1') {
            author = 'Пользователь 1';
        } else if (demoRole === 'user2') {
            author = 'Пользователь 2';
        }

        setCurrentUserAuthor(author);

        // Настройка прав переключения комнат
        if (demoRole === 'user_1') {
            setCurrentRoom('user1');
            setCanSwitch(false);
        } else if (demoRole === 'user_2') {
            setCanSwitch(true);
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
            const response = await chatApi.getMessages(50);

            // Преобразуем сообщения с медиа
            const msgs = await Promise.all(response.data.map(async (msg) => {
                if (msg.mediaId) {
                    // ПолучаемPresigned URL для медиа
                    try {
                        const mediaResponse = await mediaApi.getStreamUrl(msg.mediaId);
                        return {
                            ...msg,
                            mediaUrl: mediaResponse.data.url,
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
            // Fallback к localStorage
            loadMessagesFromStorage(room);
        }
    }, []);

    // Загрузка из localStorage
    const loadMessagesFromStorage = useCallback((room) => {
        const key = `${storageKeyPrefix}_${room}`;
        const savedMessages = localStorage.getItem(key);

        if (savedMessages) {
            try {
                setMessages(JSON.parse(savedMessages));
            } catch (e) {
                console.warn('Corrupted localStorage data:', e);
                setMessages([]);
            }
        } else {
            const initialMessage = {
                id: 1,
                author: 'Система',
                content: 'Добро пожаловать в чат!',
                timestamp: new Date().toISOString()
            };
            setMessages([initialMessage]);
            localStorage.setItem(key, JSON.stringify([initialMessage]));
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

        const tempId = `temp_${Date.now()}`;
        const tempMessage = {
            id: tempId,
            author: currentUserAuthor,
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
                // Заменяем временное сообщение реальным
                setMessages(prev =>
                    prev.map(msg =>
                        msg.id === tempId
                            ? { ...response.data, pending: false }
                            : msg
                    )
                );
                return response.data;
            } catch (error) {
                console.error('Failed to send message:', error);
                // Удаляем временное сообщение при ошибке
                setMessages(prev => prev.filter(msg => msg.id !== tempId));
                throw error;
            }
        } else {
            // LocalStorage режим
            const message = {
                id: Date.now(),
                author: currentUserAuthor,
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
                const saved = localStorage.getItem(key);
                existing = saved ? JSON.parse(saved) : [];
            } catch (e) {
                existing = [];
            }
            localStorage.setItem(key, JSON.stringify([...existing, message]));

            return message;
        }
    }, [currentUserAuthor, currentRoom, useApi, storageKeyPrefix]);

    // Загрузка файла на сервер
    const uploadMedia = useCallback(async (file) => {
        setUploading(true);

        try {
            const type = getMediaType(file.type);
            const response = await mediaApi.upload(file, type);
            const mediaFile = response.data;

            // Получаем presigned URL для предпросмотра
            let mediaUrl = null;
            try {
                const urlResponse = await mediaApi.getStreamUrl(mediaFile.id);
                mediaUrl = urlResponse.data.url;
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

    // Старая функция для совместимости (localStorage)
    const uploadFile = useCallback((file) => {
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
                    const saved = localStorage.getItem(key);
                    existing = saved ? JSON.parse(saved) : [];
                } catch (e) {
                    existing = [];
                }
                localStorage.setItem(key, JSON.stringify([...existing, message]));

                resolve(message);
            };

            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(file);
        });
    }, [currentUserAuthor, currentRoom, storageKeyPrefix]);

    // Редактирование сообщения
    const editMessage = useCallback((msgId, newContent) => {
        if (!newContent.trim()) return;

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
                localStorage.setItem(key, JSON.stringify(updated));
            }
            return updated;
        });
    }, [currentRoom, useApi, storageKeyPrefix]);

    // Удаление сообщения
    const deleteMessage = useCallback((msgId) => {
        if (!window.confirm('Удалить это сообщение?')) return false;

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
                localStorage.setItem(key, JSON.stringify(updated));
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
                            mediaUrl = urlResponse.data.url;
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
                            localStorage.setItem(key, JSON.stringify(updated));
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
                const url = response.data.url;
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
        loadMessages
    };
};

export default useChat;