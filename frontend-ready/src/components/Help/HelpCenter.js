// components/Help/HelpCenter.js
import React, { useState, useEffect } from 'react';
import { adminsApi } from '../../services/api';
import HelpChat from './HelpChat';
import './HelpCenter.css';

const HelpCenter = () => {
    const [admins, setAdmins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedAdmin, setSelectedAdmin] = useState(null);
    const [groupChat, setGroupChat] = useState(false);

    useEffect(() => {
        loadAdmins();
    }, []);

    const loadAdmins = async () => {
        try {
            const response = await adminsApi.getList();
            setAdmins(response.data);
        } catch (err) {
            console.error('Failed to load admins:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleGroupChat = () => {
        setGroupChat(true);
        setSelectedAdmin(null);
    };

    const handleAdminSelect = (admin) => {
        setSelectedAdmin(admin);
        setGroupChat(false);
    };

    const handleBack = () => {
        setSelectedAdmin(null);
        setGroupChat(false);
    };

    if (selectedAdmin) {
        return (
            <HelpChat
                recipient={selectedAdmin}
                isGroup={false}
                onBack={handleBack}
            />
        );
    }

    if (groupChat) {
        return (
            <HelpChat
                recipient={{ name: 'Администраторам', id: 'group' }}
                isGroup={true}
                onBack={handleBack}
            />
        );
    }

    return (
        <div className="help-center">
            <div className="help-header">
                <span className="help-icon">🆘</span>
                <h2>Помощь и поддержка</h2>
                <p>Выберите кому хотите написать</p>
            </div>

            <div className="help-options">
                <button className="help-option group" onClick={handleGroupChat}>
                    <div className="help-option-icon">👥</div>
                    <div className="help-option-info">
                        <h3>Администраторам (общий чат)</h3>
                        <p>Задайте вопрос всем администраторам сразу</p>
                        <span className="help-option-time">Среднее время ответа: ~5 минут</span>
                    </div>
                </button>

                {loading ? (
                    <div className="loading">Загрузка администраторов...</div>
                ) : (
                    <div className="admins-list">
                        <h3 className="admins-title">👤 Администраторы</h3>
                        {admins.map(admin => (
                            <button
                                key={admin.id}
                                className="help-option admin"
                                onClick={() => handleAdminSelect(admin)}
                            >
                                <div className="help-option-icon">
                                    {admin.avatar ? (
                                        <img src={admin.avatar} alt={admin.name} />
                                    ) : (
                                        '👤'
                                    )}
                                    <span className={`admin-status ${admin.status}`} />
                                </div>
                                <div className="help-option-info">
                                    <h3>{admin.name}</h3>
                                    <p className="admin-role">{admin.role}</p>
                                    <span className="admin-status-text">
                                        {admin.status === 'online' ? '🟢 Онлайн' : '🟡 Отошёл'}
                                    </span>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default HelpCenter;