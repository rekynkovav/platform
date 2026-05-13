import { useState, useEffect, useCallback } from 'react';
import { chatApi, mediaApi } from '../../services/api';

export const useChat = (initialRoom = 'general', config = {}) => {
    const {
        canSwitchRooms = true,
        useApi = true
    } = config;

    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentRoom, setCurrentRoom] = useState(initialRoom);
    const [currentUserAuthor, setCurrentUserAuthor] = useState('Пользователь');
    const [isRecording, setIsRecording] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [uploading, setUploading] = useState(false);

    // Определение автора по роли
    useEffect(() => {
        const demoRole = localStorage.getItem('demoRole');
        let author = 'Пользователь';

        if (['MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER'].includes(demoRole)) {
            author = 'Админ';
        } else if (demoRole === 'USER_1') {
            author = 'Пользователь 1';
        } else if (demoRole === 'USER_2') {
            author = 'Пользователь 2';
        }

        setCurrentUserAuthor(author);
    }, []);

    // Загрузка сообщений
    const loadMessages = useCallback(async () => {
        if (!useApi) {
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            const response = await chatApi.getGeneralMessages(100);
            setMessages(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load messages', err);
            setMessages([]);
        } finally {
            setLoading(false);
        }
    }, [useApi]);

    useEffect(() => {
        loadMessages();
    }, [currentRoom, loadMessages]);

    // Отправка сообщения
    const sendMessage = useCallback(async (content, mediaId = null) => {
        if (!content?.trim() && !mediaId) return;

        const tempMessage = {
            id: `temp_${Date.now()}`,
            author: currentUserAuthor,
            content: content?.trim() || '',
            mediaId,
            timestamp: new Date().toISOString(),
            pending: true
        };

        setMessages(prev => [...prev, tempMessage]);

        try {
            const response = await chatApi.sendToGeneralChat(content, mediaId);
            setMessages(prev =>
                prev.map(msg =>
                    msg.id === tempMessage.id ? { ...response.data, pending: false } : msg
                )
            );
        } catch (error) {
            console.error('Failed to send message', error);
            setMessages(prev => prev.filter(msg => msg.id !== tempMessage.id));
        }
    }, [currentUserAuthor]);

    // Загрузка медиа
    const uploadMedia = useCallback(async (file) => {
        setUploading(true);
        try {
            const type = file.type.startsWith('image/') ? 'image' :
                        file.type.startsWith('video/') ? 'video' :
                        file.type.startsWith('audio/') ? 'audio' : 'file';

            const response = await mediaApi.upload(file, type);
            return response.data;
        } catch (error) {
            console.error('Upload failed', error);
            throw error;
        } finally {
            setUploading(false);
        }
    }, []);

    // Заглушки для пока не реализованных функций
    const editMessage = useCallback(() => {
        console.warn('Редактирование сообщений через API пока не реализовано');
    }, []);

    const deleteMessage = useCallback(() => {
        console.warn('Удаление сообщений через API пока не реализовано');
    }, []);

    const startRecording = useCallback(() => {
        setIsRecording(true);
        setRecordingTime(0);
    }, []);

    const stopRecording = useCallback(() => {
        setIsRecording(false);
        setRecordingTime(0);
    }, []);

    return {
        messages,
        loading,
        currentRoom,
        currentUserAuthor,
        isRecording,
        recordingTime,
        uploading,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadMedia,
        startRecording,
        stopRecording
    };
};

export default useChat;