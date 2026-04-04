import React, { useState, useEffect } from 'react';
import { getUsername } from '../../keycloak';
import './Questions.css';

const Questions = () => {
    const [questions, setQuestions] = useState([]);
    const [newQuestion, setNewQuestion] = useState('');
    const [answerText, setAnswerText] = useState({});
    const currentUser = getUsername();

    // Состояния для модального окна подтверждения удаления одного вопроса
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [questionToDelete, setQuestionToDelete] = useState(null);

    // Состояние для массового удаления отвеченных вопросов
    const [showBulkDeleteConfirm, setShowBulkDeleteConfirm] = useState(false);

    // Получаем роль из localStorage (от переключателя)
    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin';

    // Функция для сортировки вопросов: неотвеченные сверху, отвеченные снизу
    const sortQuestions = (questionsList) => {
        return [...questionsList].sort((a, b) => {
            // Сначала сортируем по статусу ответа (неотвеченные первыми)
            if (a.isAnswered !== b.isAnswered) {
                return a.isAnswered ? 1 : -1;
            }
            // Затем по дате (новые сверху внутри каждой группы)
            return b.id - a.id;
        });
    };

    // Загружаем вопросы из localStorage
    useEffect(() => {
        const savedQuestions = localStorage.getItem('userQuestions');
        if (savedQuestions) {
            const allQuestions = JSON.parse(savedQuestions);
            // Сортируем вопросы
            const sortedQuestions = sortQuestions(allQuestions);

            // Если не админ - показываем только свои вопросы
            if (!isAdmin) {
                const myQuestions = sortedQuestions.filter(q => q.author === currentUser);
                setQuestions(myQuestions);
            } else {
                setQuestions(sortedQuestions);
            }
        } else {
            // Примеры вопросов для демонстрации
            const initialQuestions = [
                {
                    id: 3,
                    question: 'Когда будут новые тренировки?',
                    answer: '',
                    author: 'Елена',
                    date: '2025-03-22',
                    isAnswered: false
                },
                {
                    id: 2,
                    question: 'Где найти записи эфиров?',
                    answer: 'Все записи эфиров находятся в ленте "Главная" в разделе аудио.',
                    author: 'Михаил',
                    date: '2025-03-19',
                    isAnswered: true,
                    answeredBy: 'Admin',
                    answerDate: '2025-03-20'
                },
                {
                    id: 1,
                    question: 'Как зарегистрироваться на событие?',
                    answer: 'Перейдите во вкладку "События", выберите интересующее событие и нажмите кнопку "Записаться".',
                    author: 'Анна',
                    date: '2025-03-18',
                    isAnswered: true,
                    answeredBy: 'Admin',
                    answerDate: '2025-03-19'
                }
            ];
            const sortedInitial = sortQuestions(initialQuestions);
            localStorage.setItem('userQuestions', JSON.stringify(sortedInitial));

            if (!isAdmin) {
                const myQuestions = sortedInitial.filter(q => q.author === currentUser);
                setQuestions(myQuestions);
            } else {
                setQuestions(sortedInitial);
            }
        }
    }, [isAdmin, currentUser]);

    // Сохраняем вопросы при изменении, сохраняя порядок
    useEffect(() => {
        if (questions.length > 0) {
            const allQuestions = JSON.parse(localStorage.getItem('userQuestions') || '[]');

            // Обновляем существующие вопросы
            const updatedQuestions = allQuestions.map(q => {
                const updated = questions.find(uq => uq.id === q.id);
                return updated || q;
            });

            // Добавляем новые вопросы
            const newQuestions = questions.filter(q => !allQuestions.find(aq => aq.id === q.id));

            // Объединяем и сортируем
            const mergedQuestions = [...newQuestions, ...updatedQuestions];
            const sortedMerged = sortQuestions(mergedQuestions);

            localStorage.setItem('userQuestions', JSON.stringify(sortedMerged));
        }
    }, [questions]);

    const handleAskQuestion = () => {
        if (isAdmin) {
            alert('Администратор не может задавать вопросы, только отвечать на них');
            return;
        }

        if (!newQuestion.trim()) {
            alert('Введите ваш вопрос');
            return;
        }

        const question = {
            id: Date.now(),
            question: newQuestion.trim(),
            answer: '',
            author: currentUser,
            date: new Date().toISOString().split('T')[0],
            isAnswered: false
        };

        // Получаем все вопросы из localStorage
        const allQuestions = JSON.parse(localStorage.getItem('userQuestions') || '[]');
        // Добавляем новый вопрос
        const updatedAllQuestions = [question, ...allQuestions];
        // Сортируем
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem('userQuestions', JSON.stringify(sortedQuestions));

        // Обновляем состояние с сортировкой
        setQuestions(sortedQuestions);
        setNewQuestion('');
        alert('Ваш вопрос отправлен! Ответ появится в ближайшее время.');
    };

    const handleAnswerQuestion = (questionId) => {
        if (!isAdmin) {
            alert('Только администратор может отвечать на вопросы');
            return;
        }

        const answer = answerText[questionId];
        if (!answer || !answer.trim()) {
            alert('Введите ответ');
            return;
        }

        // Получаем все вопросы
        const allQuestions = JSON.parse(localStorage.getItem('userQuestions') || '[]');

        // Обновляем ответ у конкретного вопроса
        const updatedAllQuestions = allQuestions.map(q =>
            q.id === questionId
                ? {
                    ...q,
                    answer: answer.trim(),
                    isAnswered: true,
                    answeredBy: currentUser,
                    answerDate: new Date().toISOString().split('T')[0]
                }
                : q
        );

        // Сортируем после обновления
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem('userQuestions', JSON.stringify(sortedQuestions));

        // Обновляем состояние с новой сортировкой
        setQuestions(sortedQuestions);

        setAnswerText({ ...answerText, [questionId]: '' });
        alert('Ответ сохранен. Вопрос перемещен в раздел отвеченных.');
    };

    // Функция для открытия модального окна подтверждения удаления одного вопроса
    const confirmDeleteQuestion = (question) => {
        setQuestionToDelete(question);
        setShowDeleteConfirm(true);
    };

    // Функция для фактического удаления одного вопроса
    const executeDeleteQuestion = () => {
        if (!questionToDelete) return;

        const demoRole = localStorage.getItem('demoRole');
        if (demoRole !== 'admin') {
            alert('Только администратор может удалять вопросы!');
            setShowDeleteConfirm(false);
            setQuestionToDelete(null);
            return;
        }

        const allQuestions = JSON.parse(localStorage.getItem('userQuestions') || '[]');
        const updatedAllQuestions = allQuestions.filter(q => q.id !== questionToDelete.id);
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem('userQuestions', JSON.stringify(sortedQuestions));

        setQuestions(sortedQuestions);
        setShowDeleteConfirm(false);
        setQuestionToDelete(null);
    };

    // Функция для отмены удаления одного вопроса
    const cancelDeleteQuestion = () => {
        setShowDeleteConfirm(false);
        setQuestionToDelete(null);
    };

    // Функция для открытия модального окна массового удаления отвеченных вопросов
    const confirmBulkDelete = () => {
        const answeredCount = questions.filter(q => q.isAnswered).length;
        if (answeredCount === 0) {
            alert('Нет отвеченных вопросов для удаления');
            return;
        }
        setShowBulkDeleteConfirm(true);
    };

    // Функция для массового удаления отвеченных вопросов
    const executeBulkDelete = () => {
        const demoRole = localStorage.getItem('demoRole');
        if (demoRole !== 'admin') {
            alert('Только администратор может удалять вопросы!');
            setShowBulkDeleteConfirm(false);
            return;
        }

        // Оставляем только неотвеченные вопросы
        const unansweredQuestions = questions.filter(q => !q.isAnswered);
        const sortedQuestions = sortQuestions(unansweredQuestions);
        localStorage.setItem('userQuestions', JSON.stringify(sortedQuestions));

        setQuestions(sortedQuestions);
        setShowBulkDeleteConfirm(false);
        alert(`Удалено ${questions.filter(q => q.isAnswered).length} отвеченных вопросов`);
    };

    // Функция для отмены массового удаления
    const cancelBulkDelete = () => {
        setShowBulkDeleteConfirm(false);
    };

    // Подсчет неотвеченных и отвеченных вопросов
    const unansweredCount = questions.filter(q => !q.isAnswered).length;
    const answeredCount = questions.filter(q => q.isAnswered).length;

    return (
        <div className="help-center">
            {/* Модальное окно подтверждения удаления одного вопроса */}
            {showDeleteConfirm && questionToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteQuestion}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить вопрос?</p>
                            <div className="question-preview">
                                <strong>{questionToDelete.question}</strong>
                                <div className="question-details-preview">
                                    <span>👤 {questionToDelete.author}</span>
                                    <span>📅 {questionToDelete.date}</span>
                                </div>
                                {questionToDelete.isAnswered && (
                                    <small>✅ Уже есть ответ</small>
                                )}
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteQuestion}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteQuestion}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно подтверждения массового удаления отвеченных вопросов */}
            {showBulkDeleteConfirm && (
                <div className="modal-overlay" onClick={cancelBulkDelete}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️📦</span>
                            <h3>Массовое удаление</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить все отвеченные вопросы?</p>
                            <div className="bulk-preview">
                                <div className="bulk-count">
                                    <span className="bulk-icon">✅</span>
                                    <strong>{answeredCount} отвеченных вопросов</strong>
                                </div>
                                <div className="bulk-warning">
                                    ⚠️ Будут удалены все вопросы, на которые уже дан ответ
                                </div>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelBulkDelete}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeBulkDelete}>
                                ✅ Да, удалить все
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="help-header">
                <h2>❓ Вопросы и ответы</h2>
                <p>
                    {isAdmin
                        ? `👑 Режим администратора: ${unansweredCount} вопрос${unansweredCount === 1 ? '' : 'ов'} ожидают ответа, ${answeredCount} отвечено`
                        : '💬 Задайте вопрос администратору. Ответ появится здесь.'
                    }
                </p>
            </div>

            {/* Секция задания вопроса - показываем только для пользователей (не админов) */}
            {!isAdmin && (
                <div className="ask-section">
                    <h3>💬 Задать вопрос</h3>
                    <div className="ask-description">
                        <p>Напишите свой вопрос, и администратор ответит вам в ближайшее время.</p>
                    </div>

                    <div className="ask-form">
                        <textarea
                            className="question-input"
                            placeholder="Введите ваш вопрос здесь..."
                            value={newQuestion}
                            onChange={(e) => setNewQuestion(e.target.value)}
                            rows="4"
                        />
                        <button
                            className="submit-button"
                            onClick={handleAskQuestion}
                        >
                            📤 Отправить вопрос
                        </button>
                    </div>
                </div>
            )}

            <div className="faq-section">
                <div className="faq-header-actions">
                    <h3>
                        {isAdmin
                            ? `📋 Все вопросы (${unansweredCount} ожидают ответа, ${answeredCount} отвечено)`
                            : `📋 Мои вопросы (${questions.length})`
                        }
                    </h3>
                    {isAdmin && answeredCount > 0 && (
                        <button
                            className="bulk-delete-btn"
                            onClick={confirmBulkDelete}
                            title="Удалить все отвеченные вопросы"
                        >
                            🗑️ Удалить отвеченные ({answeredCount})
                        </button>
                    )}
                </div>

                {/* Разделитель для админа - неотвеченные вопросы */}
                {isAdmin && unansweredCount > 0 && (
                    <div className="section-divider">
                        <span className="divider-title">⏳ Ожидают ответа ({unansweredCount})</span>
                    </div>
                )}

                <div className="faq-list">
                    {questions.length === 0 ? (
                        <div className="no-questions">
                            <p>❓ {isAdmin ? 'Пока нет вопросов от пользователей' : 'У вас пока нет вопросов'}</p>
                            {!isAdmin && <p className="hint-text">Задайте свой первый вопрос выше! ✍️</p>}
                        </div>
                    ) : (
                        questions.map((q, index) => (
                            <div
                                key={q.id}
                                className={`faq-item ${q.isAnswered ? 'answered' : 'pending'} ${!q.isAnswered && index === 0 ? 'newest' : ''}`}
                                style={{
                                    animation: `fadeIn 0.3s ease`
                                }}
                            >
                                <div className="faq-question">
                                    <div className="question-header">
                                        <div className="question-title">
                                            <strong>❓ {q.question}</strong>
                                            {!q.isAnswered && !isAdmin && (
                                                <span className="waiting-badge">⏳ Ожидает ответа</span>
                                            )}
                                        </div>
                                        {isAdmin && (
                                            <button
                                                className="delete-question-btn"
                                                onClick={() => confirmDeleteQuestion(q)}
                                                title="Удалить вопрос"
                                            >
                                                🗑️
                                            </button>
                                        )}
                                    </div>
                                    <div className="question-meta">
                                        <span>👤 {q.author}</span>
                                        <span>📅 {q.date}</span>
                                        {q.isAnswered && <span className="answered-badge">✅ Ответ дан</span>}
                                        {!q.isAnswered && isAdmin && <span className="pending-badge">⏳ Ожидает ответа</span>}
                                    </div>
                                </div>

                                {q.isAnswered ? (
                                    <div className="faq-answer">
                                        <strong>📝 Ответ:</strong>
                                        <p>{q.answer}</p>
                                        <div className="answer-meta">
                                            <span>👨‍💼 {q.answeredBy}</span>
                                            <span>📅 {q.answerDate}</span>
                                        </div>
                                    </div>
                                ) : (
                                    isAdmin && (
                                        <div className="answer-form">
                                            <textarea
                                                className="answer-input"
                                                placeholder="Введите ответ на вопрос..."
                                                value={answerText[q.id] || ''}
                                                onChange={(e) => setAnswerText({ ...answerText, [q.id]: e.target.value })}
                                                rows="3"
                                            />
                                            <button
                                                className="answer-submit"
                                                onClick={() => handleAnswerQuestion(q.id)}
                                            >
                                                ✍️ Ответить
                                            </button>
                                        </div>
                                    )
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default Questions;