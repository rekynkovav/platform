import React, { useState, useEffect, useRef, useCallback } from 'react';
import { feedApi } from '../../services/api';
import useAudioRecorder from '../../hooks/useAudioRecorder';
import { usePermissions } from '../../hooks/usePermissions';
import ConfirmModal from '../common/ConfirmModal';
import usePostAudioControls from '../../hooks/usePostAudioControls';
import fallbackPosts from '../../constants/fallbackPosts';
import './MainFeed.css';

const MainFeed = () => {
    const [posts, setPosts] = useState([]);
    const [newPost, setNewPost] = useState({ type: 'text', title: '', content: '', url: '' });
    const [showAddForm, setShowAddForm] = useState(false);
    const [error, setError] = useState(null);
    const feedEndRef = useRef(null);
    const addFormRef = useRef(null);
    const [activeAudioPostId, setActiveAudioPostId] = useState(null);
    const blobUrlsRef = useRef([]);

    // Состояния для пагинации
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDelete, setPostToDelete] = useState(null);

    // Состояния для редактирования
    const [editingPost, setEditingPost] = useState(null);
    const [showEditForm, setShowEditForm] = useState(false);

    // Хук записи аудио
    const {
        isRecording,
        recordingTime,
        audioBlobUrl,
        startRecording,
        stopRecording,
        resetRecording
    } = useAudioRecorder();

    const { hasLevel } = usePermissions();
    const isAdmin = hasLevel(6);
    const isSuperAdmin = hasLevel(8);

    // Хук управления плеером в посте
    const {
        handlePlayAudio,
        handleSeek,
        handleVolumeChangeLocal,
        handleSpeedChangeLocal,
        getCurrentTimeForPost,
        getDurationForPost,
        isThisPlaying,
        stopTrack,
        volume,
        playbackRate,
        formatTime,
        currentTrack
    } = usePostAudioControls();

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

    // Cleanup blob URLs при размонтировании
    useEffect(() => {
        return () => {
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    // Загрузка постов с пагинацией
    const loadPosts = useCallback(async (reset = false) => {
        try {
            if (reset) {
                setLoadingMore(true);
                const data = await feedApi.getMainFeed(0, 20);
                setPosts(data.content || data);
                setPage(1);
                setHasMore(data.totalPages ? data.totalPages > 1 : data.length >= 20);
                setLoadingMore(false);
            } else {
                setLoadingMore(true);
                const data = await feedApi.getMainFeed(page, 20);
                const newPosts = data.content || data;
                setPosts(prev => [...prev, ...newPosts]);
                setPage(prev => prev + 1);
                setHasMore(data.totalPages ? data.totalPages > page + 1 : newPosts.length >= 20);
                setLoadingMore(false);
            }
        } catch (err) {
            console.error('Failed to load feed:', err);
            setError('Не удалось загрузить ленту');
            if (reset) {
                setPosts(fallbackPosts);
                setHasMore(false);
            }
        }
    }, [page]);

    useEffect(() => {
        loadPosts(true);
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
        if (!isAdmin) {
            alert('Только администратор может добавлять записи');
            return;
        }

        setShowAddForm(true);
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

    // Обработчик записи аудио через хук
    const handleStartRecording = async () => {
        try {
            await startRecording();
        } catch (err) {
            setError('Не удалось получить доступ к микрофону');
        }
    };

    const handleStopRecording = () => {
        stopRecording();
    };

    // Когда хук создаёт blob URL, используем его
    useEffect(() => {
        if (audioBlobUrl) {
            blobUrlsRef.current.push(audioBlobUrl);
            setNewPost(prev => ({
                ...prev,
                type: 'audio',
                url: audioBlobUrl,
                title: prev.title || `Аудиозапись ${new Date().toLocaleString()}`
            }));
        }
    }, [audioBlobUrl]);

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handlePublishPost = async () => {
        if (!newPost.title || (!newPost.content && newPost.type === 'text')) {
            alert('Заполните заголовок и содержание');
            return;
        }

        try {
            const postData = {
                type: newPost.type,
                title: newPost.title,
                content: newPost.content,
                url: newPost.url || '',
                date: new Date().toISOString().split('T')[0],
                author: 'Admin'
            };

            const newPostData = await feedApi.createPost(postData);
            setPosts(prev => [...prev, newPostData]);

            setNewPost({ type: 'text', title: '', content: '', url: '' });
            setShowAddForm(false);
            resetRecording();

            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];

            setTimeout(() => {
                if (feedEndRef.current) {
                    feedEndRef.current.scrollIntoView({ behavior: 'smooth' });
                }
            }, 100);
        } catch (error) {
            console.error('Ошибка при создании поста:', error);
            alert('Не удалось создать пост. Попробуйте снова.');
        }
    };

    const confirmDeletePost = (post) => {
        setPostToDelete(post);
        setShowDeleteConfirm(true);
    };

    const executeDeletePost = async () => {
        if (!postToDelete) return;

        if (!isAdmin) {
            alert('Только администратор может удалять записи!');
            setShowDeleteConfirm(false);
            setPostToDelete(null);
            return;
        }

        try {
            await feedApi.deletePost(postToDelete.id);
            setPosts(prev => prev.filter(post => post.id !== postToDelete.id));

            if (currentTrack?.id === postToDelete.id) {
                stopTrack();
            }

            setShowDeleteConfirm(false);
            setPostToDelete(null);
        } catch (error) {
            console.error('Ошибка при удалении поста:', error);
            alert('Не удалось удалить пост. Попробуйте снова.');
        }
    };

    const cancelDeletePost = () => {
        setShowDeleteConfirm(false);
        setPostToDelete(null);
    };

    const handleEditPost = (post) => {
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

    const handleSaveEditedPost = async () => {
        if (!newPost.title || (!newPost.content && newPost.type === 'text')) {
            alert('Заполните заголовок и содержание');
            return;
        }

        try {
            const updatedPostData = {
                type: newPost.type,
                title: newPost.title,
                content: newPost.content,
                url: newPost.url || '',
                editedAt: new Date().toISOString()
            };

            const updatedPost = await feedApi.updatePost(editingPost.id, updatedPostData);
            setPosts(prev => prev.map(post =>
                post.id === editingPost.id ? updatedPost : post
            ));

            setNewPost({ type: 'text', title: '', content: '', url: '' });
            setShowEditForm(false);
            setEditingPost(null);
            resetRecording();
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        } catch (err) {
            console.error('Failed to update post:', err);
            alert('Не удалось обновить запись');
        }
    };

    const handleCancelEdit = () => {
        setShowEditForm(false);
        setEditingPost(null);
        setNewPost({ type: 'text', title: '', content: '', url: '' });
        resetRecording();
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];
    };

    const handleDownloadPost = async (post) => {
        if (!isSuperAdmin) {
            alert('Только главный админ может скачивать материалы');
            return;
        }

        try {
            if (post.type === 'audio' || post.type === 'video') {
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
            setError('Не удалось скачать файл');
        }
    };

    const clearError = () => {
        setError(null);
    };

    const onPlayAudio = (post) => {
        handlePlayAudio(post, activeAudioPostId, setActiveAudioPostId);
    };

    // Вспомогательная функция для очистки blob URLs при отмене добавления
    const cancelAddPost = () => {
        setShowAddForm(false);
        setNewPost({ type: 'text', title: '', content: '', url: '' });
        resetRecording();
        // Очищаем blob URLs, созданные при предварительном просмотре или записи
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];
    };

    return (
        <div className="main-feed">
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <ConfirmModal
                isOpen={showDeleteConfirm}
                title="Подтверждение удаления"
                message="Вы действительно хотите удалить публикацию?"
                itemLabel={postToDelete?.title || ''}
                onConfirm={executeDeletePost}
                onCancel={cancelDeletePost}
            />

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
                                                <button className="inline-play" onClick={() => onPlayAudio(post)}>
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
                                        <button className="play-audio-btn" onClick={() => onPlayAudio(post)}>
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
                </div>

                {isAdmin && (
                    <div className="admin-panel" ref={addFormRef}>
                        <button className="add-post-btn" onClick={handleAddPost}>
                            ➕ Добавить запись
                        </button>
                    </div>
                )}

                {showAddForm && (
                    <div className="add-post-form">
                        <h3>Новая запись</h3>
                        <input
                            type="text"
                            placeholder="Заголовок"
                            value={newPost.title}
                            onChange={(e) => setNewPost({ ...newPost, title: e.target.value })}
                        />
                        <textarea
                            placeholder="Текст (для аудио/видео описание)"
                            value={newPost.content}
                            onChange={(e) => setNewPost({ ...newPost, content: e.target.value })}
                        />
                        <div className="media-upload-section">
                            <label className="file-upload-label">
                                📁 Загрузить файл
                                <input
                                    type="file"
                                    accept="audio/*,video/*,image/*"
                                    onChange={handleFileUpload}
                                    style={{ display: 'none' }}
                                />
                            </label>
                            <button
                                className={`mic-btn ${isRecording ? 'recording' : ''}`}
                                onClick={isRecording ? handleStopRecording : handleStartRecording}
                            >
                                {isRecording ? '⏹️ Остановить запись' : '🎤 Записать аудио'}
                            </button>
                        </div>
                        {isRecording && (
                            <div className="recording-indicator">
                                <span className="recording-dot">🔴</span>
                                <span className="recording-time">{formatRecordingTime(recordingTime)}</span>
                            </div>
                        )}
                        {newPost.url && (
                            <div className="attached-file">
                                <span className="file-info">
                                    {newPost.type === 'audio' ? '🎵 Аудио' : newPost.type === 'video' ? '🎬 Видео' : '📷 Фото'} прикреплено
                                </span>
                            </div>
                        )}
                        <div className="form-buttons">
                            <button className="publish-btn" onClick={handlePublishPost}>Опубликовать</button>
                            <button className="cancel-btn" onClick={cancelAddPost}>Отмена</button>
                        </div>
                    </div>
                )}

                {showEditForm && (
                    <div className="edit-post-form">
                        <h3>Редактировать запись</h3>
                        <input
                            type="text"
                            placeholder="Заголовок"
                            value={newPost.title}
                            onChange={(e) => setNewPost({ ...newPost, title: e.target.value })}
                        />
                        <textarea
                            placeholder="Текст"
                            value={newPost.content}
                            onChange={(e) => setNewPost({ ...newPost, content: e.target.value })}
                        />
                        <div className="media-upload-section">
                            <label className="file-upload-label">
                                📁 Заменить файл
                                <input
                                    type="file"
                                    accept="audio/*,video/*,image/*"
                                    onChange={handleFileUpload}
                                    style={{ display: 'none' }}
                                />
                            </label>
                            <button
                                className={`mic-btn ${isRecording ? 'recording' : ''}`}
                                onClick={isRecording ? handleStopRecording : handleStartRecording}
                            >
                                {isRecording ? '⏹️ Остановить запись' : '🎤 Записать аудио'}
                            </button>
                        </div>
                        {newPost.url && (
                            <div className="attached-file">
                                <span className="file-info">
                                    {newPost.type === 'audio' ? '🎵 Аудио' : newPost.type === 'video' ? '🎬 Видео' : '📷 Фото'} прикреплено
                                </span>
                            </div>
                        )}
                        <div className="form-buttons">
                            <button className="save-btn" onClick={handleSaveEditedPost}>Сохранить</button>
                            <button className="cancel-btn" onClick={handleCancelEdit}>Отмена</button>
                        </div>
                    </div>
                )}

                <div ref={feedEndRef} />
            </div>
        </div>
    );
};

export default MainFeed;
