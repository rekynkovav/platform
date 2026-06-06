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
            type: post