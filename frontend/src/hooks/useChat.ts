import { useState, useEffect, useCallback, useRef } from 'react';
import { chatApi, mediaApi } from '../services/api';
import chatLogger from '../utils/chatLogger';
import { ChatMessage } from '../types/ChatRoom';

interface UseChatConfig {
  storageKeyPrefix?: string;
  canSwitchRooms?: boolean;
  availableRooms?: Array<{ id: string; label: string }>;
  useApi?: boolean;
}

interface UseChatReturn {
  // Состояние
  messages: ChatMessage[];
  loading: boolean;
  currentRoom: string;
  canSwitch: boolean;
  currentUserAuthor: string;
  isRecording: boolean;
  recordingTime: number;
  uploading: boolean;
  error: string | null;

  // Действия
  changeRoom: (room: string) => void;
  sendMessage: (content: string, mediaId?: string | null) => Promise<ChatMessage>;
  editMessage: (msgId: string, newContent: string) => void;
  deleteMessage: (msgId: string) => boolean;
  uploadFile: (file: File) => Promise<ChatMessage>;
  uploadMedia: (file: File) => Promise<{
    mediaId: string;
    mediaType: string;
    mediaUrl: string | null;
    fileName: string;
  }>;
  startRecording: () => Promise<void>;
  stopRecording: () => void;
  formatRecordingTime: (seconds: number) => string;
  getMediaUrl: (mediaId: string) => Promise<string | null>;
  loadMessages: (room: string) => void;
  clearError: () => void;
}

export const useChat = (chatRoom: string, config: UseChatConfig = {}): UseChatReturn => {
  const {
    storageKeyPrefix = 'chatMessages',
    canSwitchRooms = true,
    availableRooms = [],
    useApi = true
  } = config;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentRoom, setCurrentRoom] = useState(chatRoom || 'default');
  const [canSwitch, setCanSwitch] = useState(canSwitchRooms);
  const [currentUserAuthor, setCurrentUserAuthor] = useState('');
  const [currentUserAvatar, setCurrentUserAvatar] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [mediaRecorder, setMediaRecorder] = useState<MediaRecorder | null>(null);
  const [recordingTime, setRecordingTime] = useState(0);
  const [recordingInterval, setRecordingInterval] = useState<NodeJS.Timeout | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Кэш для URL медиа файлов
  const mediaUrlCache = useRef(new Map<string, string>());
  const chunksRef = useRef<Blob[]>([]);

  // Определяем текущего пользователя и права доступа
  useEffect(() => {
    const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
        ? sessionStorage.getItem('demoRole')
        : null;
    let author = 'Пользователь';
    let avatarUrl = '';

    if (demoRole === 'MODERATOR_ADMIN' || demoRole === 'DEVELOPER' || demoRole === 'SUPER_ADMIN' || demoRole === 'VYACHESLAV') {
      author = 'Админ';
      avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin&backgroundColor=dc3545&hairColor=000000&accessories=sunglasses';
    } else if (demoRole === 'USER_0') {
      author = 'Пользователь 1';
      avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user0&backgroundColor=4a90e2&hairColor=2c1b18&accessories=round&facialHair=beardLight';
    } else if (demoRole === 'USER_1') {
      author = 'Пользователь 2';
      avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1&backgroundColor=28a745&hairColor=2c1b18&accessories=prescription02';
    } else {
      avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + (demoRole || 'user') + '&backgroundColor=6c757d&hairColor=2c1b18';
    }

    setCurrentUserAuthor(author);
    setCurrentUserAvatar(avatarUrl);

    // Настройка прав переключения комнат
    if (chatRoom === 'user1' || chatRoom === 'user2') {
      if (demoRole === 'USER_0') {
        setCurrentRoom('user1');
        setCanSwitch(false);
      } else if (demoRole === 'USER_1') {
        setCurrentRoom('user1');
        setCanSwitch(false);
      }
    } else {
      setCurrentRoom(chatRoom);
    }
  }, []);

  // Определение типа медиа по MIME
  const getMediaType = (mimeType: string | undefined): string => {
    if (mimeType?.startsWith('image/')) return 'image';
    if (mimeType?.startsWith('video/')) return 'video';
    if (mimeType?.startsWith('audio/')) return 'audio';
    return 'file';
  };

  // Загрузка сообщений с API
  const loadMessagesFromApi = useCallback(async (room: string) => {
    try {
      setError(null);
      const response = await chatApi.getMessages(room, 0, 50);

      // Преобразуем сообщения с медиа
      const msgs = await Promise.all(response.data.map(async (msg: any) => {
        if (msg.mediaId) {
          try {
            const mediaResponse = await mediaApi.getStreamUrl(msg.mediaId);
            return {
              ...msg,
              mediaUrl: mediaResponse.data.url,
              mediaType: msg.mediaType || 'file'
            } as ChatMessage;
          } catch (e) {
            console.error('Failed to load media URL:', e);
            return msg as ChatMessage;
          }
        }
        return msg as ChatMessage;
      }));

      setMessages(msgs);
      setLoading(false);
    } catch (error) {
      console.error('Failed to load messages from API:', error);
      setError('Не удалось загрузить сообщения. Проверьте подключение к интернету.');
      // Fallback к sessionStorage
      loadMessagesFromStorage(room);
    }
  }, []);

  // Загрузка из sessionStorage
  const loadMessagesFromStorage = useCallback((room: string) => {
    const key = `${storageKeyPrefix}_${room}`;
    const savedMessages = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem(key)
        : null;

    const FORCE_TEST_MESSAGES = true;

    if (savedMessages && !FORCE_TEST_MESSAGES) {
      try {
        let messages = JSON.parse(savedMessages) as ChatMessage[];

        // Добавляем avatarUrl к старым сообщениям, если его нет
        messages = messages.map(msg => {
          if (!msg.avatarUrl) {
            let avatarSeed = msg.author.toLowerCase().replace(/\s+/g, '');
            let backgroundColor = '6c757d';

            if (msg.author === 'Админ' || msg.author.includes('Админ')) {
              backgroundColor = 'dc3545';
            } else if (msg.author === 'Система') {
              backgroundColor = '0d6efd';
            } else if (msg.author.includes('1')) {
              backgroundColor = '4a90e2';
            } else if (msg.author.includes('2')) {
              backgroundColor = '28a745';
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
        setError('Ошибка загрузки данных из локального хранилища');
        setMessages([]);
      }
    } else {
      // Тестовая переписка для разных чатов
      let initialMessages: ChatMessage[] = [];

      if (room.includes('user1') || room.includes('general')) {
        initialMessages = [
          {
            id: '1',
            author: 'Система',
            avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
            content: 'Добро пожаловать в общий чат! Здесь вы можете задавать вопросы и общаться с другими пользователями.',
            timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString(),
            chatType: 'general',
            userId: 'system',
            userName: 'Система',
            isRead: true,
            createdAt: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
          },
          {
            id: '2',
            author: 'Алексей',
            avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=alexey&backgroundColor=4a90e2&hairColor=2c1b18&facialHair=beardLight',
            content: 'Привет всем! Я новичок в спорте, с чего лучше начать?',
            timestamp: new Date(Date.now() - 3600000 * 24).toISOString(),
            chatType: 'general',
            userId: 'alexey',
            userName: 'Алексей',
            isRead: true,
            createdAt: new Date(Date.now() - 3600000 * 24).toISOString()
          }
        ];
      } else if (room.includes('user2')) {
        initialMessages = [
          {
            id: '1',
            author: 'Система',
            avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
            content: 'Добро пожаловать в чат продвинутых! Здесь обсуждаем продвинутые техники и программы тренировок.',
            timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString(),
            chatType: 'admin',
            userId: 'system',
            userName: 'Система',
            isRead: true,
            createdAt: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
          }
        ];
      } else if (room.includes('nutrition')) {
        initialMessages = [
          {
            id: '1',
            author: 'Система',
            avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
            content: 'Добро пожаловать в чат по питанию! Здесь обсуждаем диеты, БЖУ, добавки и здоровое питание.',
            timestamp: new Date(Date.now() - 3600000 * 24 * 2).toISOString(),
            chatType: 'nutrition',
            userId: 'system',
            userName: 'Система',
            isRead: true,
            createdAt: new Date(Date.now() - 3600000 * 24 * 2).toISOString()
          }
        ];
      } else {
        initialMessages = [
          {
            id: '1',
            author: 'Система',
            avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=system&backgroundColor=0d6efd&hairColor=000000&accessories=prescription01',
            content: 'Добро пожаловать в чат!',
            timestamp: new Date().toISOString(),
            chatType: 'general',
            userId: 'system',
            userName: 'Система',
            isRead: true,
            createdAt: new Date().toISOString()
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
  const loadMessages = useCallback((room: string) => {
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
  const changeRoom = useCallback((room: string) => {
    if (!canSwitch) return;
    setCurrentRoom(room);
  }, [canSwitch]);

  // Отправка сообщения
  const sendMessage = useCallback(async (content: string, mediaId: string | null = null): Promise<ChatMessage> => {
    if (!content?.trim() && !mediaId) {
      setError('Сообщение не может быть пустым');
      throw new Error('Message content or media is required');
    }

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
    const tempMessage: ChatMessage = {
      id: tempId,
      author: currentUserAuthor,
      avatarUrl: currentUserAvatar,
      content: content?.trim() || '',
      mediaId,
      timestamp: new Date().toISOString(),
      pending: true,
      chatType: currentRoom.includes('nutrition') ? 'nutrition' : 'general',
      userId: 'current',
      userName: currentUserAuthor,
      isRead: false,
      createdAt: new Date().toISOString()
    };

    // Добавляем временное сообщение
    setMessages(prev => [...prev, tempMessage]);

    if (useApi) {
      try {
        const response = await chatApi.sendMessage(currentRoom, { content, mediaId });
        // Заменяем временное сообщение реальным, сохраняя avatarUrl
        const realMessage: ChatMessage = {
          ...response.data,
          avatarUrl: currentUserAvatar,
          pending: false,
          author: currentUserAuthor,
          timestamp: response.data.createdAt
        };

        setMessages(prev =>
          prev.map(msg =>
            msg.id === tempId ? realMessage : msg
          )
        );
        setError(null);
        return realMessage;
      } catch (error) {
        console.error('Failed to send message:', error);
        // Удаляем временное сообщение при ошибке
        setMessages(prev => prev.filter(msg => msg.id !== tempId));
        setError('Не удалось отправить сообщение. Проверьте подключение к интернету.');
        throw error;
      }
    } else {
      // LocalStorage режим
      const message: ChatMessage = {
        id: Date.now().toString(),
        author: currentUserAuthor,
        avatarUrl: currentUserAvatar,
        content: content.trim(),
        mediaId,
        timestamp: new Date().toISOString(),
        chatType: currentRoom.includes('nutrition') ? 'nutrition' : 'general',
        userId: 'current',
        userName: currentUserAuthor,
        isRead: false,
        createdAt: new Date().toISOString()
      };

      setMessages(prev =>
        prev.map(msg =>
          msg.id === tempId ? { ...message, pending: false } : msg
        )
      );

      const key = `${storageKeyPrefix}_${currentRoom}`;
      let existing: ChatMessage[];
      try {
        const saved = process.env.NODE_ENV === 'development'
            ? sessionStorage.getItem(key)
            : null;
        existing = saved ? JSON.parse(saved) : [];
      } catch (e) {
        existing = [];
        setError('Ошибка сохранения сообщения в локальное хранилище');
      }
      if (process.env.NODE_ENV === 'development') {
          sessionStorage.setItem(key, JSON.stringify([...existing, message]));
      }

      return message;
    }
  }, [currentUserAuthor, currentUserAvatar, currentRoom, useApi, storageKeyPrefix]);

  // Загрузка файла на сервер
  const uploadMedia = useCallback(async (file: File) => {
    setUploading(true);
    setError(null);

    // Логирование загрузки файла
    const demoRole = process.env.NODE_ENV === 'development'
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
      let mediaUrl: string | null = null;
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
      setError('Не удалось загрузить файл. Проверьте подключение к интернету и размер файла.');
      throw error;
    } finally {
      setUploading(false);
    }
  }, [currentRoom]);

  // Старая функция для совместимости (sessionStorage)
  const uploadFile = useCallback((file: File): Promise<ChatMessage> => {
    // Логирование загрузки файла (sessionStorage режим)
    const demoRole = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
        : 'UNKNOWN';
    const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                   currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

    chatLogger.logFileUpload(chatType, currentRoom, demoRole, file);

    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = (event) => {
        const message: ChatMessage = {
          id: Date.now().toString(),
          author: currentUserAuthor,
          content: file.name,
          fileUrl: event.target.result as string,
          fileType: file.type,
          fileName: file.name,
          timestamp: new Date().toISOString(),
          type: getMediaType(file.type),
          chatType: currentRoom.includes('nutrition') ? 'nutrition' : 'general',
          userId: 'current',
          userName: currentUserAuthor,
          isRead: false,
          createdAt: new Date().toISOString()
        };

        setMessages(prev => [...prev, message]);

        const key = `${storageKeyPrefix}_${currentRoom}`;
        let existing: ChatMessage[];
        try {
          const saved = process.env.NODE_ENV === 'development'
            ? sessionStorage.getItem(key)
            : null;
          existing = saved ? JSON.parse(saved) : [];
        } catch (e) {
          existing = [];
          setError('Ошибка сохранения файла в локальное хранилище');
        }
        if (process.env.NODE_ENV === 'development') {
          sessionStorage.setItem(key, JSON.stringify([...existing, message]));
      }

        resolve(message);
      };

      reader.onerror = () => {
        setError('Ошибка чтения файла');
        reject(reader.error);
      };
      reader.readAsDataURL(file);
    });
  }, [currentUserAuthor, currentRoom, storageKeyPrefix]);

  // Редактирование сообщения
  const editMessage = useCallback((msgId: string, newContent: string) => {
    if (!newContent.trim()) return;

    // Логирование редактирования
    const demoRole = process.env.NODE_ENV === 'development'
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
  }, [currentRoom, useApi, storageKeyPrefix, messages]);

  // Удаление сообщения
  const deleteMessage = useCallback((msgId: string): boolean => {
    if (!window.confirm('Удалить это сообщение?')) return false;

    // Логирование удаления
    const demoRole = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem('demoRole') || 'UNKNOWN'
        : 'UNKNOWN';
    const chatType = currentRoom.includes('nutrition') ? 'NutritionChat' :
                   currentRoom.includes('user') ? 'GeneralChat' : 'UnknownChat';

    chatLogger.logMessageDelete(chatType, currentRoom, demoRole, msgId);

    if (useApi) {
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
            let mediaUrl: string | null = null;
            try {
              const urlResponse = await mediaApi.getStreamUrl(mediaFile.id);
              mediaUrl = urlResponse.data.url;
            } catch (e) {
              console.warn('Could not get audio URL:', e);
            }

            // Отправляем сообщение с медиа
            await sendMessage('Голосовое сообщение', mediaFile.id);

            // Добавляем в локальный кэш
            mediaUrlCache.current.set(mediaFile.id, mediaUrl || '');
          } catch (error) {
            console.error('Failed to upload voice message:', error);
            setError('Не удалось загрузить голосовое сообщение');
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
            const message: ChatMessage = {
              id: Date.now().toString(),
              author: currentUserAuthor,
              content: 'Голосовое сообщение',
              fileUrl: event.target.result as string,
              fileType: 'audio/webm',
              timestamp: new Date().toISOString(),
              type: 'voice',
              chatType: currentRoom.includes('nutrition') ? 'nutrition' : 'general',
              userId: 'current',
              userName: currentUserAuthor,
              isRead: false,
              createdAt: new Date().toISOString()
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
          reader.onerror = () => {
            setError('Ошибка чтения голосового сообщения');
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
      setError('Не удалось получить доступ к микрофону. Проверьте разрешения браузера.');
      alert('Не удалось получить доступ к микрофону');
    }
  }, [currentUserAuthor, currentRoom, useApi, sendMessage, storageKeyPrefix, recordingTime]);

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
  const getMediaUrl = useCallback(async (mediaId: string): Promise<string | null> => {
    if (mediaUrlCache.current.has(mediaId)) {
      return mediaUrlCache.current.get(mediaId) || null;
    }

    if (useApi) {
      try {
        const response = await mediaApi.getStreamUrl(mediaId);
        const url = response.data.url;
        mediaUrlCache.current.set(mediaId, url);
        return url;
      } catch (error) {
        console.error('Failed to get media URL:', error);
        setError('Не удалось загрузить медиа файл');
        return null;
      }
    }

    return null;
  }, [useApi]);

  // Форматирование времени
  const formatRecordingTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // Очистка ошибки
  const clearError = useCallback(() => {
    setError(null);
  }, []);

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
    uploadMedia,
    startRecording,
    stopRecording,
    formatRecordingTime,
    getMediaUrl,
    loadMessages,
    clearError
  };
};

export default useChat;