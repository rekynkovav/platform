import React, { useState } from 'react';
import './HelpCenter.css';

const HelpCenter = () => {
    const [activeSection, setActiveSection] = useState('faq');
    const [question, setQuestion] = useState('');
    const [answer, setAnswer] = useState('');

    const contacts = [
        {
            category: '🍎 По вопросам питания',
            contacts: [
                { name: 'Анна Иванова', role: 'Диетолог', phone: '+7 (999) 123-45-67', email: 'anna@example.com', telegram: '@anna_diet' }
            ]
        },
        {
            category: '💪 По физической подготовке',
            contacts: [
                { name: 'Сергей Петров', role: 'Тренер', phone: '+7 (999) 234-56-78', email: 'sergey@example.com', telegram: '@sergey_coach' }
            ]
        },
        {
            category: '👤 Лично Тимошенко',
            contacts: [
                { name: 'Тимошенко Александр', role: 'Руководитель', phone: '+7 (999) 345-67-89', email: 'timoshenko@example.com', telegram: '@timoshenko' }
            ]
        }
    ];

    const faqItems = [
        {
            question: 'Как зарегистрироваться на событие?',
            answer: 'Перейдите во вкладку "События", выберите интересующее событие и нажмите кнопку "Записаться".'
        },
        {
            question: 'Где найти расписание тренировок?',
            answer: 'Расписание тренировок доступно во вкладке "События" в разделе "Физическая подготовка".'
        },
        {
            question: 'Как связаться с администратором?',
            answer: 'Используйте контакты выше или напишите в общий чат во вкладке "Чат".'
        },
        {
            question: 'Как изменить тему оформления?',
            answer: 'Нажмите на кнопку с иконкой луны/солнца в правом верхнем углу.'
        }
    ];

    const handleAskQuestion = () => {
        if (question.trim()) {
            // Здесь будет отправка вопроса в общий чат
            alert(`Ваш вопрос отправлен: "${question}"\nОтвет будет дан в ближайшее время в общем чате.`);
            setQuestion('');
            setAnswer('Спасибо за вопрос! Ответ появится в общем чате.');
            setTimeout(() => setAnswer(''), 3000);
        }
    };

    return (
        <div className="help-center">
            <div className="help-header">
                <h2>🆘 Центр помощи</h2>
                <p>Здесь вы можете найти ответы на вопросы и связаться с нужными специалистами</p>
            </div>

            <div className="help-tabs">
                <button
                    className={`help-tab ${activeSection === 'faq' ? 'active' : ''}`}
                    onClick={() => setActiveSection('faq')}
                >
                    ❓ Частые вопросы
                </button>
                <button
                    className={`help-tab ${activeSection === 'contacts' ? 'active' : ''}`}
                    onClick={() => setActiveSection('contacts')}
                >
                    📞 Контакты
                </button>
                <button
                    className={`help-tab ${activeSection === 'ask' ? 'active' : ''}`}
                    onClick={() => setActiveSection('ask')}
                >
                    💬 Задать вопрос
                </button>
            </div>

            <div className="help-content">
                {activeSection === 'faq' && (
                    <div className="faq-section">
                        <h3>Часто задаваемые вопросы</h3>
                        <div className="faq-list">
                            {faqItems.map((item, index) => (
                                <div key={index} className="faq-item">
                                    <div className="faq-question">
                                        <strong>❓ {item.question}</strong>
                                    </div>
                                    <div className="faq-answer">
                                        <strong>Ответ:</strong> {item.answer}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {activeSection === 'contacts' && (
                    <div className="contacts-section">
                        <h3>📋 Контакты специалистов</h3>
                        {contacts.map((category, idx) => (
                            <div key={idx} className="contact-category">
                                <h4>{category.category}</h4>
                                <div className="contact-cards">
                                    {category.contacts.map((contact, cidx) => (
                                        <div key={cidx} className="contact-card">
                                            <div className="contact-name">{contact.name}</div>
                                            <div className="contact-role">{contact.role}</div>
                                            <div className="contact-details">
                                                <div>📞 {contact.phone}</div>
                                                <div>✉️ {contact.email}</div>
                                                <div>💬 {contact.telegram}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {activeSection === 'ask' && (
                    <div className="ask-section">
                        <h3>💬 Общий топик вопросов</h3>
                        <div className="ask-description">
                            <p>Здесь вам ответят по существу на любой ваш вопрос!</p>
                            <p>Напишите свой вопрос, и специалисты свяжутся с вами в ближайшее время.</p>
                        </div>

                        {answer && (
                            <div className="answer-message">
                                {answer}
                            </div>
                        )}

                        <div className="ask-form">
                            <textarea
                                className="question-input"
                                placeholder="Введите ваш вопрос здесь..."
                                value={question}
                                onChange={(e) => setQuestion(e.target.value)}
                                rows="4"
                            />
                            <button
                                className="submit-button"
                                onClick={handleAskQuestion}
                            >
                                Отправить вопрос
                            </button>
                        </div>

                        <div className="ask-note">
                            <small>💡 Вопрос будет отправлен в общий чат, где вы получите ответ от администраторов или других пользователей.</small>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default HelpCenter;