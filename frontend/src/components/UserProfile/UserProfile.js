import React, { useState, useEffect } from 'react';
import { userApi, paymentApi } from '../../services/api';
import { getUsername, getUserRoles } from '../../keycloak';
import { usePermissions } from '../../hooks/usePermissions';
import './UserProfile.css';

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [editForm, setEditForm] = useState({});
    const [qrCode, setQrCode] = useState(null);
    const [currentPayment, setCurrentPayment] = useState(null);
    const [error, setError] = useState(null);
    const username = getUsername();
    const userRoles = getUserRoles();

    const { hasLevel } = usePermissions();

    const clearError = () => {
        setError(null);
    };

    useEffect(() => {
        loadUserData();
        loadPayments();
    }, []);

    const loadUserData = async () => {
        try {
            const data = await userApi.getMe();
            setUser(data);
            setEditForm({
                username: data.username,
                email: data.email,
                phone: data.phone || '',
                bio: data.bio || '',
                avatarUrl: data.avatarUrl || ''
            });
            setError(null);
        } catch (error) {
            console.error('Failed to load user:', error);
            setError('Не удалось загрузить данные пользователя');
        } finally {
            setLoading(false);
        }
    };

    const loadPayments = async () => {
        try {
            const data = await paymentApi.getUserPayments();
            setPayments(data || []);
            setError(null);
        } catch (error) {
            console.error('Failed to load payments:', error);
            setError('Не удалось загрузить данные о платежах');
        }
    };

    const handleUpdate = async () => {
        try {
            const updated = await userApi.updateMe(editForm);
            setUser(updated);
            setIsEditing(false);
            setError(null);
            alert('Profile updated successfully');
        } catch (error) {
            console.error('Failed to update profile:', error);
            setError('Не удалось обновить профиль');
        }
    };

    const createPayment = async (months) => {
        try {
            const payment = await paymentApi.create({ subscriptionMonths: months });
            setCurrentPayment(payment);

            // Загрузить QR-код
            const qrResponse = await paymentApi.getPaymentQR(payment.id);
            setQrCode(`data:image/png;base64,${qrResponse.qrCodeBase64}`);

            setError(null);
            alert(`Payment created: ${payment.amount} RUB`);
            loadPayments();
        } catch (error) {
            console.error('Failed to create payment:', error);
            setError('Не удалось создать платеж');
        }
    };

    if (loading) return <div className="loading">Loading...</div>;
    if (!user) return <div className="error">User not found</div>;

    return (
        <div className="user-profile">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="profile-header">
                <h2>Profile: {user.username}</h2>
                <div className="roles">
                    Roles: {userRoles.join(', ')}
                </div>
            </div>

            <div className="profile-info">
                {isEditing ? (
                    <div className="edit-form">
                        <input
                            value={editForm.username}
                            onChange={(e) => setEditForm({...editForm, username: e.target.value})}
                            placeholder="Username"
                        />
                        <input
                            value={editForm.email}
                            onChange={(e) => setEditForm({...editForm, email: e.target.value})}
                            placeholder="Email"
                        />
                        <input
                            value={editForm.phone}
                            onChange={(e) => setEditForm({...editForm, phone: e.target.value})}
                            placeholder="Phone"
                        />
                        <textarea
                            value={editForm.bio}
                            onChange={(e) => setEditForm({...editForm, bio: e.target.value})}
                            placeholder="Bio"
                        />
                        <button onClick={handleUpdate}>Save</button>
                        <button onClick={() => setIsEditing(false)}>Cancel</button>
                    </div>
                ) : (
                    <div className="profile-details">
                        <p><strong>Email:</strong> {user.email}</p>
                        <p><strong>Phone:</strong> {user.phone || 'Not set'}</p>
                        <p><strong>Bio:</strong> {user.bio || 'Not set'}</p>
                        <p><strong>Member since:</strong> {new Date(user.createdAt).toLocaleDateString()}</p>
                        <button onClick={() => setIsEditing(true)}>Edit Profile</button>
                    </div>
                )}
            </div>

            <div className="payments-section">
                <h3>Subscription</h3>
                <div className="payment-options">
                    <button onClick={() => createPayment(1)}>1 month - 1500 RUB</button>
                    <button onClick={() => createPayment(3)}>3 months - 4000 RUB</button>
                    <button onClick={() => createPayment(6)}>6 months - 7500 RUB</button>
                    <button onClick={() => createPayment(12)}>12 months - 14000 RUB</button>
                </div>

                <h4>Payment History</h4>
                {payments.length === 0 ? (
                    <p>No payments yet</p>
                ) : (
                    <table className="payments-table">
                        <thead>
                            <tr><th>Date</th><th>Amount</th><th>Status</th><th>Months</th></tr>
                        </thead>
                        <tbody>
                            {payments.map(p => (
                                <tr key={p.id}>
                                    <td>{new Date(p.createdAt).toLocaleDateString()}</td>
                                    <td>{p.amount} {p.currency}</td>
                                    <td className={`status-${p.status?.toLowerCase()}`}>{p.status}</td>
                                    <td>{p.subscriptionMonths}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* QR Code Modal */}
            {qrCode && currentPayment && (
                <div className="qr-modal">
                    <div className="qr-modal-content">
                        <h3>Оплата подписки</h3>
                        <img src={qrCode} alt="QR-код для оплаты" />
                        <p>Сумма: {currentPayment.amount} RUB</p>
                        <p>Платёж действителен до: {new Date(currentPayment.expiresAt).toLocaleString()}</p>
                        <div className="bank-details">
                            <h4>Реквизиты получателя:</h4>
                            <p>БФАДД</p>
                            <p>ИНН: 9715470089 | КПП: 771501001</p>
                            <p>р/с: 40703810512000002359</p>
                            <p>Банк: ВОЛОГОДСКОЕ ОТДЕЛЕНИЕ N8638 ПАО СБЕРБАНК г. Вологда</p>
                            <p>к/с: 30101810900000000644</p>
                            <p>БИК: 041909644</p>
                        </div>
                        <button onClick={() => setQrCode(null)}>Закрыть</button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default UserProfile;
