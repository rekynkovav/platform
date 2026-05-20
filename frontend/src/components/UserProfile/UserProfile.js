import React, { useState, useEffect } from 'react';
import { usersApi, mediaApi } from '../../services/api';
import './UserProfile.css';

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        phone: '',
        bio: '',
        avatarUrl: ''
    });
    const [saving, setSaving] = useState(false);
    const [uploadingAvatar, setUploadingAvatar] = useState(false);
    const [error, setError] = useState(null);
    const [paymentId, setPaymentId] = useState(null);
    const [qrCodeUrl, setQrCodeUrl] = useState(null);
    const [showPayment, setShowPayment] = useState(false);

    const periods = [
        { months: 1, price: 10000, label: '1 месяц', pricePerMonth: 10000 },
        { months: 3, price: 27000, label: '3 месяца', discount: '10%', pricePerMonth: 9000, savings: 3000 },
        { months: 6, price: 48000, label: '6 месяцев', discount: '20%', pricePerMonth: 8000, savings: 12000 },
        { months: 12, price: 84000, label: '12 месяцев', discount: '30%', pricePerMonth: 7000, savings: 36000 }
    ];

    const [selectedPeriod, setSelectedPeriod] = useState(1);

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

            // === ВРЕМЕННЫЙ ФИКС ДЛЯ USER_0 ===
            const demoRole = localStorage.getItem('demoRole');
            const isUser0Demo = demoRole === 'USER_0' &&
                               (process.env.REACT_APP_DEMO_MODE === 'true' ||
                                window.location.hostname === 'localhost');

            if (isUser0Demo) {
                // МОК-ДАННЫЕ ДЛЯ USER_0
                const mockUser0 = {
                    id: 'user-0-demo-id',
                    username: 'Пользователь 0 (Демо)',
                    email: 'user0@demo.platform.ru',
                    phone: '+7 (999) 000-00-00',
                    role: 'USER_0',
                    avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user0&backgroundColor=4a90e2&hairColor=2c1b18&accessories=round&facialHair=beardLight',
                    bio: 'Демонстрационный аккаунт для показа функционала личного кабинета',
                    banned: false,
                    banReason: null,
                    lastActiveAt: new Date().toISOString(),
                    createdAt: '2024-01-01T00:00:00Z',
                    // Дополнительные поля для демо
                    subscriptionActive: true,
                    subscriptionEnd: '2024-12-31T23:59:59Z',
                    paymentMethod: 'bank_transfer',
                    notificationsEnabled: true
                };

                setUser(mockUser0);
                setFormData({
                    username: mockUser0.username,
                    email: mockUser0.email,
                    phone: mockUser0.phone,
                    bio: mockUser0.bio,
                    avatarUrl: mockUser0.avatarUrl
                });
                setError(null);
                setLoading(false);
                return;
            }
            // === КОНЕЦ ВРЕМЕННОГО ФИКСА ===

            // Обычная загрузка для остальных
            const response = await usersApi.getCurrentUser();
            setUser(response.data);
            setFormData({
                username: response.data.username || '',
                email: response.data.email || '',
                phone: response.data.phone || '',
                bio: response.data.bio || '',
                avatarUrl: response.data.avatarUrl || ''
            });
            setError(null);
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

            // === ВРЕМЕННЫЙ ФИКС ДЛЯ USER_0 ===
            const demoRole = localStorage.getItem('demoRole');
            const isUser0Demo = demoRole === 'USER_0' &&
                               (process.env.REACT_APP_DEMO_MODE === 'true' ||
                                window.location.hostname === 'localhost');

            if (isUser0Demo) {
                // Мок-сохранение для USER_0
                const updatedUser = {
                    ...user,
                    username: formData.username,
                    email: formData.email,
                    phone: formData.phone,
                    bio: formData.bio,
                    avatarUrl: formData.avatarUrl
                };
                setUser(updatedUser);
                setEditing(false);
                alert('Демо-профиль USER_0 обновлен (изменения не сохраняются в БД)');
                setSaving(false);
                return;
            }
            // === КОНЕЦ ВРЕМЕННОГО ФИКСА ===

            // Обычное сохранение для остальных
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

    const handleAvatarUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Проверка типа файла
        if (!file.type.startsWith('image/')) {
            alert('Пожалуйста, выберите изображение (JPG, PNG, GIF)');
            return;
        }

        // Проверка размера файла (макс 5MB)
        if (file.size > 5 * 1024 * 1024) {
            alert('Размер файла не должен превышать 5MB');
            return;
        }

        try {
            setUploadingAvatar(true);

            // Загрузка файла на сервер
            const response = await mediaApi.upload(file, 'image');
            const mediaFile = response.data;

            // Получаем stream URL для отображения
            const streamResponse = await mediaApi.getStreamUrl(mediaFile.id);
            const avatarUrl = streamResponse.data.url || `/api/media/stream/${mediaFile.id}`;

            // Обновляем formData с новым avatarUrl
            setFormData({ ...formData, avatarUrl });

            // Если не в режиме редактирования, сразу обновляем пользователя
            if (!editing) {
                const updatedUser = { ...user, avatarUrl };
                setUser(updatedUser);
            }

            alert('Аватар успешно загружен!');
        } catch (err) {
            console.error('Ошибка загрузки аватара:', err);
            alert('Не удалось загрузить аватар');
        } finally {
            setUploadingAvatar(false);
            // Сбрасываем input file
            e.target.value = '';
        }
    };

    const handlePayment = async () => {
        setLoading(true);
        try {
            const newPaymentId = `PAY-${Date.now()}-${Math.random().toString(36).substr(2, 9).toUpperCase()}`;
            setPaymentId(newPaymentId);

            const selectedPeriodData = periods.find(p => p.months === selectedPeriod);
            const amount = selectedPeriodData.price;
            const sbpData = generateSBPQRData(amount, newPaymentId);
            const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(sbpData)}`;
            setQrCodeUrl(qrUrl);

            localStorage.setItem('pendingPayment', JSON.stringify({
                paymentId: newPaymentId,
                amount: amount,
                period: selectedPeriod,
                createdAt: new Date().toISOString()
            }));
        } catch (error) {
            console.error('Ошибка создания платежа:', error);
            alert('Ошибка при создании платежа. Попробуйте еще раз.');
        } finally {
            setLoading(false);
        }
    };

    const generateSBPQRData = (amount, paymentId) => {
        const bankAccount = bankDetails.account;
        const recipientName = bankDetails.name;
        const purpose = `Подписка ${paymentId}`;
        return `ST00012|Name=${recipientName}|PersonalAcc=${bankAccount}|BankName=${bankDetails.bank}|BIC=${bankDetails.bik}|CorrespAcc=${bankDetails.corrAccount}|Sum=${amount * 100}|Purpose=${purpose}|PayeeINN=${bankDetails.inn}|KPP=${bankDetails.kpp}`;
    };

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text).catch(() => {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        });
        alert('Скопировано в буфер обмена!');
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
        return roleMap[role?.toLowerCase()] || { icon: '👤', name: role || 'Пользователь', color: '#6c757d' };
    };

    const isPaidRole = (role) => {
        return ['USER_1', 'USER_2', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'DEVELOPER', 'VYACHESLAV'].includes(role?.toUpperCase());
    };

    const selectedPeriodData = periods.find(p => p.months === selectedPeriod) || periods[0];

    if (loading && !user) {
        return <div className="user-profile"><div className="loading">Загрузка...</div></div>;
    }

    if (error) {
        return <div className="user-profile"><div className="error">{error}</div></div>;
    }

    const roleInfo = getRoleInfo(user?.role);
    const needsPayment = !isPaidRole(user?.role);

    return (
        <div className="user-profile">
            <div className="profile-header">
                <h2>👤 Личный кабинет</h2>
            </div>

            <div className="profile-card">
                <div className="profile-avatar-section">
                    <div className="profile-avatar-container">
                        {user?.avatarUrl ? (
                            <img
                                src={user.avatarUrl}
                                alt="Аватар пользователя"
                                className="profile-avatar"
                                onError={(e) => {
                                    e.target.style.display = 'none';
                                    e.target.parentElement.querySelector('.profile-avatar-placeholder').style.display = 'flex';
                                }}
                            />
                        ) : null}
                        <div className={`profile-avatar-placeholder ${user?.avatarUrl ? 'hidden' : ''}`}>
                            <span className="avatar-icon">👤</span>
                        </div>
                    </div>
                    {editing && (
                        <div className="avatar-upload-controls">
                            <label className="avatar-upload-btn">
                                {uploadingAvatar ? 'Загрузка...' : '📷 Сменить аватар'}
                                <input
                                    type="file"
                                    accept="image/*"
                                    onChange={handleAvatarUpload}
                                    style={{ display: 'none' }}
                                    disabled={uploadingAvatar}
                                />
                            </label>
                            {formData.avatarUrl && formData.avatarUrl !== user?.avatarUrl && (
                                <button
                                    className="avatar-remove-btn"
                                    onClick={() => setFormData({ ...formData, avatarUrl: '' })}
                                >
                                    ❌ Удалить
                                </button>
                            )}
                        </div>
                    )}
                </div>

                <div className="profile-role-badge" style={{ background: roleInfo.color }}>
                    <span className="role-badge-icon">{roleInfo.icon}</span>
                    <div className="role-badge-info">
                        <div className="role-badge-name">{roleInfo.name}</div>
                        <div className="role-badge-description">Роль: {user?.role || 'user'}</div>
                    </div>
                </div>

                {needsPayment && (
                    <div className="payment-notice">
                        <p>⚠️ Для доступа к полному функционалу необходимо оформить подписку</p>
                    </div>
                )}
            </div>

            {!showPayment && (
                <div className="profile-edit-section">
                    <div className="section-header">
                        <h3>📝 Профиль</h3>
                        {!editing && (
                            <button className="edit-btn" onClick={() => setEditing(true)}>Редактировать</button>
                        )}
                    </div>

                    <div className="profile-fields">
                        <div className="profile-field">
                            <label>Никнейм:</label>
                            {editing ? (
                                <input
                                    type="text"
                                    value={formData.username}
                                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                                />
                            ) : (
                                <span>{user?.username || 'Не указан'}</span>
                            )}
                        </div>

                        <div className="profile-field">
                            <label>Email:</label>
                            {editing ? (
                                <input
                                    type="email"
                                    value={formData.email}
                                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                />
                            ) : (
                                <span>{user?.email || 'Не указан'}</span>
                            )}
                        </div>

                        <div className="profile-field">
                            <label>Телефон:</label>
                            {editing ? (
                                <input
                                    type="tel"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                />
                            ) : (
                                <span>{user?.phone || 'Не указан'}</span>
                            )}
                        </div>

                        <div className="profile-field bio-field">
                            <label>О себе:</label>
                            {editing ? (
                                <textarea
                                    value={formData.bio}
                                    onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
                                    rows={4}
                                />
                            ) : (
                                <span>{user?.bio || 'Ничего не рассказал о себе'}</span>
                            )}
                        </div>


                        <div className="profile-field">
                            <label>Дата регистрации:</label>
                            <span>{user?.createdAt ? new Date(user.createdAt).toLocaleDateString('ru-RU') : '-'}</span>
                        </div>

                        <div className="profile-field">
                            <label>Последняя активность:</label>
                            <span>{user?.lastActiveAt ? new Date(user.lastActiveAt).toLocaleString('ru-RU') : '-'}</span>
                        </div>
                    </div>

                    {editing && (
                        <div className="edit-actions">
                            <button className="save-btn" onClick={handleSave} disabled={saving}>
                                {saving ? 'Сохранение...' : '💾 Сохранить'}
                            </button>
                            <button className="cancel-btn" onClick={() => {
                                setEditing(false);
                                setFormData({
                                    username: user?.username || '',
                                    email: user?.email || '',
                                    phone: user?.phone || '',
                                    bio: user?.bio || '',
                                    avatarUrl: user?.avatarUrl || ''
                                });
                            }}>
                                Отмена
                            </button>
                        </div>
                    )}
                </div>
            )}

            {needsPayment && !showPayment && !paymentId && (
                <div className="payment-section">
                    <h3>💳 Оформление подписки</h3>
                    <p className="payment-description">Выберите период подписки</p>

                    <div className="periods-grid">
                        {periods.map((period) => (
                            <div
                                key={period.months}
                                className={`period-card ${selectedPeriod === period.months ? 'selected' : ''}`}
                                onClick={() => setSelectedPeriod(period.months)}
                            >
                                {period.discount && (
                                    <div className="period-discount">-{period.discount}</div>
                                )}
                                <div className="period-label">{period.label}</div>
                                <div className="period-price">{period.price.toLocaleString('ru-RU')} ₽</div>
                                {period.discount && (
                                    <>
                                        <div className="period-per-month">{period.pricePerMonth.toLocaleString('ru-RU')} ₽/мес</div>
                                        <div className="period-savings">Экономия {period.savings.toLocaleString('ru-RU')} ₽</div>
                                    </>
                                )}
                            </div>
                        ))}
                    </div>

                    <button className="payment-button" onClick={handlePayment}>
                        {loading ? '⏳ Генерация QR-кода...' : `💳 Оплатить ${selectedPeriodData.price.toLocaleString('ru-RU')} ₽`}
                    </button>
                </div>
            )}

            {needsPayment && paymentId && (
                <div className="payment-qr-section">
                    <h3>📱 Оплата через СБП</h3>

                    <div className="payment-info-box">
                        <div className="payment-info-item">
                            <span className="info-label">Сумма к оплате:</span>
                            <span className="info-value">{selectedPeriodData.price.toLocaleString('ru-RU')} ₽</span>
                        </div>
                        <div className="payment-info-item">
                            <span className="info-label">Период:</span>
                            <span className="info-value">{selectedPeriodData.label}</span>
                        </div>
                        <div className="payment-info-item">
                            <span className="info-label">ID платежа:</span>
                            <span className="info-value payment-id">
                                {paymentId}
                                <button className="copy-btn" onClick={() => copyToClipboard(paymentId)} title="Копировать">📋</button>
                            </span>
                        </div>
                    </div>

                    <div className="qr-code-container">
                        <h4>Отсканируйте QR-код в приложении банка:</h4>
                        {qrCodeUrl ? (
                            <img src={qrCodeUrl} alt="QR-код для оплаты" className="qr-code-image" />
                        ) : (
                            <div className="qr-code-placeholder">
                                <span className="qr-icon">⏳</span>
                                <p>Генерация QR-кода...</p>
                            </div>
                        )}
                        <p className="qr-instruction">
                            1. Откройте приложение вашего банка<br/>
                            2. Найдите раздел "Оплата по QR"<br/>
                            3. Отсканируйте код выше<br/>
                            4. Подтвердите платеж
                        </p>
                    </div>

                    <div className="bank-details-section">
                        <h4>🏦 Или переведите по реквизитам:</h4>
                        <p className="bank-details-note">
                            ⚠️ ВАЖНО: В назначении платежа обязательно укажите: <strong>{paymentId}</strong>
                        </p>

                        <div className="bank-details-grid">
                            <div className="bank-detail-item">
                                <span className="detail-label">Получатель:</span>
                                <span className="detail-value">{bankDetails.name}</span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">ИНН:</span>
                                <span className="detail-value">{bankDetails.inn}</span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">КПП:</span>
                                <span className="detail-value">{bankDetails.kpp}</span>
                            </div>
                            <div className="bank-detail-item full-width">
                                <span className="detail-label">Расчетный счет:</span>
                                <span className="detail-value">{bankDetails.account}</span>
                            </div>
                            <div className="bank-detail-item full-width">
                                <span className="detail-label">Банк:</span>
                                <span className="detail-value">{bankDetails.bank}</span>
                            </div>
                        </div>
                    </div>

                    <div className="payment-actions">
                        <button className="back-btn" onClick={() => { setPaymentId(null); setQrCodeUrl(null); }}>
                            ← Выбрать другой тариф
                        </button>
                    </div>
                </div>
            )}

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
