import React, { useState, useEffect, useRef, useMemo } from 'react';
import { FixedSizeList as List } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';
import { getUsername } from '../../keycloak';
import { chatApi } from '../../services/api';
import './Questions.css';

const Questions = () => {
    const [questions, setQuestions] = useState([]);
    const [newQuestion, setNewQuestion] = useState('');
    const [answerText, setAnswerText] = useState({});
    const [questionMode, setQuestionMode] = useState('general'); // general, nutrition, vyacheslav
    const [vyacheslavView, setVyacheslavView] = useState('personal'); // personal, general - для Вячеслава
    const currentUser = getUsername();

    // Состояния для модального окна подтверждения удаления одного вопроса
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [questionToDelete, setQuestionToDelete] = useState(null);

    // Состояние для массового удаления отвеченных вопросов
    const [showBulkDeleteConfirm, setShowBulkDeleteConfirm] = useState(false);

    // Состояние для уведомления об отправке вопроса
    const [showQuestionSent, setShowQuestionSent] = useState(false);
    const questionSentTimerRef = useRef(null);

    // Состояние для подтверждения полного ответа
    const [showAnswerConfirm, setShowAnswerConfirm] = useState(false);
    const [pendingAnswerData, setPendingAnswerData] = useState(null);
    const [error, setError] = useState(null);
    const clearError = () => {
        setError(null);
    };

    // Получаем роль из localStorage (от переключателя)
    const demoRole = localStorage.getItem('demoRole');

    // Определяем ID роли (уникальный идентификатор)
    const getRoleId = (role) => {
        const roleIds = {
            'MODERATOR_ADMIN': 'MODERATOR_ADMIN',
            'CONSULTANT_PITANIE': 'CONSULTANT_PITANIE',
            'CONSULTANT_SPORT': 'CONSULTANT_SPORT',
            'VYACHESLAV': 'VYACHESLAV',
            'DEVELOPER': 'DEVELOPER',
            'SUPER_ADMIN': 'SUPER_ADMIN',
            'USER_0': 'USER_0',
            'USER_1': 'USER_1',
            'USER_2': 'USER_2'
        };
        return roleIds[role] || role;
    };

    const currentRoleId = getRoleId(demoRole);

    // Информация о комнатах вопросов из бэкенда (для будущей интеграции)
    const questionRooms = [
        { slug: 'questions-general', mode: 'general', name: 'Общие вопросы', description: 'Вопросы в общий раздел' },
        { slug: 'questions-nutrition', mode: 'nutrition', name: 'Вопросы по питанию', description: 'Вопросы консультанту по питанию' },
        { slug: 'questions-vyacheslav', mode: 'vyacheslav', name: 'Вопросы лично Вячеславу', description: 'Личные вопросы Вячеславу Тимошенко' }
    ];

    // Определяем роли четко
    const isGeneralAdmin = demoRole === 'MODERATOR_ADMIN' || demoRole === 'DEVELOPER';
    const isNutritionConsultant = demoRole === 'CONSULTANT_PITANIE';
    const isSportConsultant = demoRole === 'CONSULTANT_SPORT';
    const isVyacheslav = demoRole === 'VYACHESLAV';
    const isSuperAdmin = demoRole === 'SUPER_ADMIN';
    const isUser0 = demoRole === 'USER_0';
    const isAnyAdmin = isGeneralAdmin || isNutritionConsultant || isSportConsultant || isVyacheslav || isSuperAdmin;

    // Кто может переключать режимы (ВСЕ пользователи, кроме админов в режиме ответа)
    // Админы видят переключатель только когда отвечают на вопросы
    const canSwitchModes = !isGeneralAdmin && !isNutritionConsultant && !isVyacheslav && !isSportConsultant && !isSuperAdmin;

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

    // Загружаем вопросы с бэкенда
    useEffect(() => {
        const loadQuestions = async () => {
            try {
                const data = await chatApi.getQuestions(questionMode);
                setQuestions(sortQuestions(data));
            } catch (error) {
                console.error('Ошибка загрузки вопросов:', error);
                setError('Не удалось загрузить вопросы');
            }
        };

        loadQuestions();

        // Опционально: обновляем вопросы каждые 30 секунд
        const interval = setInterval(loadQuestions, 30000);

        return () => {
            clearInterval(interval);
        };
    }, [questionMode]);

    // Устанавливаем режим по умолчанию для админа питания и Вячеслава
    useEffect(() => {
        if (isNutritionConsultant && questionMode !== 'nutrition') {
            setQuestionMode('nutrition');
        }
        if (isVyacheslav && questionMode !== 'vyacheslav' && questionMode !== 'general') {
            setQuestionMode('vyacheslav');
        }
    }, [isNutritionConsultant, isVyacheslav, questionMode]);


    const handleAskQuestion = async () => {
        if (isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSportConsultant || isSuperAdmin) {
            alert('Администратор не может задавать вопросы, только отвечать на них');
            return;
        }

        if (!newQuestion.trim()) {
            alert('Введите ваш вопрос');
            return;
        }

        try {
            const questionData = {
                content: newQuestion.trim(),
                mode: questionMode
            };

            await chatApi.sendQuestion(questionMode, questionData);

            // Обновляем список вопросов
            const data = await chatApi.getQuestions(questionMode);
            setQuestions(sortQuestions(data));
            setNewQuestion('');

            // Показываем уведомление об отправке
            showQuestionSentNotification();
        } catch (error) {
            console.error('Ошибка отправки вопроса:', error);
            alert('Не удалось отправить вопрос. Попробуйте позже.');
        }
    };

    const showQuestionSentNotification = () => {
        // Сначала скрываем, если уже показано
        setShowQuestionSent(false);

        // Очищаем предыдущий таймер если есть
        if (questionSentTimerRef.current) {
            clearTimeout(questionSentTimerRef.current);
            questionSentTimerRef.current = null;
        }

        // Небольшая задержка для сброса анимации
        setTimeout(() => {
            setShowQuestionSent(true);

            // Устанавливаем новый таймер
            questionSentTimerRef.current = setTimeout(() => {
                setShowQuestionSent(false);
                questionSentTimerRef.current = null;
            }, 2000);
        }, 10);
    };

    const handleAnswerQuestion = (questionId) => {
        // Проверяем права на ответ
        const canAnswer = (vyacheslavView === 'general' && isVyacheslav) ||
                         (vyacheslavView === 'personal' && isVyacheslav) ||
                         (questionMode === 'general' && isGeneralAdmin) ||
                         (questionMode === 'general' && isSuperAdmin) ||
                         (questionMode === 'nutrition' && isNutritionConsultant);

        if (!canAnswer) {
            alert('У вас нет прав для ответа на вопросы в этом разделе');
            return;
        }

        const answer = answerText[questionId];
        if (!answer || !answer.trim()) {
            alert('Введите ответ');
            return;
        }

        // Сохраняем данные для подтверждения
        setPendingAnswerData({ questionId, answer: answer.trim() });
        setShowAnswerConfirm(true);
    };

    const confirmAnswer = async () => {
        if (!pendingAnswerData) return;

        const { questionId, answer } = pendingAnswerData;

        try {
            // Отправляем ответ на бэкенд
            await chatApi.sendMessage(`questions-${questionMode}`, {
                content: answer,
                questionId: questionId,
                isAnswer: true
            });

            // Обновляем список вопросов
            const data = await chatApi.getQuestions(questionMode);
            setQuestions(sortQuestions(data));

            setAnswerText({ ...answerText, [questionId]: '' });
            setShowAnswerConfirm(false);
            setPendingAnswerData(null);
        } catch (error) {
            console.error('Ошибка отправки ответа:', error);
            alert('Не удалось отправить ответ. Попробуйте позже.');
        }
    };

    const cancelAnswer = () => {
        setShowAnswerConfirm(false);
        setPendingAnswerData(null);
    };

    // Функция для открытия модального окна подтверждения удаления одного вопроса
    const confirmDeleteQuestion = (question) => {
        // Запрещаем удаление неотвеченных вопросов
        if (!question.isAnswered) {
            alert('Нельзя удалить неотвеченный вопрос');
            return;
        }
        setQuestionToDelete(question);
        setShowDeleteConfirm(true);
    };

    // Функция для фактического удаления одного вопроса
    const executeDeleteQuestion = async () => {
        if (!questionToDelete) return;

        const canDelete = (vyacheslavView === 'general' && isVyacheslav) ||
                         (vyacheslavView === 'personal' && isVyacheslav) ||
                         (questionMode === 'general' && isGeneralAdmin) ||
                         (questionMode === 'nutrition' && isNutritionConsultant);

        if (!canDelete) {
            alert('У вас нет прав для удаления вопросов в этом разделе!');
            setShowDeleteConfirm(false);
            setQuestionToDelete(null);
            return;
        }

        try {
            // Удаляем вопрос через API
            // TODO: Добавить endpoint для удаления вопросов в бэкенде
            // await chatApi.deleteQuestion(questionToDelete.id);

            // Пока просто обновляем список
            const data = await chatApi.getQuestions(questionMode);
            setQuestions(sortQuestions(data));

            setShowDeleteConfirm(false);
            setQuestionToDelete(null);
        } catch (error) {
            console.error('Ошибка удаления вопроса:', error);
            alert('Не удалось удалить вопрос. Попробуйте позже.');
        }
    };

    // Функция для отмены удаления одного вопроса
    const cancelDeleteQuestion = () => {
        setShowDeleteConfirm(false);
        setQuestionToDelete(null);
    };

    // Функция для открытия модального окна очистки отвеченных вопросов
    const confirmBulkDelete = () => {
        const answeredCount = questions.filter(q => q.isAnswered).length;
        if (answeredCount === 0) {
            alert('Нет отвеченных вопросов для очистки');
            return;
        }
        setShowBulkDeleteConfirm(true);
    };

    // Функция для очистки отвеченных вопросов (архивирование)
    const executeBulkDelete = async () => {
        try {
            // TODO: Добавить endpoint для массового архивирования вопросов в бэкенде
            // await chatApi.archiveAnsweredQuestions(questionMode);

            // Пока просто обновляем список
            const data = await chatApi.getQuestions(questionMode);
            setQuestions(sortQuestions(data));

            setShowBulkDeleteConfirm(false);
        } catch (error) {
            console.error('Ошибка очистки вопросов:', error);
            alert('Не удалось очистить список вопросов. Попробуйте позже.');
        }
    };

    // Функция для отмены массового удаления
    const cancelBulkDelete = () => {
        setShowBulkDeleteConfirm(false);
    };

    // Подсчет неотвеченных и отвеченных вопросов
    const unansweredCount = questions.filter(q => !q.isAnswered).length;
    const answeredCount = questions.filter(q => q.isAnswered).length;

    // Компонент для виртуализированного элемента
    const QuestionItem = ({ index, style }) => {
        const q = questions[index];
        return (
            <div
                style={style}
                className={`faq-item ${q.isAnswered ? 'answered' : 'pending'} ${!q.isAnswered && index === 0 ? 'newest' : ''}`}
            >
                <div className="faq-question">
                    <div className="question-header">
                        <div className="question-title">
                            <strong>❓ {q.question}</strong>
                            {!q.isAnswered && !(isGeneralAdmin || isNutritionConsultant || isVyacheslav) && (
                                <span className="waiting-badge">⏳ Ожидает ответа</span>
                            )}
                        </div>
                        {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && q.isAnswered && (
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
                        {!q.isAnswered && (isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && <span className="pending-badge">⏳ Ожидает ответа</span>}
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
                    (isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && (
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
        );
    };

    return (
        <div className="help-center">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            {/* Уведомление об отправке вопроса */}
            {showQuestionSent && (
                <div className="question-sent-notification">
                    Вопрос отправлен
                </div>
            )}

            {/* Модальное окно подтверждения полного ответа */}
            {showAnswerConfirm && (
                <div className="modal-overlay" onClick={cancelAnswer}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">✅</span>
                            <h3>Подтверждение ответа</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы дали полный ответ?</p>
                            <p className="warning-text">После подтверждения вопрос будет перемещен в раздел отвеченных</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelAnswer}>
                                Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={confirmAnswer}>
                                Да, ответ полный
                            </button>
                        </div>
                    </div>
                </div>
            )}

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

            {/* Модальное окно подтверждения очистки отвеченных вопросов */}
            {showBulkDeleteConfirm && (
                <div className="modal-overlay" onClick={cancelBulkDelete}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🧹</span>
                            <h3>Очистка списка вопросов</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите очистить список отвеченных вопросов?</p>
                            <div className="bulk-preview">
                                <div className="bulk-count">
                                    <span className="bulk-icon">✅</span>
                                    <strong>{answeredCount} отвеченных вопросов</strong>
                                </div>
                                <div className="bulk-warning">
                                    💾 Вопросы будут скрыты из списка, но история сохранится в базе данных
                                </div>
                            </div>
                            <p className="warning-text">Список станет чище и удобнее для работы</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelBulkDelete}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeBulkDelete}>
                                🧹 Да, очистить список
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="help-header">
                <h2>Вопросы и ответы</h2>
                <div className="help-subtitle">
                    <small>💡 Система вопросов подготовлена для интеграции с бэкендом. В бэкенде уже есть 3 комнаты:</small>
                    <div className="room-info">
                        {questionRooms.map(room => (
                            <div key={room.slug} className="room-badge">
                                <span className="room-slug">{room.slug}</span>
                                <span className="room-name">{room.name}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Переключатель для выбора комнаты вопросов */}
                <div className="mode-section">
                    <h3>📝 Выберите раздел для вопроса:</h3>

                    {/* Переключатель для обычных пользователей */}
                    {canSwitchModes && (
                        <div className="mode-switcher">
                            {questionRooms.map(room => (
                                <button
                                    key={room.mode}
                                    className={`mode-btn ${questionMode === room.mode ? 'active' : ''}`}
                                    onClick={() => setQuestionMode(room.mode)}
                                    title={room.description}
                                >
                                    {room.name}
                                </button>
                            ))}
                        </div>
                    )}

                {/* Переключатель для Вячеслава между личными и общими вопросами */}
                {isVyacheslav && (
                    <div className="mode-switcher">
                        <button
                            className={`mode-btn ${vyacheslavView === 'personal' ? 'active' : ''}`}
                            onClick={() => setVyacheslavView('personal')}
                        >
                            Личные вопросы
                        </button>
                        <button
                            className={`mode-btn ${vyacheslavView === 'general' ? 'active' : ''}`}
                            onClick={() => setVyacheslavView('general')}
                        >
                            Общие вопросы
                        </button>
                    </div>
                )}

                {/* Админ питания НЕ видит переключателей - только свои вопросы по питанию */}
                </div>
            </div>

            {/* Секция задания вопроса - показываем только для пользователей (не админов) */}
            {!isGeneralAdmin && !isNutritionConsultant && !isVyacheslav && !isSportConsultant && !isSuperAdmin && (
                <div className="ask-section">
                    <h3>💬 Задать вопрос</h3>
                    <div className="ask-description">
                        <div className="current-room-info">
                            <strong>Выбранный раздел:</strong>
                            <span className="room-indicator">
                                {questionRooms.find(r => r.mode === questionMode)?.name || 'Общие вопросы'}
                            </span>
                            <small className="room-slug-hint">
                                (комната в бэкенде: <code>{questionRooms.find(r => r.mode === questionMode)?.slug || 'questions-general'}</code>)
                            </small>
                        </div>
                        <p>
                            {questionMode === 'general' && 'Напишите свой вопрос в общий раздел. Ответят администраторы или другие пользователи.'}
                            {questionMode === 'nutrition' && 'Напишите вопрос по питанию. Ответит консультант по питанию.'}
                            {questionMode === 'vyacheslav' && 'Напишите вопрос лично Вячеславу Тимошенко.'}
                        </p>
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
                    <div className="faq-title-section">
                        <h3>
                            {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin)
                                ? `📋 Все вопросы (${unansweredCount} ожидают ответа, ${answeredCount} отвечено)`
                                : `📋 Мои вопросы (${questions.length})`
                            }
                        </h3>
                        <div className="backend-info">
                            <small>🔧 Для интеграции с бэкендом использовать:</small>
                            <code className="api-endpoint">GET /api/chat/questions/{questionMode}</code>
                            <code className="api-endpoint">POST /api/chat/questions/{questionMode}</code>
                        </div>
                    </div>
                    {/* Кнопка очистки для админов */}
                    {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && answeredCount > 0 && (
                        <button
                            className="bulk-delete-btn"
                            onClick={confirmBulkDelete}
                            title="Очистить список отвеченных вопросов"
                        >
                            🧹 Очистить список ({answeredCount})
                        </button>
                    )}
                    {/* Кнопка очистки для пользователей */}
                    {!isGeneralAdmin && !isNutritionConsultant && !isVyacheslav && !isSportConsultant && !isSuperAdmin && answeredCount > 0 && (
                        <button
                            className="bulk-delete-btn"
                            onClick={confirmBulkDelete}
                            title="Очистить мои отвеченные вопросы"
                        >
                            🧹 Очистить отвеченные ({answeredCount})
                        </button>
                    )}
                </div>

                {/* Разделитель для админа - неотвеченные вопросы */}
                {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && unansweredCount > 0 && (
                    <div className="section-divider">
                        <span className="divider-title">⏳ Ожидают ответа ({unansweredCount})</span>
                    </div>
                )}

                <div className="faq-list" style={{ height: '600px' }}>
                    {questions.length === 0 ? (
                        <div className="no-questions">
                            <p>❓ {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) ? 'Пока нет вопросов от пользователей' : 'У вас пока нет вопросов'}</p>
                            {!(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && <p className="hint-text">Задайте свой первый вопрос выше! ✍️</p>}
                        </div>
                    ) : (
                        <AutoSizer>
                            {({ height, width }) => (
                                <List
                                    height={height}
                                    width={width}
                                    itemCount={questions.length}
                                    itemSize={q => {
                                        const qItem = questions[q];
                                        // Динамическая высота в зависимости от контента
                                        let baseHeight = 120; // базовая высота вопроса
                                        if (qItem.isAnswered) {
                                            baseHeight += 80; // + высота ответа
                                        } else if (isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) {
                                            baseHeight += 100; // + высота формы ответа
                                        }
                                        return baseHeight;
                                    }}
                                >
                                    {QuestionItem}
                                </List>
                            )}
                        </AutoSizer>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Questions;
