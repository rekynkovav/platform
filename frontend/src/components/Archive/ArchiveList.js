// components/Archive/ArchiveList.js (Трансляции)
import React, { useState, useEffect } from 'react';
import './ArchiveList.css';
import '../common/Modal.css';
import { usePermissions } from '../../hooks/usePermissions';
import { getUserId } from '../../keycloak';
import { mediaApi, broadcastApi, demoApi } from '../../services/api';
import LiveBroadcastView from '../live/LiveBroadcastViewSimple';
import EnhancedLiveBroadcastView from '../live/EnhancedLiveBroadcastView';
import NetworkSpeedTest from '../live/NetworkSpeedTest';
import DeviceTest from '../live/DeviceTest';

/**
 * ArchiveList — broadcast management and live streaming dashboard.
 *
 * Manages the full lifecycle of broadcasts: listing existing broadcasts, creating
 * new ones (audio or video), editing, deleting, and starting live streams.
 * Integrates pre-broadcast diagnostics (network speed test, device test) and
 * displays live broadcast views (simple and enhanced). Supports admin CRUD
 * operations with confirmation modals.
 *
 * @returns {JSX.Element} The broadcasts list with create/edit forms, diagnostics, and live views
 */
const ArchiveList = () => {
    const [broadcasts, setBroadcasts] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [streamType, setStreamType] = useState('audio'); // 'audio' или 'video'
    const [newBroadcast, setNewBroadcast] = useState({
        title: '',
        description: '',
        date: '',
        time: '',
        type: 'audio'
    });

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [broadcastToDelete, setBroadcastToDelete] = useState(null);

    // Состояния для редактирования
    const [showEditModal, setShowEditModal] = useState(false);
    const [editingBroadcast, setEditingBroadcast] = useState(null);
    const [editStreamType, setEditStreamType] = useState('audio');

    // Состояния для live трансляции
    const [activeLiveBroadcast, setActiveLiveBroadcast] = useState(null);
    const [showLiveView, setShowLiveView] = useState(false);

    // Состояния для таймеров и кнопки "Буду!"
    const [timers, setTimers] = useState({});
    const [willAttendCounts, setWillAttendCounts] = useState({});

    // Состояние для модального окна настроек
    const [showSettingsModal, setShowSettingsModal] = useState(false);
    const [broadcastForSettings, setBroadcastForSettings] = useState(null);
    const [showSpeedTest, setShowSpeedTest] = useState(false);
    const [speedTestSettings, setSpeedTestSettings] = useState(null);
    const [showDeviceTest, setShowDeviceTest] = useState(false);
    const [deviceSettings, setDeviceSettings] = useState(null);

    // Состояние для меню подтверждения завершения трансляции
    const [showEndConfirm, setShowEndConfirm] = useState(false);

    const { hasLevel, hasRole, hasAnyRole, isConsultant } = usePermissions();
    const isAdmin = hasLevel(6);
    const canEdit = isAdmin || isConsultant();
    const canDownload = isAdmin;

    // Состояние для загрузки файлов
    const [downloadingFileId, setDownloadingFileId] = useState(null);

    // Состояние для ошибок
    const [error, setError] = useState(null);

    // Отладка

    useEffect(() => {
        loadBroadcasts();
        loadWillAttendCounts();
    }, []);

    // Загрузка счетчиков "Буду!"
    const loadWillAttendCounts = async () => {
        try {
            const counts = await demoApi.getWillAttendCounts();
            setWillAttendCounts(counts);
        } catch (err) {
            // Fallback для разработки
            if (process.env.NODE_ENV === 'development') {
                const saved = sessionStorage.getItem('willAttendCounts');
                if (saved) {
                    setWillAttendCounts(JSON.parse(saved));
                } else {
                    setWillAttendCounts({});
                }
            }
        }
    };

    // Сохранение счетчиков "Буду!"
    const saveWillAttendCounts = async (counts) => {
        setWillAttendCounts(counts);
        try {
            await demoApi.saveWillAttendCounts(counts);
        } catch (err) {
            // Fallback для разработки
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem('willAttendCounts', JSON.stringify(counts));
            }
        }
    };

    // Обработчик кнопки "Буду!"
    const handleWillAttend = async (broadcastId) => {
        // Проверяем, что трансляция еще запланирована
        const broadcast = broadcasts.find(b => b.id === broadcastId);
        if (!broadcast || broadcast.status !== 'scheduled') {
            showMessage('Откликнуться можно только на запланированные трансляции');
            return;
        }

        const userId = getUserId() || 'USER_0';
        const key = `${broadcastId}_${userId}`;

        try {
            // Проверяем через API, откликнулся ли пользователь
            const hasAttended = await demoApi.hasUserAttended(broadcastId, userId);

            if (hasAttended) {
                showMessage('Вы уже откликнулись на эту трансляцию!');
                return;
            }

            // Сохраняем отклик через API
            await demoApi.markUserAttended(broadcastId, userId);

            const newCounts = { ...willAttendCounts };
            newCounts[broadcastId] = (newCounts[broadcastId] || 0) + 1;
            saveWillAttendCounts(newCounts);

            showMessage('Спасибо! Вы откликнулись на трансляцию.', 'success');
        } catch (err) {
            // Fallback для разработки
            if (process.env.NODE_ENV === 'development') {
                const hasAttended = sessionStorage.getItem(key);
                if (hasAttended) {
                    showMessage('Вы уже откликнулись на эту трансляцию!');
                    return;
                }

                const newCounts = { ...willAttendCounts };
                newCounts[broadcastId] = (newCounts[broadcastId] || 0) + 1;
                saveWillAttendCounts(newCounts);

                sessionStorage.setItem(key, 'true');
                showMessage('Спасибо! Вы откликнулись на трансляцию.', 'success');
            }
        }
    };

    // Функция для расчета времени до начала трансляции
    const calculateTimeUntilBroadcast = (broadcast) => {
        try {
            // Парсим дату и время
            const [year, month, day] = broadcast.date.split('-').map(Number);
            const [hours, minutes] = broadcast.time.split(':').map(Number);

            const broadcastDateTime = new Date(year, month - 1, day, hours, minutes);
            const now = new Date();
            const diffMs = broadcastDateTime - now;

            if (diffMs <= 0) {
                return { hours: 0, minutes: 0, seconds: 0, isPast: true };
            }

            const hoursLeft = Math.floor(diffMs / (1000 * 60 * 60));
            const minutesLeft = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
            const secondsLeft = Math.floor((diffMs % (1000 * 60)) / 1000);

            return {
                hours: hoursLeft,
                minutes: minutesLeft,
                seconds: secondsLeft,
                isPast: false
            };
        } catch (error) {
            return { hours: 0, minutes: 0, seconds: 0, isPast: true };
        }
    };

    // Функция для проверки доступности трансляции
    const isBroadcastAvailable = (broadcast) => {
        if (broadcast.status === 'live') {
            return true; // Live трансляции всегда доступны
        }

        if (broadcast.status === 'ended') {
            return true; // Завершенные трансляции доступны в архиве
        }

        if (broadcast.status === 'scheduled') {
            // Запланированные трансляции доступны только для просмотра информации
            return true;
        }

        return false;
    };

    // Функция для проверки прав на редактирование
    const canEditBroadcast = (broadcast) => {
        const currentUserId = getUserId();

        // SUPER_ADMIN может редактировать все
        if (hasAnyRole(['SUPER_ADMIN', 'VYACHESLAV'])) {
            if (broadcast.status === 'live') return false; // Нельзя редактировать live трансляцию
            if (broadcast.status === 'ended') return false; // Нельзя редактировать завершенную трансляцию
            return true;
        }

        // Создатель может редактировать свои трансляции
        if (broadcast.ownerId === currentUserId) {
            if (broadcast.status === 'live') return false; // Нельзя редактировать live трансляцию
            if (broadcast.status === 'ended') return false; // Нельзя редактировать завершенную трансляцию
            return true;
        }

        return false;
    };

    // Функция для проверки прав на кнопки "Настройки" и "Начать Live"
    const canManageBroadcast = (broadcast) => {
        const currentUserId = getUserId();

        // SUPER_ADMIN может управлять всеми трансляциями
        if (hasAnyRole(['SUPER_ADMIN', 'VYACHESLAV'])) {
            return true;
        }

        // Создатель может управлять своими трансляциями
        if (broadcast.ownerId === currentUserId) {
            return true;
        }

        return false;
    };

    // Функция для форматирования таймера
    const formatTimer = (time) => {
        if (time.isPast) return '00:00:00';
        return `${time.hours.toString().padStart(2, '0')}:${time.minutes.toString().padStart(2, '0')}:${time.seconds.toString().padStart(2, '0')}`;
    };

    // Обновление таймеров каждую секунду
    useEffect(() => {
        const interval = setInterval(() => {
            const newTimers = {};
            broadcasts.forEach(broadcast => {
                if (broadcast.status === 'scheduled') {
                    newTimers[broadcast.id] = calculateTimeUntilBroadcast(broadcast);
                }
            });
            setTimers(newTimers);
        }, 1000);

        return () => clearInterval(interval);
    }, [broadcasts]);

    // Проверка, прошла ли трансляция
    const isBroadcastPast = (broadcast) => {
        const broadcastDateTime = new Date(`${broadcast.date}T${broadcast.time}`);
        const now = new Date();
        return broadcastDateTime < now;
    };

    // Перенос прошедших трансляций в Dashboard
    const moveCompletedBroadcastsToDashboard = (broadcastsList) => {
        const dashboardData = process.env.NODE_ENV === 'development'
            ? JSON.parse(sessionStorage.getItem('dashboardMenuData') || '{}')
            : {};
        const currentBroadcasts = dashboardData['broadcasts'] || [];

        broadcastsList.forEach(broadcast => {
            if (isBroadcastPast(broadcast) && broadcast.status === 'scheduled') {
                // Обновляем статус трансляции
                broadcast.status = 'completed';

                // Проверяем, не добавлена ли уже эта трансляция в Dashboard
                const alreadyExists = currentBroadcasts.some(item => item.broadcastId === broadcast.id);

                if (!alreadyExists) {
                    // Добавляем в Dashboard
                    const dashboardItem = {
                        id: Date.now() + Math.random(),
                        broadcastId: broadcast.id,
                        title: broadcast.title,
                        description: broadcast.description,
                        mediaUrl: broadcast.mediaUrl || '',
                        mediaType: broadcast.type,
                        createdAt: new Date().toISOString()
                    };
                    currentBroadcasts.push(dashboardItem);
                }
            }
        });

        dashboardData['broadcasts'] = currentBroadcasts;
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('dashboardMenuData', JSON.stringify(dashboardData));
        }
    };

    const loadBroadcasts = () => {
        const saved = process.env.NODE_ENV === 'development'
            ? sessionStorage.getItem('broadcasts')
            : null;
        if (saved) {
            const broadcastsList = JSON.parse(saved);

            // Проверяем и переносим прошедшие трансляции
            moveCompletedBroadcastsToDashboard(broadcastsList);

            // Обновляем статусы
            const updatedBroadcasts = broadcastsList.map(b => ({
                ...b,
                status: isBroadcastPast(b) ? 'completed' : b.status
            }));

            setBroadcasts(updatedBroadcasts);
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem('broadcasts', JSON.stringify(updatedBroadcasts));
            }
        } else {
            const initial = [
                {
                    id: 1,
                    title: 'Прямой эфир - Основы питания',
                    description: 'Обсуждение принципов здорового питания',
                    date: '2026-04-10',
                    time: '18:00',
                    type: 'video',
                    status: 'scheduled',
                    fileId: 'demo-video-001', // ID файла для скачивания/просмотра
                    mediaUrl: '/api/media/stream/demo-video-001', // URL для стриминга
                    ownerId: 'SUPER_ADMIN'
                },
                {
                    id: 2,
                    title: 'Технический вебинар',
                    description: 'Новые возможности платформы для разработчиков',
                    date: '2026-04-20',
                    time: '18:00',
                    type: 'video',
                    status: 'scheduled',
                    fileId: 'demo-video-002',
                    mediaUrl: '/api/media/stream/demo-video-002',
                    ownerId: 'SUPER_ADMIN'
                },
                {
                    id: 3,
                    title: 'Аудио конференция',
                    description: 'Обсуждение маркетинговой стратегии',
                    date: '2026-04-18',
                    time: '16:00',
                    type: 'audio',
                    status: 'scheduled',
                    fileId: 'demo-audio-001',
                    mediaUrl: '/api/media/stream/demo-audio-001',
                    ownerId: 'MODERATOR_ADMIN'
                },
                {
                    id: 4,
                    title: 'Прямой эфир Q&A',
                    description: 'Ответы на вопросы пользователей',
                    date: '2026-04-22',
                    time: '20:00',
                    type: 'video',
                    status: 'scheduled',
                    fileId: 'demo-video-003',
                    mediaUrl: '/api/media/stream/demo-video-003',
                    ownerId: 'USER_0'
                }
            ];
            setBroadcasts(initial);
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem('broadcasts', JSON.stringify(initial));
            }
        }
    };

    // Обработчик скачивания трансляции
    const handleDownload = async (broadcast) => {
        if (!canDownload) {
            alert('Только SUPER_ADMIN и VYACHESLAV могут скачивать трансляции');
            return;
        }

        if (!broadcast.fileId) {
            alert('У этой трансляции нет медиафайла для скачивания');
            return;
        }

        try {
            setDownloadingFileId(broadcast.id);

            const response = await mediaApi.download(broadcast.fileId);

            // Создаем URL для скачивания
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;

            // Определяем имя файла
            const fileName = broadcast.title
                ? `${broadcast.title.replace(/[^a-z0-9]/gi, '_')}.${broadcast.type === 'audio' ? 'mp3' : 'mp4'}`
                : `translation_${broadcast.id}.${broadcast.type === 'audio' ? 'mp3' : 'mp4'}`;

            link.setAttribute('download', fileName);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            alert(`Трансляция "${broadcast.title}" успешно скачана!`);

        } catch (error) {
            alert(`Ошибка скачивания: ${error.response?.data?.message || error.message || 'Неизвестная ошибка'}`);
        } finally {
            setDownloadingFileId(null);
        }
    };

    // Обработчик просмотра трансляции
    const handleWatch = async (broadcast) => {
        if (!broadcast.fileId) {
            alert('У этой трансляции нет медиафайла для просмотра');
            return;
        }

        try {
            const response = await mediaApi.getStreamUrl(broadcast.fileId);
            const streamUrl = response.data.url;

            // Открываем в новом окне или встроенном плеере
            window.open(streamUrl, '_blank', 'noopener,noreferrer');

        } catch (error) {
            alert(`Ошибка: ${error.response?.data?.message || error.message || 'Неизвестная ошибка'}`);
        }
    };

    const handleCreateBroadcast = () => {
        if (!isAdmin) {
            alert('Только администратор может создавать трансляции');
            return;
        }

        if (!newBroadcast.title || !newBroadcast.date || !newBroadcast.time) {
            alert('Заполните название, дату и время');
            return;
        }

        const broadcast = {
            id: Date.now(),
            ...newBroadcast,
            type: streamType,
            status: 'scheduled',
            fileId: `translation-${Date.now()}`, // Генерируем уникальный ID файла
            mediaUrl: `/api/media/stream/translation-${Date.now()}`, // URL для стриминга
            createdAt: new Date().toISOString(),
            ownerId: getUserId() // Привязываем к текущему пользователю
        };

        const updated = [...broadcasts, broadcast];
        setBroadcasts(updated);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }

        // Добавляем в события
        const events = process.env.NODE_ENV === 'development'
            ? JSON.parse(sessionStorage.getItem('scheduledEvents') || '[]')
            : [];
        const event = {
            id: Date.now() + 1,
            title: `📡 ${broadcast.title}`,
            date: broadcast.date,
            time: broadcast.time,
            description: `Прямая ${broadcast.type === 'audio' ? 'аудио' : 'видео'} трансляция: ${broadcast.description}`
        };
        events.push(event);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('scheduledEvents', JSON.stringify(events));
        }

        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setShowCreateForm(false);
        alert('Трансляция создана и добавлена в События!');
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
        });
    };

    const confirmDeleteBroadcast = (broadcast) => {
        if (!canEdit) {
            alert('У вас нет прав на удаление');
            return;
        }
        setBroadcastToDelete(broadcast);
        setShowDeleteConfirm(true);
    };

    const handleEditBroadcast = (broadcast) => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование');
            return;
        }
        setEditingBroadcast(broadcast);
        setNewBroadcast({
            title: broadcast.title,
            description: broadcast.description,
            date: broadcast.date,
            time: broadcast.time,
            type: broadcast.type
        });
        setEditStreamType(broadcast.type);
        setShowEditModal(true);
    };

    const handleSaveEdit = () => {
        if (!newBroadcast.title || !newBroadcast.date || !newBroadcast.time) {
            alert('Заполните название, дату и время');
            return;
        }

        const updated = broadcasts.map(b =>
            b.id === editingBroadcast.id
                ? {
                    ...b,
                    ...newBroadcast,
                    type: editStreamType,
                    edited: true,
                    // Сохраняем существующие fileId и mediaUrl, если они есть
                    fileId: b.fileId || `translation-${Date.now()}`,
                    mediaUrl: b.mediaUrl || `/api/media/stream/${b.fileId || `translation-${Date.now()}`}`
                  }
                : b
        );

        setBroadcasts(updated);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }

        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setEditingBroadcast(null);
        setShowEditModal(false);
    };

    const handleCancelEdit = () => {
        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setEditingBroadcast(null);
        setShowEditModal(false);
    };

    const clearError = () => {
        setError(null);
    };

    // Функция для показа сообщений (использует React состояние)
    const showMessage = (text, type = 'error') => {
        if (type === 'error') {
            setError(text);
            // Автоматически очищаем ошибку через 5 секунд
            setTimeout(() => {
                setError(null);
            }, 5000);
        } else {
            // Для success сообщений можно использовать alert или другую систему
            alert(text);
        }
    };

    // Обработчик начала live трансляции
    const handleStartLiveBroadcast = async (broadcast) => {
        if (!isAdmin) {
            showMessage('Только администратор может начинать live трансляции');
            return;
        }

        // Проверяем, можно ли начать эту трансляцию
        if (broadcast.status !== 'scheduled') {
            showMessage('Трансляцию можно начать только если она запланирована');
            return;
        }

        // Проверяем, истек ли таймер
        const timeUntil = calculateTimeUntilBroadcast(broadcast);
        if (!timeUntil.isPast) {
            showMessage('Запрещено до назначенного времени');
            return;
        }

        // Проверяем, есть ли уже активная трансляция
        const hasActiveBroadcast = broadcasts.some(b => b.status === 'live');
        if (hasActiveBroadcast) {
            showMessage('Уже есть активная трансляция. Завершите её перед началом новой.');
            return;
        }

        // Запрашиваем доступ к камере и микрофону
        try {
            const constraints = {
                audio: true,
                video: broadcast.type === 'video' ? {
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                } : false
            };

            const stream = await navigator.mediaDevices.getUserMedia(constraints);

            // Создаем модальное окно предпросмотра безопасно
            const previewModal = document.createElement('div');
            previewModal.className = 'preview-modal-overlay';

            const modalContent = document.createElement('div');
            modalContent.className = 'preview-modal-content';

            const previewHeader = document.createElement('div');
            previewHeader.className = 'preview-header';

            const headerTitle = document.createElement('h3');
            headerTitle.textContent = 'Предпросмотр трансляции';

            const headerText = document.createElement('p');
            headerText.textContent = 'Проверьте изображение и звук перед началом трансляции';

            previewHeader.appendChild(headerTitle);
            previewHeader.appendChild(headerText);

            const previewBody = document.createElement('div');
            previewBody.className = 'preview-body';

            if (broadcast.type === 'video') {
                const videoElement = document.createElement('video');
                videoElement.className = 'preview-video';
                videoElement.autoplay = true;
                videoElement.muted = true;
                videoElement.playsInline = true;
                previewBody.appendChild(videoElement);
            } else {
                const audioPreview = document.createElement('div');
                audioPreview.className = 'audio-preview';

                const audioVisualizer = document.createElement('div');
                audioVisualizer.className = 'audio-visualizer';

                const audioText = document.createElement('p');
                audioText.textContent = 'Аудио трансляция';

                audioPreview.appendChild(audioVisualizer);
                audioPreview.appendChild(audioText);
                previewBody.appendChild(audioPreview);
            }

            const previewControls = document.createElement('div');
            previewControls.className = 'preview-controls';

            const startBtn = document.createElement('button');
            startBtn.className = 'preview-start-btn';
            startBtn.textContent = 'Начать трансляцию';

            const cancelBtn = document.createElement('button');
            cancelBtn.className = 'preview-cancel-btn';
            cancelBtn.textContent = 'Отмена';

            previewControls.appendChild(startBtn);
            previewControls.appendChild(cancelBtn);

            modalContent.appendChild(previewHeader);
            modalContent.appendChild(previewBody);
            modalContent.appendChild(previewControls);
            previewModal.appendChild(modalContent);

            document.body.appendChild(previewModal);

            // Настраиваем видео элемент
            if (broadcast.type === 'video') {
                const videoElement = previewModal.querySelector('.preview-video');
                videoElement.srcObject = stream;
            }

            // Обработчики кнопок
            startBtn.onclick = () => {
                // Останавливаем предпросмотр
                stream.getTracks().forEach(track => track.stop());
                previewModal.remove();

                // Обновляем статус трансляции на "live"
                const updated = broadcasts.map(b =>
                    b.id === broadcast.id
                        ? { ...b, status: 'live', startedAt: new Date().toISOString() }
                        : b
                );

                setBroadcasts(updated);
                if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }

                // Устанавливаем активную трансляцию и показываем live view
                setActiveLiveBroadcast({
                    ...broadcast,
                    status: 'live',
                    startedAt: new Date().toISOString(),
                    viewerCount: 1,
                    maxViewers: 1
                });
                setShowLiveView(true);
            };

            cancelBtn.onclick = () => {
                // Останавливаем предпросмотр
                stream.getTracks().forEach(track => track.stop());
                previewModal.remove();
            };

            // Закрытие по клику на оверлей
            previewModal.onclick = (e) => {
                if (e.target === previewModal) {
                    stream.getTracks().forEach(track => track.stop());
                    previewModal.remove();
                }
            };

        } catch (error) {
            showMessage('Не удалось получить доступ к камере или микрофону. Проверьте разрешения.');
        }
    };

    // Обработчик возврата в live трансляцию
    const handleReturnToLiveBroadcast = (broadcast) => {
        // Устанавливаем активную трансляцию и показываем live view
        setActiveLiveBroadcast({
            ...broadcast,
            viewerCount: 1,
            maxViewers: 1
        });
        setShowLiveView(true);
    };

    // Обработчик завершения live трансляции
    const handleEndLiveBroadcast = () => {
        if (!activeLiveBroadcast) {
            showMessage('Нет активной трансляции для завершения');
            return;
        }

        // Проверяем права на завершение трансляции
        if (!canManageBroadcast(activeLiveBroadcast)) {
            showMessage('Только создатель или SUPER_ADMIN может завершать live трансляции');
            return;
        }

        // Показываем меню подтверждения
        setShowEndConfirm(true);
    };

    // Подтверждение завершения трансляции
    const confirmEndBroadcast = () => {
        if (!activeLiveBroadcast) return;

        // Обновляем статус трансляции на "ended"
        const updated = broadcasts.map(b =>
            b.id === activeLiveBroadcast.id
                ? { ...b, status: 'ended', endedAt: new Date().toISOString() }
                : b
        );

        setBroadcasts(updated);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }

        // Закрываем live view
        setActiveLiveBroadcast(null);
        setShowLiveView(false);
        setShowEndConfirm(false);
        showMessage('Трансляция завершена!', 'success');
    };

    // Отмена завершения трансляции
    const cancelEndBroadcast = () => {
        setShowEndConfirm(false);
    };

    // Обработчик открытия настроек трансляции
    const handleOpenSettings = (broadcast) => {
        if (!isAdmin) {
            alert('Только администратор может изменять настройки трансляции');
            return;
        }

        // Проверяем, можно ли изменять настройки для этой трансляции
        if (broadcast.status !== 'scheduled') {
            alert('Настройки можно изменять только для запланированных трансляций');
            return;
        }

        setBroadcastForSettings(broadcast);
        setShowSettingsModal(true);
    };

    // Обработчик закрытия настроек
    const handleCloseSettings = () => {
        setShowSettingsModal(false);
        setBroadcastForSettings(null);
        setShowSpeedTest(false);
        setShowDeviceTest(false);
    };

    // Обработчик завершения теста скорости
    const handleSpeedTestComplete = (settings) => {
        setSpeedTestSettings(settings);
        setShowSpeedTest(false);
        alert('Автонастройки применены! Рекомендуемое качество: ' +
              (settings.quality === 'high' ? 'Высокое' :
               settings.quality === 'medium' ? 'Среднее' :
               settings.quality === 'low' ? 'Низкое' :
               settings.quality === 'very-low' ? 'Очень низкое' : 'Только аудио'));
    };

    // Отмена теста скорости
    const handleSpeedTestCancel = () => {
        setShowSpeedTest(false);
    };

    // Обработчик завершения теста устройств
    const handleDeviceTestComplete = (settings) => {
        setDeviceSettings(settings);
        setShowDeviceTest(false);
        alert('Настройки устройств применены!');
    };

    // Отмена теста устройств
    const handleDeviceTestCancel = () => {
        setShowDeviceTest(false);
    };

    // Обработчик сохранения настроек
    const handleSaveSettings = (settings) => {
        if (!broadcastForSettings) return;

        const updated = broadcasts.map(b =>
            b.id === broadcastForSettings.id
                ? { ...b, settings: { ...b.settings, ...settings } }
                : b
        );

        setBroadcasts(updated);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }
        alert('Настройки сохранены!');
        handleCloseSettings();
    };

    // Обработчик присоединения к live трансляции
    const handleJoinLiveBroadcast = (broadcast) => {
        if (broadcast.status !== 'live') {
            alert('Трансляция не активна. Присоединиться можно только к live трансляциям.');
            return;
        }

        // Устанавливаем активную трансляцию и показываем live view
        setActiveLiveBroadcast(broadcast);
        setShowLiveView(true);
    };

    // Обработчик закрытия live view
    const handleCloseLiveView = () => {
        setShowLiveView(false);
        setActiveLiveBroadcast(null);
    };

    const executeDeleteBroadcast = () => {
        if (!broadcastToDelete) return;

        const updated = broadcasts.filter(b => b.id !== broadcastToDelete.id);
        setBroadcasts(updated);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('broadcasts', JSON.stringify(updated));
        }

        setShowDeleteConfirm(false);
        setBroadcastToDelete(null);
    };

    const cancelDeleteBroadcast = () => {
        setShowDeleteConfirm(false);
        setBroadcastToDelete(null);
    };

    return (
        <div className="archive-list broadcasts-page">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="broadcasts-header">
                <h2>📡 Трансляции</h2>
                <p>Прямые аудио и видео эфиры</p>
            </div>

            {isAdmin && (
                <div className="create-broadcast-section">
                    <button
                        className="create-broadcast-btn"
                        onClick={() => setShowCreateForm(true)}
                    >
                        + Создать трансляцию
                    </button>
                </div>
            )}

            {showCreateForm && (
                <div className="modal-overlay" onClick={() => setShowCreateForm(false)}>
                    <div className="modal-content create-broadcast-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>Новая трансляция</h3>

                        <div className="stream-type-toggle">
                            <span className="toggle-label">Тип трансляции:</span>
                            <div className="toggle-switch">
                                <button
                                    className={`toggle-option ${streamType === 'audio' ? 'active' : ''}`}
                                    onClick={() => setStreamType('audio')}
                                >
                                    🎙️ Аудио
                                </button>
                                <button
                                    className={`toggle-option ${streamType === 'video' ? 'active' : ''}`}
                                    onClick={() => setStreamType('video')}
                                >
                                    🎥 Видео
                                </button>
                            </div>
                        </div>

                        <input
                            type="text"
                            placeholder="Название трансляции"
                            value={newBroadcast.title}
                            onChange={(e) => setNewBroadcast({...newBroadcast, title: e.target.value})}
                        />

                        <textarea
                            placeholder="Описание трансляции"
                            value={newBroadcast.description}
                            onChange={(e) => setNewBroadcast({...newBroadcast, description: e.target.value})}
                            rows="3"
                        />

                        <div className="date-time-group">
                            <div className="date-field">
                                <label>📅 Дата</label>
                                <input
                                    type="date"
                                    value={newBroadcast.date}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, date: e.target.value})}
                                />
                            </div>
                            <div className="time-field">
                                <label>⏰ Время</label>
                                <input
                                    type="time"
                                    value={newBroadcast.time}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, time: e.target.value})}
                                />
                            </div>
                        </div>

                        <div className="modal-buttons-vertical">
                            <button className="modal-btn-create" onClick={handleCreateBroadcast}>
                                Создать трансляцию
                            </button>
                            <button className="modal-btn-cancel" onClick={() => setShowCreateForm(false)}>
                                Отмена
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {broadcasts.length === 0 ? (
                <div className="empty-state">
                    <p>📡 Нет запланированных трансляций</p>
                    {isAdmin && <p className="empty-hint">Создайте первую трансляцию</p>}
                </div>
            ) : (
                <div className="broadcasts-grid">
                    {broadcasts.map(broadcast => (
                        <div key={broadcast.id} className="broadcast-card">
                            <div className="broadcast-type-badge">
                                {broadcast.type === 'audio' ? '🎙️ Аудио' : '🎥 Видео'}
                            </div>
                            {canEditBroadcast(broadcast) && (
                                <div className="broadcast-actions">
                                    <button
                                        className="edit-broadcast-btn"
                                        onClick={() => handleEditBroadcast(broadcast)}
                                        title="Редактировать трансляцию"
                                    >
                                        ✏️
                                    </button>
                                    <button
                                        className="delete-broadcast-btn"
                                        onClick={() => confirmDeleteBroadcast(broadcast)}
                                        title="Удалить трансляцию"
                                    >
                                        🗑️
                                    </button>
                                </div>
                            )}

                            {/* Кнопки скачивания, просмотра и live */}
                            <div className="broadcast-media-actions">
                                {broadcast.fileId && broadcast.status === 'ended' && (
                                    <>
                                        <button
                                            className="watch-broadcast-btn"
                                            onClick={() => handleWatch(broadcast)}
                                            title="Смотреть трансляцию"
                                            disabled={!broadcast.mediaUrl}
                                        >
                                            {broadcast.type === 'audio' ? '🎧 Слушать' : '▶️ Смотреть'}
                                        </button>

                                        {canDownload && (
                                            <button
                                                className="download-broadcast-btn"
                                                onClick={() => handleDownload(broadcast)}
                                                title="Скачать трансляцию"
                                                disabled={downloadingFileId === broadcast.id}
                                            >
                                                {downloadingFileId === broadcast.id ? '⏳ Скачивается...' : '⬇️ Скачать'}
                                            </button>
                                        )}
                                    </>
                                )}

                                {/* Кнопки для live трансляций */}
                                <div className="broadcast-live-actions">
                                    {broadcast.status === 'scheduled' && canManageBroadcast(broadcast) && (
                                        <>
                                            <button
                                                className="settings-broadcast-btn"
                                                onClick={() => handleOpenSettings(broadcast)}
                                                title="Настройки трансляции"
                                            >
                                                ⚙️ Настройки
                                            </button>
                                            <button
                                                className="start-live-btn"
                                                onClick={() => handleStartLiveBroadcast(broadcast)}
                                                title="Начать live трансляцию"
                                                disabled={timers[broadcast.id] && !timers[broadcast.id].isPast}
                                            >
                                                🔴 Начать Live
                                            </button>
                                        </>
                                    )}

                                    {broadcast.status === 'live' && (
                                        <>
                                            <button
                                                className="join-live-btn"
                                                onClick={() => handleJoinLiveBroadcast(broadcast)}
                                                title="Присоединиться к live трансляции"
                                            >
                                                🔴 Присоединиться
                                            </button>

                                            {/* Кнопка "Вернуться в трансляцию" для создателя и SUPER_ADMIN */}
                                            {canManageBroadcast(broadcast) && (
                                                <button
                                                    className="return-to-live-btn"
                                                    onClick={() => handleReturnToLiveBroadcast(broadcast)}
                                                    title="Вернуться в трансляцию"
                                                >
                                                    🔄 Вернуться
                                                </button>
                                            )}
                                        </>
                                    )}

                                    {broadcast.status === 'ended' && broadcast.fileId && (
                                        <button
                                            className="watch-archive-btn"
                                            onClick={() => handleWatch(broadcast)}
                                            title="Смотреть архивную трансляцию"
                                        >
                                            📺 Смотреть
                                        </button>
                                    )}
                                </div>
                            </div>
                            <h3 className="broadcast-title">
                                {broadcast.title}
                                {broadcast.edited && <span className="edited-badge-broadcast"> (изменено)</span>}
                            </h3>
                            <p className="broadcast-description">{broadcast.description}</p>

                            {/* Кнопка "Буду!" со счетчиком */}
                            {broadcast.status === 'scheduled' && (
                                <div className="will-attend-section">
                                    <button
                                        className="will-attend-btn"
                                        onClick={() => handleWillAttend(broadcast.id)}
                                        title="Откликнуться на трансляцию"
                                    >
                                        👍 Буду! ({willAttendCounts[broadcast.id] || 0})
                                    </button>
                                </div>
                            )}
                            <div className="broadcast-datetime">
                                <span>📅 {formatDate(broadcast.date)}</span>
                                <span>⏰ {broadcast.time}</span>
                                {broadcast.status === 'scheduled' && timers[broadcast.id] && (
                                    <span className="broadcast-timer">
                                        ⏱️ {formatTimer(timers[broadcast.id])}
                                    </span>
                                )}
                            </div>
                            <div className="broadcast-status">
                                {broadcast.status === 'scheduled' && '⏳ Запланирована'}
                                {broadcast.status === 'live' && '🔴 В эфире'}
                                {broadcast.status === 'ended' && '✅ Завершена'}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Модальное окно редактирования */}
            {showEditModal && editingBroadcast && (
                <div className="modal-overlay" onClick={handleCancelEdit}>
                    <div className="modal-content create-broadcast-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">✏️</span>
                            <h3>Редактировать трансляцию</h3>
                        </div>

                        <div className="stream-type-toggle">
                            <span className="toggle-label">Тип трансляции:</span>
                            <div className="toggle-switch">
                                <button
                                    className={`toggle-option ${editStreamType === 'audio' ? 'active' : ''}`}
                                    onClick={() => setEditStreamType('audio')}
                                >
                                    🎙️ Аудио
                                </button>
                                <button
                                    className={`toggle-option ${editStreamType === 'video' ? 'active' : ''}`}
                                    onClick={() => setEditStreamType('video')}
                                >
                                    🎥 Видео
                                </button>
                            </div>
                        </div>

                        <input
                            type="text"
                            placeholder="Название трансляции"
                            value={newBroadcast.title}
                            onChange={(e) => setNewBroadcast({...newBroadcast, title: e.target.value})}
                        />

                        <textarea
                            placeholder="Описание трансляции"
                            value={newBroadcast.description}
                            onChange={(e) => setNewBroadcast({...newBroadcast, description: e.target.value})}
                            rows="3"
                        />

                        <div className="date-time-group">
                            <div className="date-field">
                                <label>📅 Дата</label>
                                <input
                                    type="date"
                                    value={newBroadcast.date}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, date: e.target.value})}
                                />
                            </div>
                            <div className="time-field">
                                <label>⏰ Время</label>
                                <input
                                    type="time"
                                    value={newBroadcast.time}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, time: e.target.value})}
                                />
                            </div>
                        </div>

                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={handleCancelEdit}>
                                Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={handleSaveEdit}>
                                Сохранить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && broadcastToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteBroadcast}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить эту трансляцию?</p>
                            <div className="broadcast-preview">
                                <strong>{broadcastToDelete.title}</strong>
                                <small>{formatDate(broadcastToDelete.date)} в {broadcastToDelete.time}</small>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteBroadcast}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteBroadcast}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Live трансляция */}
            {showLiveView && activeLiveBroadcast && (
                <div className="live-broadcast-overlay">
                    <div className="live-broadcast-modal">
                        <div className="live-broadcast-header">
                            <h3>🔴 Live трансляция: {activeLiveBroadcast.title}</h3>
                            <button
                                className="close-live-btn"
                                onClick={handleCloseLiveView}
                                title="Закрыть live трансляцию"
                            >
                                ✕
                            </button>
                        </div>
                        <div className="live-broadcast-content">
                            <EnhancedLiveBroadcastView
                                broadcast={{
                                    id: activeLiveBroadcast.id.toString(),
                                    title: activeLiveBroadcast.title,
                                    description: activeLiveBroadcast.description,
                                    status: 'live',
                                    type: activeLiveBroadcast.type,
                                    viewerCount: activeLiveBroadcast.viewerCount || 1,
                                    maxViewers: activeLiveBroadcast.maxViewers || 1,
                                    startedAt: activeLiveBroadcast.startedAt || new Date().toISOString(),
                                    endedAt: null,
                                    streamUrl: activeLiveBroadcast.mediaUrl || `/api/media/stream/${activeLiveBroadcast.fileId}`,
                                    thumbnailUrl: null,
                                    tags: [],
                                    isPublic: true,
                                    createdAt: activeLiveBroadcast.createdAt || new Date().toISOString(),
                                    updatedAt: new Date().toISOString()
                                }}
                                autoPlay={true}
                                showChat={true}
                                showControls={isAdmin}
                                showStats={isAdmin}
                                layout="player-first"
                                isOrganizer={isAdmin}
                                onEndBroadcast={handleEndLiveBroadcast}
                                demoMode={true}
                            />
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно подтверждения завершения трансляции */}
            {showEndConfirm && (
                <div className="modal-overlay" onClick={cancelEndBroadcast}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">⚠️</span>
                            <h3>Подтверждение завершения</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы уверены, что хотите завершить live трансляцию?</p>
                            <p className="warning-text">После завершения трансляция будет перемещена в архив</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelEndBroadcast}>
                                Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={confirmEndBroadcast}>
                                Да, завершить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно настроек трансляции */}
            {showSettingsModal && broadcastForSettings && (
                <div className="modal-overlay" onClick={handleCloseSettings}>
                    <div className="modal-content settings-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">⚙️</span>
                            <h3>Настройки трансляции: {broadcastForSettings.title}</h3>
                        </div>
                        <div className="modal-body">
                            <div className="settings-section">
                                <h4>Качество трансляции</h4>
                                <div className="quality-options">
                                    <label className="quality-option">
                                        <input type="radio" name="quality" value="low" defaultChecked />
                                        <span>Низкое (360p)</span>
                                        <small>Для медленного интернета</small>
                                    </label>
                                    <label className="quality-option">
                                        <input type="radio" name="quality" value="medium" />
                                        <span>Среднее (720p)</span>
                                        <small>Рекомендуемое качество</small>
                                    </label>
                                    <label className="quality-option">
                                        <input type="radio" name="quality" value="high" />
                                        <span>Высокое (1080p)</span>
                                        <small>Для быстрого интернета</small>
                                    </label>
                                </div>
                            </div>

                            <div className="settings-section">
                                <h4>Аудио настройки</h4>
                                <div className="audio-settings">
                                    <label>
                                        <span>Битрейт аудио:</span>
                                        <select defaultValue="128">
                                            <option value="64">64 kbps</option>
                                            <option value="128">128 kbps</option>
                                            <option value="192">192 kbps</option>
                                            <option value="256">256 kbps</option>
                                        </select>
                                    </label>
                                    <label>
                                        <span>Частота дискретизации:</span>
                                        <select defaultValue="44100">
                                            <option value="22050">22.05 kHz</option>
                                            <option value="44100">44.1 kHz</option>
                                            <option value="48000">48 kHz</option>
                                        </select>
                                    </label>
                                </div>
                            </div>

                            {broadcastForSettings.type === 'video' && (
                                <div className="settings-section">
                                    <h4>Видео настройки</h4>
                                    <div className="video-settings">
                                        <label>
                                            <span>Частота кадров:</span>
                                            <select defaultValue="30">
                                                <option value="24">24 fps</option>
                                                <option value="30">30 fps</option>
                                                <option value="60">60 fps</option>
                                            </select>
                                        </label>
                                        <label>
                                            <span>Битрейт видео:</span>
                                            <select defaultValue="2000">
                                                <option value="1000">1000 kbps</option>
                                                <option value="2000">2000 kbps</option>
                                                <option value="4000">4000 kbps</option>
                                                <option value="8000">8000 kbps</option>
                                            </select>
                                        </label>
                                    </div>
                                </div>
                            )}

                            <div className="settings-section">
                                <h4>Тестирование устройств</h4>
                                <div className="device-test-settings">
                                    <p className="settings-description">
                                        Проверьте и настройте микрофон и камеру перед началом трансляции.
                                    </p>
                                    <button
                                        className="device-test-btn"
                                        onClick={() => setShowDeviceTest(true)}
                                    >
                                        🎤 Тест аудио и видео
                                    </button>
                                    {deviceSettings && (
                                        <div className="applied-settings-info">
                                            <span className="applied-icon">✅</span>
                                            <span>Устройства настроены</span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="settings-section">
                                <h4>Автонастройки сети</h4>
                                <div className="network-settings">
                                    <p className="settings-description">
                                        Протестируйте скорость вашего соединения для автоматической настройки оптимальных параметров трансляции.
                                    </p>
                                    <button
                                        className="speed-test-btn"
                                        onClick={() => setShowSpeedTest(true)}
                                    >
                                        🚀 Тест скорости и возможностей сети
                                    </button>
                                    {speedTestSettings && (
                                        <div className="applied-settings-info">
                                            <span className="applied-icon">✅</span>
                                            <span>Автонастройки применены: {speedTestSettings.quality === 'high' ? 'Высокое' :
                                                                           speedTestSettings.quality === 'medium' ? 'Среднее' :
                                                                           speedTestSettings.quality === 'low' ? 'Низкое' :
                                                                           speedTestSettings.quality === 'very-low' ? 'Очень низкое' : 'Только аудио'} качество</span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={handleCloseSettings}>
                                Отмена
                            </button>
                            <button
                                className="modal-btn confirm"
                                onClick={() => handleSaveSettings({
                                    quality: document.querySelector('input[name="quality"]:checked')?.value || 'medium',
                                    audioBitrate: document.querySelector('select')?.value || '128',
                                    sampleRate: document.querySelectorAll('select')[1]?.value || '44100',
                                    frameRate: broadcastForSettings.type === 'video' ? document.querySelectorAll('select')[2]?.value || '30' : null,
                                    videoBitrate: broadcastForSettings.type === 'video' ? document.querySelectorAll('select')[3]?.value || '2000' : null,
                                    // Автоматические настройки (всегда включены)
                                    record: true, // Запись трансляции всегда включена
                                    chatEnabled: true, // Чат всегда включен
                                    chatModeration: true // Модерация чата всегда включена
                                })}
                            >
                                Сохранить настройки
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно теста скорости сети */}
            {showSpeedTest && (
                <div className="modal-overlay" onClick={handleSpeedTestCancel}>
                    <div className="modal-content speed-test-modal" onClick={(e) => e.stopPropagation()}>
                        <NetworkSpeedTest
                            onTestComplete={handleSpeedTestComplete}
                            onCancel={handleSpeedTestCancel}
                        />
                    </div>
                </div>
            )}

            {/* Модальное окно тестирования устройств */}
            {showDeviceTest && (
                <div className="modal-overlay" onClick={handleDeviceTestCancel}>
                    <div className="modal-content device-test-modal" onClick={(e) => e.stopPropagation()}>
                        <DeviceTest
                            onTestComplete={handleDeviceTestComplete}
                            onCancel={handleDeviceTestCancel}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default ArchiveList;
