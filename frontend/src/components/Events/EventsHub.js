import React, { useState, useRef, useEffect } from 'react';
import { getUsername } from '../../keycloak';
import './EventsHub.css';

const EventsHub = () => {
    const [userRoles, setUserRoles] = useState([]);
    const [showAddForm, setShowAddForm] = useState(false);
    const [activeMenu, setActiveMenu] = useState(null);
    const [newMaterial, setNewMaterial] = useState({ type: 'text', title: '', content: '', url: '' });
    const [notification, setNotification] = useState(null);
    const [showFeedbackForm, setShowFeedbackForm] = useState(false);
    const [feedbackText, setFeedbackText] = useState('');
    const [audioFiles, setAudioFiles] = useState([]);
    const [editingMaterial, setEditingMaterial] = useState(null);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [materialToDelete, setMaterialToDelete] = useState(null);

    // Состояния для записи аудио
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const recordingTimerRef = useRef(null);

    // Меню и их подменю (без смайликов)
    const menuItems = [
        { id: 'about', title: 'О Нас', subItems: [] },
        { id: 'important', title: 'Важные материалы', subItems: [] },
        { id: 'interviews', title: 'Интервью', subItems: [] },
        { id: 'beginners', title: 'Для новичков', isHighlighted: true, subItems: [] },
        { id: 'reflections', title: 'Для размышлений', subItems: [] },
        { id: 'participants', title: 'От соучастников проекта', subItems: [] },
        { id: 'music', title: 'Музыка', subItems: [] },
        { id: 'motivation', title: 'Мотивация', subItems: [] },
        { id: 'movies', title: 'Фильмы', subItems: [] },
        { id: 'feedback', title: 'Отзывы и Предложения', isFeedback: true, subItems: [] },
        { id: 'results', title: 'Результаты Тимошенко', subItems: [] },
        { id: 'broadcasts', title: 'Эфиры', isSpecial: true, subItems: [] },
        { id: 'challenge', title: 'Годовой челлендж', isHighlighted: true, subItems: [] }
    ];

    // Получаем роль из localStorage
    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
    const currentUser = getUsername();

    // Отладка: выводим роль и статус админа
    console.log('[EventsHub] demoRole:', demoRole);
    console.log('[EventsHub] isAdmin:', isAdmin);

    // Загрузка аудио файлов из папки
    useEffect(() => {
        // Список аудио файлов из папки public/audio
        const audioList = [
            { id: 1, title: 'Эфир от 01.01.2025', url: '/audio/broadcast1.mp3', date: '2025-01-01' },
            { id: 2, title: 'Эфир от 15.01.2025', url: '/audio/broadcast2.mp3', date: '2025-01-15' },
            { id: 3, title: 'Эфир от 01.02.2025', url: '/audio/broadcast3.mp3', date: '2025-02-01' },
            { id: 4, title: 'Эфир от 15.02.2025', url: '/audio/broadcast4.mp3', date: '2025-02-15' },
            { id: 5, title: 'Эфир от 01.03.2025', url: '/audio/broadcast5.mp3', date: '2025-03-01' }
        ];
        setAudioFiles(audioList);
    }, []);

    useEffect(() => {
        const savedRoles = localStorage.getItem('demoRole');
        let roles = [];
        if (savedRoles === 'admin') {
            roles = ['admin', 'user'];
        } else if (savedRoles === 'user') {
            roles = ['user'];
        }
        setUserRoles(roles);
    }, []);

    // Загрузка материалов для раздела
    const getMaterialsForSection = (sectionId) => {
        const savedArchive = localStorage.getItem('archiveMaterials');
        if (!savedArchive) return [];
        const archive = JSON.parse(savedArchive);
        return archive.filter(m => m.menuId === sectionId);
    };

    // Функция для определения типа файла
    const getFileType = (file) => {
        if (file.type.startsWith('audio/')) return 'audio';
        if (file.type.startsWith('video/')) return 'video';
        if (file.type.startsWith('image/')) return 'photo';
        return 'text';
    };

    // Обработка загрузки файла
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const fileType = getFileType(file);
        const url = URL.createObjectURL(file);

        setNewMaterial({
            ...newMaterial,
            type: fileType,
            url: url,
            title: newMaterial.title || file.name.replace(/\.[^/.]+$/, '')
        });
    };

    // Функции для записи аудио
    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            const chunks = [];

            recorder.ondataavailable = (e) => {
                chunks.push(e.data);
            };

            recorder.onstop = async () => {
                const audioBlob = new Blob(chunks, { type: 'audio/webm' });
                const audioUrl = URL.createObjectURL(audioBlob);

                setNewMaterial({
                    ...newMaterial,
                    type: 'audio',
                    url: audioUrl,
                    title: newMaterial.title || `Аудиозапись ${new Date().toLocaleString()}`
                });

                stream.getTracks().forEach(track => track.stop());
            };

            recorder.start();
            setMediaRecorder(recorder);
            setIsRecording(true);

            setRecordingTime(0);
            recordingTimerRef.current = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);

        } catch (err) {
            console.error('Ошибка доступа к микрофону:', err);
            alert('Не удалось получить доступ к микрофону. Проверьте разрешения.');
        }
    };

    const stopRecording = () => {
        if (mediaRecorder && isRecording) {
            mediaRecorder.stop();
            setIsRecording(false);
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
        }
    };

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    // Отправка отзыва/предложения
    const handleSendFeedback = () => {
        if (!feedbackText.trim()) {
            alert('Введите ваш отзыв или предложение');
            return;
        }

        const feedback = {
            id: Date.now(),
            type: 'feedback',
            text: feedbackText.trim(),
            author: currentUser,
            date: new Date().toISOString().split('T')[0],
            time: new Date().toLocaleTimeString(),
            isRead: false
        };

        const savedFeedbacks = localStorage.getItem('userFeedbacks');
        let feedbacks = savedFeedbacks ? JSON.parse(savedFeedbacks) : [];
        feedbacks.unshift(feedback);
        localStorage.setItem('userFeedbacks', JSON.stringify(feedbacks));

        const savedQuestions = localStorage.getItem('userQuestions');
        let questions = savedQuestions ? JSON.parse(savedQuestions) : [];

        const questionFeedback = {
            id: Date.now(),
            question: `Отзыв/Предложение от ${currentUser}: ${feedbackText.substring(0, 100)}${feedbackText.length > 100 ? '...' : ''}`,
            fullText: feedbackText,
            answer: '',
            author: currentUser,
            date: new Date().toISOString().split('T')[0],
            isAnswered: false,
            isFeedback: true
        };

        questions.unshift(questionFeedback);
        localStorage.setItem('userQuestions', JSON.stringify(questions));

        setNotification('Ваш отзыв отправлен администратору!');
        setTimeout(() => setNotification(null), 3000);

        setFeedbackText('');
        setShowFeedbackForm(false);
        setActiveMenu(null);
    };

    const handleAddMaterial = () => {
        if (!isAdmin) {
            alert('Только администратор может добавлять материалы');
            return;
        }

        if (!newMaterial.title) {
            alert('Заполните заголовок');
            return;
        }

        const savedArchive = localStorage.getItem('archiveMaterials');
        let archive = savedArchive ? JSON.parse(savedArchive) : [];

        if (editingMaterial) {
            // Редактирование существующего материала
            archive = archive.map(m =>
                m.id === editingMaterial.id
                    ? {
                        ...m,
                        ...newMaterial,
                        editedAt: new Date().toISOString()
                    }
                    : m
            );
            setNotification('Материал обновлён!');
            setEditingMaterial(null);
        } else {
            // Создание нового материала
            const material = {
                id: Date.now(),
                ...newMaterial,
                date: new Date().toISOString().split('T')[0],
                time: new Date().toLocaleTimeString(),
                author: 'Admin',
                menuId: activeMenu
            };
            archive.unshift(material);
            setNotification('Материал добавлен в архив!');
        }

        localStorage.setItem('archiveMaterials', JSON.stringify(archive));
        setTimeout(() => setNotification(null), 3000);

        setNewMaterial({ type: 'text', title: '', content: '', url: '' });
        setShowAddForm(false);
        setActiveMenu(null);
    };

    const handleEditMaterial = (material) => {
        if (!isAdmin) {
            alert('Только администратор может редактировать материалы');
            return;
        }

        setEditingMaterial(material);
        setNewMaterial({
            type: material.type,
            title: material.title,
            content: material.content || '',
            url: material.url || ''
        });
        setShowAddForm(true);
    };

    const handleDeleteMaterial = (material) => {
        if (!isAdmin) {
            alert('Только администратор может удалять материалы');
            return;
        }

        setMaterialToDelete(material);
        setShowDeleteConfirm(true);
    };

    const confirmDeleteMaterial = () => {
        const savedArchive = localStorage.getItem('archiveMaterials');
        let archive = savedArchive ? JSON.parse(savedArchive) : [];

        archive = archive.filter(m => m.id !== materialToDelete.id);
        localStorage.setItem('archiveMaterials', JSON.stringify(archive));

        setNotification('Материал удалён!');
        setTimeout(() => setNotification(null), 3000);

        setShowDeleteConfirm(false);
        setMaterialToDelete(null);
    };

    const cancelDeleteMaterial = () => {
        setShowDeleteConfirm(false);
        setMaterialToDelete(null);
    };

    // Получение материалов для текущего раздела
    const currentMaterials = activeMenu && activeMenu !== 'feedback' && activeMenu !== 'broadcasts'
        ? getMaterialsForSection(activeMenu)
        : [];

    return (
        <div className="events-hub">
            {notification && (
                <div className="hub-notification">
                    <div className="notification-content">
                        <div className="notification-text">
                            <strong>Успешно!</strong>
                            <span>{notification}</span>
                        </div>
                        <div className="notification-close" onClick={() => setNotification(null)}>✕</div>
                    </div>
                </div>
            )}

            {/* Модальное окно добавления материала */}
            {showAddForm && isAdmin && (
                <div className="add-material-modal">
                    <div className="modal-container">
                        <div className="modal-header">
                            <h3>{editingMaterial ? 'Редактировать материал' : `Добавить материал в раздел "${menuItems.find(m => m.id === activeMenu)?.title}"`}</h3>
                            <button className="modal-close" onClick={() => {
                                setShowAddForm(false);
                                setActiveMenu(null);
                                setEditingMaterial(null);
                                setNewMaterial({ type: 'text', title: '', content: '', url: '' });
                            }}>✕</button>
                        </div>
                        <div className="modal-body">
                            <input
                                type="text"
                                className="material-title-input"
                                placeholder="Заголовок материала"
                                value={newMaterial.title}
                                onChange={(e) => setNewMaterial({...newMaterial, title: e.target.value})}
                            />
                            <textarea
                                className="material-content-input"
                                placeholder="Описание материала..."
                                value={newMaterial.content}
                                onChange={(e) => setNewMaterial({...newMaterial, content: e.target.value})}
                                rows="4"
                            />

                            <div className="media-controls">
                                <div className="media-buttons">
                                    <label className="attach-button">
                                        Прикрепить файл
                                        <input
                                            type="file"
                                            accept="audio/*,video/*,image/*"
                                            onChange={handleFileUpload}
                                            style={{ display: 'none' }}
                                        />
                                    </label>

                                    <button
                                        className={`mic-button ${isRecording ? 'recording' : ''}`}
                                        onClick={isRecording ? stopRecording : startRecording}
                                        type="button"
                                    >
                                        {isRecording ? 'Остановить запись' : 'Записать аудио'}
                                    </button>
                                </div>

                                {isRecording && (
                                    <div className="recording-indicator">
                                        <span className="recording-dot">●</span>
                                        <span className="recording-time">Запись: {formatRecordingTime(recordingTime)}</span>
                                    </div>
                                )}

                                {newMaterial.url && (
                                    <div className="attached-file">
                                        <span className="file-info">
                                            {newMaterial.type === 'audio' ? 'Аудиофайл' :
                                                newMaterial.type === 'video' ? 'Видеофайл' :
                                                    newMaterial.type === 'photo' ? 'Фото' : 'Документ'}
                                        </span>
                                        <button
                                            className="remove-file"
                                            onClick={() => setNewMaterial({...newMaterial, url: '', type: 'text'})}
                                        >
                                            ✕
                                        </button>
                                    </div>
                                )}
                            </div>

                            <button className="publish-btn" onClick={handleAddMaterial}>
                                Добавить в архив
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно для отзыва/предложения */}
            {showFeedbackForm && (
                <div className="add-material-modal">
                    <div className="modal-container">
                        <div className="modal-header">
                            <h3>Ваш отзыв или предложение</h3>
                            <button className="modal-close" onClick={() => {
                                setShowFeedbackForm(false);
                                setActiveMenu(null);
                            }}>✕</button>
                        </div>
                        <div className="modal-body">
                            <textarea
                                className="feedback-input"
                                placeholder="Поделитесь своим мнением, предложением или отзывом о проекте..."
                                value={feedbackText}
                                onChange={(e) => setFeedbackText(e.target.value)}
                                rows="6"
                            />
                            <div className="feedback-hint">
                                <p>Ваш отзыв будет отправлен администратору</p>
                            </div>
                            <button className="publish-btn" onClick={handleSendFeedback}>
                                Отправить отзыв
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Основное меню */}
            <div className="events-menu">
                <div className="menu-title">
                    <h2>Библиотека материалов</h2>
                    <p>Наведите на раздел, чтобы увидеть содержимое</p>
                </div>

                <div className="menu-grid">
                    {menuItems.map((item) => (
                        <div
                            key={item.id}
                            className={`menu-card ${activeMenu === item.id ? 'active' : ''} ${item.isHighlighted ? 'highlighted' : ''} ${item.isSpecial ? 'special' : ''}`}
                            onMouseEnter={() => setActiveMenu(item.id)}
                            onMouseLeave={() => setActiveMenu(null)}
                        >
                            <div className="menu-card-header">
                                <span className="menu-title-text">{item.title}</span>
                                {item.isHighlighted && <span className="badge">важное</span>}
                                {item.isSpecial && <span className="badge special-badge">эфиры</span>}
                            </div>

                            {activeMenu === item.id && (
                                <div className="menu-dropdown">
                                    {item.id === 'broadcasts' ? (
                                        // Список эфиров из папки audio
                                        <div className="broadcasts-list">
                                            {audioFiles.length > 0 ? (
                                                audioFiles.map(audio => (
                                                    <div key={audio.id} className="broadcast-item">
                                                        <div className="broadcast-info">
                                                            <span className="broadcast-title">{audio.title}</span>
                                                            <span className="broadcast-date">{audio.date}</span>
                                                        </div>
                                                        <audio controls className="broadcast-player">
                                                            <source src={audio.url} type="audio/mpeg" />
                                                            Ваш браузер не поддерживает аудио
                                                        </audio>
                                                    </div>
                                                ))
                                            ) : (
                                                <div className="empty-message">Нет доступных эфиров</div>
                                            )}
                                            {isAdmin && (
                                                <button
                                                    className="add-material-btn-full"
                                                    onClick={() => {
                                                        setShowAddForm(true);
                                                    }}
                                                >
                                                    + Добавить материал в этот раздел
                                                </button>
                                            )}
                                        </div>
                                    ) : item.id === 'feedback' ? (
                                        <div className="feedback-section">
                                            {!isAdmin && (
                                                <button
                                                    className="add-material-btn-full feedback-btn"
                                                    onClick={() => {
                                                        setShowFeedbackForm(true);
                                                    }}
                                                >
                                                    Оставить отзыв или предложение
                                                </button>
                                            )}
                                            {isAdmin && (
                                                <div className="admin-notice">
                                                    <p>Отзывы пользователей отображаются в разделе "Вопросы"</p>
                                                </div>
                                            )}
                                        </div>
                                    ) : (
                                        <div className="materials-list">
                                            {currentMaterials.length > 0 ? (
                                                currentMaterials.map(material => (
                                                    <div key={material.id} className="material-item">
                                                        <div className="material-header">
                                                            <span className="material-title">{material.title}</span>
                                                            <div className="material-actions">
                                                                <span className="material-date">{material.date}</span>
                                                                {isAdmin && (
                                                                    <>
                                                                        <button
                                                                            className="edit-btn"
                                                                            onClick={() => handleEditMaterial(material)}
                                                                            title="Редактировать"
                                                                        >
                                                                            ✏️
                                                                        </button>
                                                                        <button
                                                                            className="delete-btn"
                                                                            onClick={() => handleDeleteMaterial(material)}
                                                                            title="Удалить"
                                                                        >
                                                                            🗑️
                                                                        </button>
                                                                    </>
                                                                )}
                                                            </div>
                                                        </div>
                                                        {material.content && (
                                                            <p className="material-description">{material.content}</p>
                                                        )}
                                                        {material.type === 'audio' && material.url && (
                                                            <audio controls className="material-player">
                                                                <source src={material.url} type="audio/mpeg" />
                                                            </audio>
                                                        )}
                                                        {material.type === 'video' && material.url && (
                                                            <video controls className="material-player">
                                                                <source src={material.url} type="video/mp4" />
                                                            </video>
                                                        )}
                                                        {material.type === 'photo' && material.url && (
                                                            <img src={material.url} alt={material.title} className="material-image" />
                                                        )}
                                                    </div>
                                                ))
                                            ) : (
                                                <div className="empty-message">Нет материалов в этом разделе</div>
                                            )}
                                            {isAdmin && (
                                                <button
                                                    className="add-material-btn-full"
                                                    onClick={() => {
                                                        setShowAddForm(true);
                                                    }}
                                                >
                                                    + Добавить материал в этот раздел
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && (
                <div className="add-material-modal">
                    <div className="modal-container delete-confirm">
                        <div className="modal-header">
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы уверены, что хотите удалить материал "{materialToDelete?.title}"?</p>
                            <div className="confirm-buttons">
                                <button className="cancel-btn" onClick={cancelDeleteMaterial}>
                                    Отмена
                                </button>
                                <button className="delete-confirm-btn" onClick={confirmDeleteMaterial}>
                                    Удалить
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EventsHub;
