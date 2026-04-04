// components/Contacts/ContactsList.js
import React, { useState, useEffect } from 'react';
import { contactsApi } from '../../services/api';
import './ContactsList.css';

const ContactsList = () => {
    const [contacts, setContacts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        loadContacts();
    }, []);

    const loadContacts = async () => {
        try {
            const response = await contactsApi.getAll();
            setContacts(response.data);
        } catch (err) {
            console.error('Failed to load contacts:', err);
        } finally {
            setLoading(false);
        }
    };

    const filteredContacts = contacts.filter(contact =>
        contact.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        contact.username?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="contacts-list">
            <div className="contacts-search">
                <input
                    type="text"
                    placeholder="🔍 Поиск контактов..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="search-input"
                />
            </div>

            {loading ? (
                <div className="loading">Загрузка...</div>
            ) : filteredContacts.length === 0 ? (
                <div className="empty-state">
                    <p>👥 Контакты не найдены</p>
                </div>
            ) : (
                <div className="contacts-grid">
                    {filteredContacts.map(contact => (
                        <div key={contact.id} className="contact-card">
                            <div className="contact-avatar">
                                {contact.avatar ? (
                                    <img src={contact.avatar} alt={contact.name} />
                                ) : (
                                    <span>{contact.name?.[0] || '👤'}</span>
                                )}
                                <span className={`contact-status ${contact.status}`} />
                            </div>
                            <div className="contact-info">
                                <h3 className="contact-name">{contact.name}</h3>
                                <p className="contact-username">@{contact.username}</p>
                                {contact.bio && (
                                    <p className="contact-bio">{contact.bio}</p>
                                )}
                            </div>
                            <button className="contact-message">💬</button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ContactsList;