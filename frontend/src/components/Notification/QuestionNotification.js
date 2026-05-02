import React, { useState, useEffect } from 'react';
import './QuestionNotification.css';

const QuestionNotification = () => {
    const [notifications, setNotifications] = useState([]);
    const demoRole = localStorage.getItem('demoRole');

    const isGeneralAdmin = demoRole === 'admin_moderator' || demoRole === 'developer';
    const isNutritionConsultant = demoRole === 'consultant_nutrition';
    const isVyacheslav = demoRole === 'vyacheslav';
    const isUser = demoRole === 'user0' || demoRole === 'user1' || demoRole === 'user2';

    useEffect(() => {
        const interval = setInterval(() => {
            checkForNotifications();
        }, 2000);

        return () => clearInterval(interval);
    }, [demoRole]);

    const checkForNotifications = () => {
        const lastCheck = localStorage.getItem(`lastNotificationCheck_${demoRole}`) || '0';
        const currentTime = Date.now();

        if (isGeneralAdmin || isNutritionConsultant || isVyacheslav) {
            checkNewQuestions(lastCheck);
        }

        if (isUser) {
            checkNewAnswers(lastCheck);
        }

        localStorage.setItem(`lastNotificationCheck_${demoRole}`, currentTime.toString());
    };

    const checkNewQuestions = (lastCheck) => {
        const modes = [];
        if (isGeneralAdmin) modes.push('general');
        if (isNutritionConsultant) modes.push('nutrition');
        if (isVyacheslav) modes.push('vyacheslav');

        modes.forEach(mode => {
            const storageKey = `userQuestions_${mode}`;
            const questions = JSON.parse(localStorage.getItem(storageKey) || '[]');

            const newQuestions = questions.filter(q =>
                !q.isAnswered &&
                parseInt(q.id) > parseInt(lastCheck)
            );

            newQuestions.forEach(q => {
                addNotification({
                    id: `question_${q.id}`,
                    type: 'new_question',
                    mode: mode,
                    question: q.question,
                    author: q.author,
                    timestamp: Date.now()
                });
            });
        });
    };

    const checkNewAnswers = (lastCheck) => {
        const currentUser = localStorage.getItem('username') || 'user0';
        const modes = ['general', 'nutrition', 'vyacheslav'];

        modes.forEach(mode => {
            const storageKey = `userQuestions_${mode}`;
            const questions = JSON.parse(localStorage.getItem(storageKey) || '[]');

            const myAnsweredQuestions = questions.filter(q =>
                q.author === currentUser &&
                q.isAnswered &&
                q.answerTimestamp &&
                parseInt(q.answerTimestamp) > parseInt(lastCheck)
            );

            myAnsweredQuestions.forEach(q => {
                addNotification({
                    id: `answer_${q.id}`,
                    type: 'new_answer',
                    mode: mode,
                    question: q.question,
                    answer: q.answer,
                    answeredBy: q.answeredBy,
                    timestamp: Date.now()
                });
            });
        });
    };

    const addNotification = (notification) => {
        setNotifications(prev => {
            if (prev.some(n => n.id === notification.id)) {
                return prev;
            }
            return [...prev, notification];
        });

        setTimeout(() => {
            removeNotification(notification.id);
        }, 5000);
    };

    const removeNotification = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    const getModeLabel = (mode) => {
        switch(mode) {
            case 'general': return 'Общий';
            case 'nutrition': return 'Питание';
            case 'vyacheslav': return 'Вячеславу';
            default: return '';
        }
    };

    if (notifications.length === 0) return null;

    return (
        <div className="question-notifications">
            {notifications.map(notification => (
                <div
                    key={notification.id}
                    className={`notification ${notification.type}`}
                    onClick={() => removeNotification(notification.id)}
                >
                    {notification.type === 'new_question' ? (
                        <>
                            <div className="notification-header">
                                <span className="notification-icon">❓</span>
                                <span className="notification-title">Новый вопрос ({getModeLabel(notification.mode)})</span>
                                <button className="notification-close" onClick={(e) => {
                                    e.stopPropagation();
                                    removeNotification(notification.id);
                                }}>×</button>
                            </div>
                            <div className="notification-body">
                                <p className="notification-author">👤 {notification.author}</p>
                                <p className="notification-text">{notification.question}</p>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="notification-header">
                                <span className="notification-icon">✅</span>
                                <span className="notification-title">Получен ответ ({getModeLabel(notification.mode)})</span>
                                <button className="notification-close" onClick={(e) => {
                                    e.stopPropagation();
                                    removeNotification(notification.id);
                                }}>×</button>
                            </div>
                            <div className="notification-body">
                                <p className="notification-question">❓ {notification.question}</p>
                                <p className="notification-answer">📝 {notification.answer}</p>
                                <p className="notification-author">👨‍💼 {notification.answeredBy}</p>
                            </div>
                        </>
                    )}
                </div>
            ))}
        </div>
    );
};

export default QuestionNotification;
