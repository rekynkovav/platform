import React, { useState, useEffect, useRef } from 'react';
import { getUserRoles } from '../../keycloak';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import './MainFeed.css';

const MainFeed = () => {
    const [posts, setPosts] = useState([]);
    const [newPost, setNewPost] = useState({ type: 'text', title: '', content: '', url: '' });
    const [showAddForm, setShowAddForm] = useState(false);
    const [userRoles, setUserRoles] = useState([]);
    const [notification, setNotification] = useState(null);
    const feedEndRef = useRef(null);
    const addFormRef = useRef(null);
    const [activeAudioPostId, setActiveAudioPostId] = useState(null);
    const [, forceUpdate] = useState(0);
    const blobUrlsRef = useRef([]);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDelete, setPostToDelete] = useState(null);

    // Состояния для редактирования
    const [editingPost, setEditingPost] = useState(null);
    const [showEditForm, setShowEditForm] = useState(false);

    // Состояния для записи аудио
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [audioChunks, setAudioChunks] = useState([]);
    const [recordingTime, setRecordingTime] = useState(0);
    const recordingTimerRef = useRef(null);

    const {
        playTrack,
        pauseTrack,
        currentTrack,
        isPlaying,
        stopTrack,
        volume,
        playbackRate,
        setVolume,
        setPlaybackRate,
        seek,
        currentTime,
        duration,
        formatTime
    } = useAudioPlayer();

    // Функция для обновления ролей из localStorage
    const updateRoles = () => {
        const demoRole = localStorage.getItem('demoRole');
        let roles = [];

        if (demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin') {
            roles = ['admin', 'user'];
        } else if (demoRole === 'user1' || demoRole === 'user2') {
            roles = ['user'];
        } else {
            roles = getUserRoles();
        }

        setUserRoles(roles);
        return roles;
    };

    // Синхронизируем activeAudioPostId с currentTrack
    useEffect(() => {
        if (currentTrack) {
            setActiveAudioPostId(currentTrack.id);
        } else if (!currentTrack && activeAudioPostId) {
            const timer = setTimeout(() => {
                setActiveAudioPostId(null);
            }, 500);
            return () => clearTimeout(timer);
        }
    }, [currentTrack]);

    // Cleanup blob URLs и recording timer при размонтировании
    useEffect(() => {
        return () => {
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    // Загрузка постов и ролей
    useEffect(() => {
        const savedPosts = localStorage.getItem('mainFeedPosts');
        if (savedPosts) {
            setPosts(JSON.parse(savedPosts));
        } else {
            const initialPosts = [
                {
                    id: 1,
                    type: 'text',
                    title: '📄 О стремлении человека не есть',
                    content: 'Понимание истинных потребностей тела и духа. Человек может существовать без физической пищи, черпая энергию из духовных источников.',
                    date: '2025-03-15',
                    author: 'Admin'
                },
                {
                    id: 2,
                    type: 'text',
                    title: '📄 Основа сила человека - дух',
                    content: 'Духовная сила - это основа всего. Развивая дух, мы развиваем себя.',
                    date: '2025-03-16',
                    author: 'Admin'
                },
                {
                    id: 3,
                    type: 'audio',
                    title: '🎵 Эфир - 01.01.2025',
                    content: 'Новогодний эфир. Подведение итогов и планы на будущее.',
                    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
                    date: '2025-01-01',
                    author: 'Admin'
                },
                {
                    id: 4,
                    type: 'audio',
                    title: '🎵 Эфир - 13.01.2025',
                    content: 'Практики дыхания и медитации.',
                    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
                    date: '2025-01-13',
                    author: 'Admin'
                },
                {
                    id: 5,
                    type: 'audio',
                    title: '🎵 Эфир - 22.01.2025',
                    content: 'Сила движения и его значение в жизни.',
                    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
                    date: '2025-01-22',
                    author: 'Admin'
                },
                {
                    id: 6,
                    type: 'text',
                    title: '📄 Значение движения',
                    content: 'Движение - это жизнь. Ежедневная физическая активность необходима для поддержания здоровья и энергии.',
                    date: '2025-03-17',
                    author: 'Admin'
                },
                {
                    id: 7,
                    type: 'text',
                    title: '📄 Дыхание - основа жизни человека',
                    content: 'Правильное дыхание - ключ к здоровью и долголетию.',
                    date: '2025-03-18',
                    author: 'Admin'
                },
                {
                    id: 8,
                    type: 'text',
                    title: '📄 О силе человека',
                    content: 'Источники внутренней силы. Как развивать и сохранять силу духа.',
                    date: '2025-03-19',
                    author: 'Admin'
                }
            ];
            setPosts(initialPosts);
            localStorage.setItem('mainFeedPosts', JSON.stringify(initialPosts));
        }

        updateRoles();
    }, []);

    useEffect(() => {
        if (feedEndRef.current) {
            feedEndRef.current.scrollIntoView({ behavior: 'auto' });
        }
    }, [posts]);

    useEffect(() => {
        if (showAddForm && addFormRef.current) {
            setTimeout(() => {
                addFormRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 100);
        }
    }, [showAddForm]);

    // Функция для определения типа файла
    const getFileType = (file) => {
        if (file.type.startsWith('audio/')) return 'audio';
        if (file.type.startsWith('video/')) return 'video';
        return 'text';
    };

    const handleAddPost = () => {
        const demoRole = localStorage.getItem('demoRole');
        const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
        if (!isAdmin) {
            alert('Только администратор может добавлять записи');
            return;
        }

        setShowAddForm(true);
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
                blobUrlsRef.current.push(audioUrl);

                setNewPost({
                    ...newPost,
                    type: 'audio',
                    url: audioUrl,
                    title: newPost.title || `Аудиозапись ${new Date().toLocaleString()}`
                });

                setAudioChunks([]);
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

    // Обработка загрузки файла
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const fileType = getFileType(file);
        const url = URL.createObjectURL(file);
        blobUrlsRef.current.push(url);

        setNewPost({
            ...newPost,
            type: fileType,
            url: url,
            title: newPost.title || file.name.replace(/\.[^/.]+$/, '')
        });
    };

    const handlePublishPost = () => {
        if (!newPost.title || (!newPost.content && newPost.type === 'text')) {
            alert('Заполните заголовок и содержание');
            return;
        }

        const post = {
            id: Date.now(),
            type: newPost.type,
            title: newPost.title,
            content: newPost.content,
            url: newPost.url || '',
            date: new Date().toISOString().split('T')[0],
            author: 'Admin'
        };

        const updatedPosts = [...posts, post];
        setPosts(updatedPosts);
        localStorage.setItem('mainFeedPosts', JSON.stringify(updatedPosts));

        setNewPost({ type: 'text', title: '', content: '', url: '' });
        setShowAddForm(false);
        setIsRecording(false);
        setRecordingTime(0);

        // Revoke blob URLs
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];

        setTimeout(() => {
            if (feedEndRef.current) {
                feedEndRef.current.scrollIntoView({ behavior: 'smooth' });
            }
        }, 100);
    };

    // Функция для открытия модального окна подтверждения
    const confirmDeletePost = (post) => {
        setPostToDelete(post);
        setShowDeleteConfirm(true);
    };

    // Функция для фактического удаления
    const executeDeletePost = () => {
        if (!postToDelete) return;

        const demoRole = localStorage.getItem('demoRole');
        const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
        if (!isAdmin) {
            alert('Только администратор может удалять записи!');
            setShowDeleteConfirm(false);
            setPostToDelete(null);
            return;
        }

        const updatedPosts = posts.filter(post => post.id !== postToDelete.id);
        setPosts(updatedPosts);
        localStorage.setItem('mainFeedPosts', JSON.stringify(updatedPosts));

        if (currentTrack?.id === postToDelete.id) {
            stopTrack();
        }

        setShowDeleteConfirm(false);
        setPostToDelete(null);
        forceUpdate(prev => prev + 1);
    };

    // Функция для отмены удаления
    const cancelDeletePost = () => {
        setShowDeleteConfirm(false);
        setPostToDelete(null);
    };

    // Функция для начала редактирования поста
    const handleEditPost = (post) => {
        const demoRole = localStorage.getItem('demoRole');
        const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';

        if (!isAdmin) {
            alert('Только администратор может редактировать записи');
            return;
        }

        setEditingPost(post);
        setNewPost({
            type: post.type,
            title: post.title,
            content: post.content,
            url: post.url || ''
        });
        setShowEditForm(true);
    };

    // Функция для сохранения отредактированного поста
    const handleSaveEditedPost = () => {
        if (!newPost.title || (!newPost.content && newPost.type === 'text')) {
            alert('Заполните заголовок и содержание');
            return;
        }

        const updatedPosts = posts.map(post =>
            post.id === editingPost.id
                ? {
                    ...post,
                    type: newPost.type,
                    title: newPost.title,
                    content: newPost.content,
                    url: newPost.url || '',
                    editedAt: new Date().toISOString()
                }
                : post
        );

        setPosts(updatedPosts);
        localStorage.setItem('mainFeedPosts', JSON.stringify(updatedPosts));

        setNewPost({ type: 'text', title: '', content: '', url: '' });
        setShowEditForm(false);
        setEditingPost(null);
        setIsRecording(false);
        setRecordingTime(0);
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];
    };

    // Функция для отмены редактирования
    const handleCancelEdit = () => {
        setShowEditForm(false);
        setEditingPost(null);
        setNewPost({ type: 'text', title: '', content: '', url: '' });
        setIsRecording(false);
        setRecordingTime(0);
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];
    };

    // Функция для скачивания материала (только для главного админа)
    const handleDownloadPost = async (post) => {
        const demoRole = localStorage.getItem('demoRole');
        const isSuperAdmin = demoRole === 'super_admin';

        if (!isSuperAdmin) {
            alert('Только главный админ может скачивать материалы');
            return;
        }

        try {
            if (post.type === 'audio' || post.type === 'video') {
                // Скачивание медиа файлов
                const response = await fetch(post.url);
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${post.title}.${post.type === 'audio' ? 'mp3' : 'mp4'}`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            } else {
                // Скачивание текстового контента
                const content = `${post.title}\n\n${post.content}\n\nДата: ${post.date}\nАвтор: ${post.author}`;
                const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${post.title}.txt`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (error) {
            console.error('Ошибка при скачивании:', error);
            alert('Не удалось скачать файл');
        }
    };

    const handlePlayAudio = (post) => {
        if (activeAudioPostId === post.id && currentTrack?.id === post.id && isPlaying) {
            pauseTrack();
        } else if (activeAudioPostId === post.id && currentTrack?.id === post.id && !isPlaying) {
            playTrack({ id: post.id, title: post.title, url: post.url });
        } else {
            setActiveAudioPostId(post.id);
            playTrack({ id: post.id, title: post.title, url: post.url });
        }
    };

    const handleSeek = (e, postId) => {
        const newTime = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            seek(newTime);
        }
    };

    const handleVolumeChangeLocal = (e, postId) => {
        const newVolume = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setVolume(newVolume);
        }
    };

    const handleSpeedChangeLocal = (e, postId) => {
        const newSpeed = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setPlaybackRate(newSpeed);
        }
    };

    const getCurrentTimeForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return currentTime;
        }
        return 0;
    };

    const getDurationForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return duration;
        }
        return 0;
    };

    const isThisPlaying = (postId) => {
        return currentTrack?.id === postId && isPlaying;
    };

    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin_moderator' || demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
    const isSuperAdmin = demoRole === 'super_admin';

    return (
        <div className="main-feed">
            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && postToDelete && (
                <div className="modal-overlay" onClick={cancelDeletePost}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить публикацию?</p>
                            <div className="post-preview">
                                <strong>{postToDelete.title}</strong>
                                <small>{postToDelete.date}</small>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeletePost}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeletePost}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {notification && (
                <div className={`event-notification ${notification.includes('удалена') ? 'delete-notification' : ''}`}>
                    <div className="notification-content">
                        <div className="notification-text">
                            <strong>{notification.includes('удалена') ? 'Запись удалена' : 'Новая запись!'}</strong>
                            <span>{notification}</span>
                        </div>
                        <div className="notification-close" onClick={() => setNotification(null)}>✕</div>
                    </div>
                </div>
            )}

            <div className="feed-container">
                <div className="feed-list">
                    {posts.map((post) => (
                        <div key={post.id} className="feed-post">
                            <div className="post-header">
                                <div className="post-title">{post.title}</div>
                                {isAdmin && (
                                    <div className="post-actions">
                                        {isSuperAdmin && (
                                            <button
                                                className="download-post"
                                                onClick={() => handleDownloadPost(post)}
                                                title="Скачать материал"
                                            >
                                                ⬇️
                                            </button>
                                        )}
                                        <button
                                            className="edit-post"
                                            onClick={() => handleEditPost(post)}
                                            title="Редактировать запись"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            className="delete-post"
                                            onClick={() => confirmDeletePost(post)}
                                            title="Удалить запись"
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                )}
                            </div>
                            <div className="post-date">{post.date}</div>
                            <div className="post-content">{post.content}</div>

                            {post.type === 'audio' && (
                                <div className="post-audio">
                                    {activeAudioPostId === post.id ? (
                                        <div className="inline-audio-player">
                                            <div className="inline-audio-header">
                                                <span className="inline-audio-title">🎵 {post.title}</span>
                                                <button className="inline-stop-btn" onClick={() => stopTrack()}>
                                                    ✕
                                                </button>
                                            </div>
                                            <div className="inline-progress">
                                                <span>{formatTime(getCurrentTimeForPost(post.id))}</span>
                                                <input
                                                    type="range"
                                                    className="inline-seek"
                                                    min="0"
                                                    max={getDurationForPost(post.id) || 100}
                                                    value={getCurrentTimeForPost(post.id)}
                                                    onChange={(e) => handleSeek(e, post.id)}
                                                />
                                                <span>{formatTime(getDurationForPost(post.id))}</span>
                                            </div>
                                            <div className="inline-controls">
                                                <button className="inline-play" onClick={() => handlePlayAudio(post)}>
                                                    {isThisPlaying(post.id) ? '⏸️' : '▶️'}
                                                </button>
                                                <div className="inline-volume">
                                                    <span>🔊</span>
                                                    <input
                                                        type="range"
                                                        min="0"
                                                        max="1"
                                                        step="0.01"
                                                        value={volume}
                                                        onChange={(e) => handleVolumeChangeLocal(e, post.id)}
                                                    />
                                                </div>
                                                <div className="inline-speed">
                                                    <span>⚡</span>
                                                    <select value={playbackRate} onChange={(e) => handleSpeedChangeLocal(e, post.id)}>
                                                        <option value="0.5">0.5x</option>
                                                        <option value="0.75">0.75x</option>
                                                        <option value="1.0">1.0x</option>
                                                        <option value="1.25">1.25x</option>
                                                        <option value="1.5">1.5x</option>
                                                        <option value="2.0">2.0x</option>
                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <button className="play-audio-btn" onClick={() => handlePlayAudio(post)}>
                                            ▶️ Слушать аудио
                                        </button>
                                    )}
                                </div>
                            )}

                            {post.type === 'video' && post.url && (
                                <div className="post-video">
                                    <video controls width="100%">
                                        <source src={post.url} type="video/mp4" />
                                    </video>
                                </div>
                            )}
                        </div>
                    ))}
                    <div ref={feedEndRef} className="feed-end-marker" />
                </div>

                {isAdmin && (
                    <div className="admin-section" ref={addFormRef}>
                        {!showAddForm && !showEditForm && (
                            <button className="create-post-btn add-event-style" onClick={handleAddPost}>
                                + Добавить запись
                            </button>
                        )}

                        {(showAddForm || showEditForm) && (
                            <div className="admin-panel-content">
                                <h3>{showEditForm ? 'Редактирование записи' : 'Новая запись'}</h3>
                                <input
                                    type="text"
                                    className="post-title-input"
                                    placeholder="Заголовок записи"
                                    value={newPost.title}
                                    onChange={(e) => setNewPost({...newPost, title: e.target.value})}
                                />
                                <textarea
                                    className="post-content-input"
                                    placeholder="Текст записи..."
                                    value={newPost.content}
                                    onChange={(e) => setNewPost({...newPost, content: e.target.value})}
                                    rows="4"
                                />

                                <div className="media-controls">
                                    <div className="media-buttons">
                                        <label className="attach-button">
                                            📎 Прикрепить файл
                                            <input
                                                type="file"
                                                accept="audio/*,video/*"
                                                onChange={handleFileUpload}
                                                style={{ display: 'none' }}
                                            />
                                        </label>

                                        <button
                                            className={`mic-button ${isRecording ? 'recording' : ''}`}
                                            onClick={isRecording ? stopRecording : startRecording}
                                            type="button"
                                        >
                                            {isRecording ? '⏹️ Остановить запись' : '🎤 Записать аудио'}
                                        </button>
                                    </div>

                                    {isRecording && (
                                        <div className="recording-indicator">
                                            <span className="recording-dot">🔴</span>
                                            <span className="recording-time">Запись: {formatRecordingTime(recordingTime)}</span>
                                        </div>
                                    )}

                                    {newPost.url && (
                                        <div className="attached-file">
                                            <span className="file-info">
                                                {newPost.type === 'audio' ? '🎵 Аудиофайл' : '🎬 Видеофайл'}:
                                                {newPost.title !== newPost.url && ` ${newPost.title}`}
                                            </span>
                                            <button
                                                className="remove-file"
                                                onClick={() => {
                                                    setNewPost(prev => {
                                                        if (prev.url && prev.url.startsWith('blob:')) URL.revokeObjectURL(prev.url);
                                                        return { ...prev, url: '', type: 'text' };
                                                    });
                                                }}
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    )}
                                </div>

                                <div className="form-actions">
                                    {showEditForm ? (
                                        <>
                                            <button className="publish-btn" onClick={handleSaveEditedPost}>Сохранить изменения</button>
                                            <button className="cancel-btn" onClick={handleCancelEdit}>Отмена</button>
                                        </>
                                    ) : (
                                        <button className="publish-btn" onClick={handlePublishPost}>Опубликовать</button>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MainFeed;