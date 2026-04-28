import React, { useState, useEffect } from 'react';
import './UserProfile.css';

const UserProfile = () => {
    const [selectedPeriod, setSelectedPeriod] = useState(1);
    const [paymentId, setPaymentId] = useState(null);
    const [qrCodeUrl, setQrCodeUrl] = useState(null);
    const [loading, setLoading] = useState(false);
    const demoRole = localStorage.getItem('demoRole') || 'user0';

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

    const checkPaymentStatus = () => {
        alert('Проверка статуса платежа...\n\nВ реальной системе здесь будет автоматическая проверка через банковский API.');
    };

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        alert('Скопировано в буфер обмена!');
    };

    const getRoleInfo = () => {
        switch(demoRole) {
            case 'user0': return { icon: '👁️', name: 'Пользователь 0', description: 'Просмотр сайта', color: '#6c757d' };
            case 'user1': return { icon: '🌱', name: 'Пользователь 1', description: 'Новички + Чат 1', color: '#28a745' };
            case 'user2': return { icon: '⭐', name: 'Пользователь 2', description: 'Продвинутые + Чат 2', color: '#ffc107' };
            case 'admin-moderator': return { icon: '🛡️', name: 'Админ (модератор)', description: 'Модерация', color: '#0d6efd' };
            case 'consultant-nutrition': return { icon: '🥗', name: 'Консультант (питание)', description: 'Питание', color: '#0d6efd' };
            case 'consultant-sport': return { icon: '⚽', name: 'Консультант (спорт)', description: 'Спортивный контент', color: '#0d6efd' };
            case 'developer': return { icon: '💻', name: 'Разработчик', description: 'Разработка', color: '#6f42c1' };
            case 'vyacheslav': return { icon: '👑', name: 'Вячеслав', description: 'Владелец', color: '#dc3545' };
            case 'super-admin': return { icon: '🔱', name: 'Главный Админ', description: 'Полный доступ', color: '#dc3545' };
            default: return { icon: '👤', name: 'Пользователь', description: '', color: '#6c757d' };
        }
    };

    const roleInfo = getRoleInfo();
    const needsPayment = demoRole === 'user0';
    const selectedPeriodData = periods.find(p => p.months === selectedPeriod) || periods[0];

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
                        <div className="role-badge-description">{roleInfo.description}</div>
                    </div>
                </div>

                {needsPayment && (
                    <div className="payment-notice">
                        <p>⚠️ Для доступа к полному функционалу необходимо оформить подписку</p>
                    </div>
                )}
            </div>

            {needsPayment && !paymentId && (
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

                    <button
                        className="payment-button"
                        onClick={handlePayment}
                        disabled={loading}
                    >
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
                                <button className="copy-btn" onClick={() => copyToClipboard(paymentId)} title="Скопировать">📋</button>
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
                                <span className="detail-value">{bankDetails.name} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.name)}>📋</button></span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">ИНН:</span>
                                <span className="detail-value">{bankDetails.inn} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.inn)}>📋</button></span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">КПП:</span>
                                <span className="detail-value">{bankDetails.kpp} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.kpp)}>📋</button></span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">Расчетный счет:</span>
                                <span className="detail-value">{bankDetails.account} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.account)}>📋</button></span>
                            </div>
                            <div className="bank-detail-item full-width">
                                <span className="detail-label">Банк:</span>
                                <span className="detail-value">{bankDetails.bank}</span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">Корр. счет:</span>
                                <span className="detail-value">{bankDetails.corrAccount} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.corrAccount)}>📋</button></span>
                            </div>
                            <div className="bank-detail-item">
                                <span className="detail-label">БИК:</span>
                                <span className="detail-value">{bankDetails.bik} <button className="copy-btn-small" onClick={() => copyToClipboard(bankDetails.bik)}>📋</button></span>
                            </div>
                        </div>
                    </div>

                    <div className="payment-actions">
                        <button className="check-payment-btn" onClick={checkPaymentStatus}>🔄 Проверить статус платежа</button>
                        <button className="back-btn" onClick={() => { setPaymentId(null); setQrCodeUrl(null); }}>← Выбрать другой тариф</button>
                    </div>

                    <div className="payment-help">
                        <p>💡 После оплаты статус обновится автоматически в течение 1-2 минут</p>
                        <p>❓ Возникли проблемы? Свяжитесь с поддержкой, указав ID платежа: <strong>{paymentId}</strong></p>
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
