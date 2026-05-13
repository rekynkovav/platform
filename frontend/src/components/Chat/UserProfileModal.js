import React, { useState, useEffect } from 'react';
import './UserProfileModal.css';

const UserProfileModal = ({ userId, username, onClose }) => {
    const [userData, setUserData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadUserData();
    }, [userId]);

    const loadUserData = async () => {
        try {
            setLoading(true);
            // В реальном приложении здесь был бы запрос к API
            // Сейчас используем мок-данные
            const savedPhoto = localStorage.getItem('userProfilePhoto');

            // Имитируем задержку сети
            await new Promise(resolve => setTimeout(resolve, 300));

            setUserData({
                username: username || 'Пользователь',
                bio: 'Это тестовый пользователь для демонстрации. Здесь будет информация из раздела "О себе".',
                profilePhoto: savedPhoto || null,
                role: 'USER_0',
                joinDate: '2024-01-01'
            });
        } catch (err) {
            console.error('Ошибка загрузки данных пользователя:', err);
            setError('Не удалось загрузить данные пользователя');
        } finally {
            setLoading(false);
        }
    };

    const getRoleInfo = (role) => {
        const roleMap = {
            'USER_0': { icon: '👁️', name: 'Пользователь 0', color: '#6c757d' },
            'USER_1': { icon: '🌱', name: 'Пользователь 1', color: '#28a745' },
            'USER_2': { icon: '⭐', name: 'Пользователь 2', color: '#ffc107' },
            'MODERATOR_ADMIN': { icon: '🛡️', name: 'Модератор/Админ', color: '#0d6efd' },
            'SUPER_ADMIN': { icon: '🔱', name: 'Главный Админ', color: '#dc3545' },
            'VYACHESLAV': { icon: '👑', name: 'Владелец', color: '#9c27b0' },
            'CONSULTANT_PITANIE': { icon: '🍎', name: 'Консультант по питанию', color: '#4caf50' },
            'CONSULTANT_SPORT': { icon: '🏋️', name: 'Консультант по спорту', color: '#2196f3' },
            'DEVELOPER': { icon: '💻', name: 'Разработчик', color: '#ff9800' },
            'vyacheslav': { icon: '👑', name: 'Вячеслав', color: '#dc3545' }
        };
        const upperRole = role?.toUpperCase();
        return roleMap[upperRole] || { icon: '👤', name: role || 'Пользователь', color: '#6c757d' };
    };

    if (loading) {
        return (
            <div className="user-profile-modal-overlay" onClick={onClose}>
                <div className="user-profile-modal" onClick={e => e.stopPropagation()}>
                    <div className="modal-loading">
                        <div className="loading-spinner"></div>
                        <p>Загрузка профиля...</p>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="user-profile-modal-overlay" onClick={onClose}>
                <div className="user-profile-modal" onClick={e => e.stopPropagation()}>
                    <div className="modal-error">
                        <p>❌ {error}</p>
                        <button className="close-btn" onClick={onClose}>Закрыть</button>
                    </div>
                </div>
            </div>
        );
    }

    const roleInfo = getRoleInfo(userData.role);

    return (
        <div className="user-profile-modal-overlay" onClick={onClose}>
            <div className="user-profile-modal" onClick={e => e.stopPropagation()}>
                <div className="modal-header">
                    <h3>Профиль пользователя</h3>
                    <button className="modal-close-btn" onClick={onClose} title="Закрыть">
                        ✕
                    </button>
                </div>

                <div className="modal-content">
                    <div className="profile-photo-section">
                        <div className="profile-photo-large">
                            {userData.profilePhoto ? (
                                <img
                                    src={userData.profilePhoto}
                                    alt={`Фото ${userData.username}`}
                                    className="profile-photo-img"
                                />
                            ) : (
                                <div className="profile-photo-placeholder-large">
                                    <span className="photo-icon-large">👤</span>
                                </div>
                            )}
                        </div>

                        <div className="profile-info">
                            <h2 className="profile-username">{userData.username}</h2>
                            <div className="profile-role-badge" style={{ background: roleInfo.color }}>
                                <span className="role-icon">{roleInfo.icon}</span>
                                <span className="role-name">{roleInfo.name}</span>
                            </div>
                        </div>
                    </div>

                    <div className="profile-details">
                        <div className="detail-section">
                            <h4>О себе</h4>
                            <div className="bio-content">
                                {userData.bio || 'Пользователь еще не добавил информацию о себе.'}
                            </div>
                        </div>

                        <div className="detail-section">
                            <h4>Информация</h4>
                            <div className="info-grid">
                                <div className="info-item">
                                    <span className="info-label">Роль:</span>
                                    <span className="info-value">{userData.role}</span>
                                </div>
                                <div className="info-item">
                                    <span className="info-label">Дата регистрации:</span>
                                    <span className="info-value">
                                        {new Date(userData.joinDate).toLocaleDateString('ru-RU')}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="close-btn" onClick={onClose}>Закрыть</button>
                </div>
            </div>
        </div>
    );
};

export default UserProfileModal;