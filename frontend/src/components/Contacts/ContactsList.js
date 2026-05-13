import React, { useState, useEffect } from 'react';
import { contactsApi } from '../../services/api';
import './ContactsList.css';

const ContactsList = () => {
    const [contacts, setContacts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [search, setSearch] = useState('');

    const loadContacts = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await contactsApi.getAll(search);
            setContacts(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load contacts', err);
            setError('Не удалось загрузить контакты');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadContacts();
    }, [search]);

    if (loading) return <div className="loading">Загрузка контактов...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="contacts-list">
            <h2>👥 Контакты</h2>

            <input
                type="text"
                placeholder="Поиск по имени или роли..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="search-input"
            />

            <div className="contacts-grid">
                {contacts.length === 0 ? (
                    <p className="no-contacts">Контактов пока нет</p>
                ) : (
                    contacts.map(contact => (
                        <div key={contact.id} className="contact-card">
                            <div className="contact-avatar">👤</div>
                            <div className="contact-info">
                                <strong>{contact.username || contact.nickname || 'Пользователь'}</strong>
                                {contact.role && <span className="contact-role">{contact.role}</span>}
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default ContactsList;