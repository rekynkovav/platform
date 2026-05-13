import React, { useState, useEffect } from 'react';
import { usersApi } from '../../services/api';
import './UserProfile.css';

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        phone: '',
        bio: ''
    });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    // Платежная часть
    const [paymentId, setPaymentId] = useState(null);
    const [qrCodeUrl, setQrCodeUrl] = useState(null);
    const [selectedPeriod, setSelectedPeriod] = useState(1);

    const periods = [
        { months: 1, price: 10000, label: '1 месяц', pricePerMonth: 10000 },
        { months: 3, price: 27000, label: '3 месяца', discount: '10%', pricePerMonth: 9000, savings: 3000 },
        { months: 6, price: 48000, label: '6 месяцев', discount: '20%', pricePerMonth: 8000, savings: 12000 },
        { months: 12, price: 84000, label: '12 месяцев', discount: '30%', pricePerMonth: 7000, savings: 36000 }
    ];

    const bankDetails = {
        name: 'БФАДД',
        inn: '9715470089',
        kpp: '771501001',
        account: '40703810512000002359',
        bank: 'ВОЛОГОДСКОЕ ОТДЕЛЕНИЕ N8638 ПАО СБЕРБАНК г. Вологда',
        corrAccount: '30101810900000000644',
        bik: '041909644'
    };

    useEffect(() => {
        loadUser();
    }, []);

    const loadUser = async () => {
        try {
            setLoading(true);
            const response = await usersApi.getCurrentUser();
            setUser(response.data);
            setFormData({
                username: response.data.username || '',
                email: response.data.email || '',
                phone: response.data.phone || '',
                bio: response.data.bio || ''
            });
        } catch (err) {
            console.error('Ошибка загрузки пользователя:', err);
            setError('Не удалось загрузить профиль');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        try {
            setSaving(true);
            const response = await usersApi.updateCurrentUser(formData);
            setUser(response.data);
            setEditing(false);
        } catch (err) {
            console.error('Ошибка сохранения:', err);
            alert('Не удалось сохранить профиль');
        } finally {
            setSaving(false);
        }
    };

    const getRoleInfo = (role) => {
        const roleMap = {
            'USER_0': { icon: '👁️', name: 'Пользователь 0', color: '#6c757d' },
            'USER_1': { icon: '🌱', name: 'Пользователь 1', color: '#28a745' },
            'USER_2': { icon: '⭐', name: 'Пользователь 2', color: '#ffc107' },
            'MODERATOR_ADMIN': { icon: '🛡️', name: 'Модератор/Админ', color: '#0d6efd' },
            'SUPER_ADMIN': { icon: '🔱', name: 'Главный Админ', color: '#dc3545' },
            'CONSULTANT_PITANIE': { icon: '🥗', name: 'Консультант Питание', color: '#fd7e14' },
            'CONSULTANT_SPORT': { icon: '⚽', name: 'Консультант Спорт', color: '#20c997' },
            'DEVELOPER': { icon: '💻', name: 'Разработчик', color: '#6f42c1' },
            'VYACHESLAV': { icon: '👑', name: 'Вячеслав', color: '#dc3545' }
        };
        return roleMap[role] || { icon: '👤', name: role || 'Пользователь', color: '#6c757d' };
    };

    const isPaidRole = (role) => {
        return ['USER_1', 'USER_2', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'DEVELOPER', 'VYACHESLAV'].includes(role);
    };

    const roleInfo = getRoleInfo(user?.role);
    const needsPayment = !isPaidRole(user?.role);
    const selectedPeriodData = periods.find(p => p.months === selectedPeriod) || periods[0];

    if (loading && !user) return <div className="user-profile"><div className="loading">Загрузка...</div></div>;
    if (error) return <div className="user-profile"><div className="error-message">{error}</div></div>;

    return (
        <div className="user-profile">
            <div className="profile-header">
                <h2>👤 Личный кабинет</h2>
            </div>

            <div className="profile-card">
                <div className="profile-role-badge" style={{ background: roleInfo.color }}>
                    <span className="role-badge-icon">{roleInfo.icon}</span>
                    <div className="role-badge-info">
                        <div className="role-badge-name">{roleInfo.name}</div>
                    </div>
                </div>

                {needsPayment && (
                    <div className="payment-notice">
                        <p>⚠️ Для доступа к полному функционалу необходимо оформить подписку</p>
                    </div>
                )}
            </div>

            {!needsPayment && (
                <div className="subscription-active">
                    <div className="active-icon">✅</div>
                    <h3>Подписка активна</h3>
                    <p>У вас есть полный доступ ко всем функциям платформы</p>
                </div>
            )}
        </div>
    );
};

export default UserProfile;