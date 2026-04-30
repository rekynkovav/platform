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
        navigator.clipboard.writeText(text);
        alert('Скопировано в буфер обмена!');
    };

    const getRoleInfo = (role) => {
        const roleMap = {
            'user_0': { icon: '👁️', name: 'Пользователь 0', color: '#6c757d' },
            'user_1': { icon: '🌱', name: 'Пользователь 1', color: '#28a745' },
            'user_2': { icon: '⭐', name: 'Пользователь 2', color: '#ffc107' },
            'moderator_admin': { icon: '🛡️', name: 'Модератор/Админ', color: '#0d6efd' },
            'super_admin': { icon: '🔱', name: 'Главный Админ', color: '#dc3545' },
            'consultant_pitanie': { icon: '🥗', name: 'Консультант Питание', color: '#fd7e14' },
            'consultant_sport': { icon: '⚽', name: 'Консультант Спорт', color: '#20c997' },
            'developer': { icon: '💻', name: 'Разработчик', color: '#6f42c1' },
            'vyacheslav': { icon: '👑', name: 'Вячеслав', color: '#dc3545' }
        };
        return roleMap[role?.toLowerCase()] || { icon: '👤', name: role || 'Пользователь', color: '#6c757d' };
    };

    const isPaidRole = (role) => {
        return ['user_1', 'user_2', 'moderator_admin', 'super_admin', 'consultant_pitanie', 'consultant_sport', 'developer', 'vyacheslav'].includes(role?.toLowerCase());
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
                                    bio: user?.bio || ''
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
