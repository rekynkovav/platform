import React, { useState, useEffect, useRef } from 'react';
import { getUsername } from '../../keycloak';
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

    // Миграция данных - выполняется один раз при загрузке компонента
    useEffect(() => {
        const migrationKey = 'questions_migration_v2_done';
        const migrationDone = localStorage.getItem(migrationKey);

        if (!migrationDone) {
            console.log('Выполняется миграция вопросов...');

            // Очищаем все категории для чистого старта
            const categories = ['general', 'nutrition', 'vyacheslav'];
            categories.forEach(cat => {
                const key = `userQuestions_${cat}`;
                const questions = JSON.parse(localStorage.getItem(key) || '[]');

                // Фильтруем вопросы по правильной категории
                const correctQuestions = questions.filter(q => q.mode === cat);

                if (correctQuestions.length !== questions.length) {
                    console.log(`Категория ${cat}: было ${questions.length}, осталось ${correctQuestions.length}`);
                    localStorage.setItem(key, JSON.stringify(correctQuestions));
                }
            });

            localStorage.setItem(migrationKey, 'true');
            console.log('Миграция завершена');
        }
    }, []);

    // Получаем роль из localStorage (от переключателя)
    const demoRole = localStorage.getItem('demoRole');

    // Определяем ID роли (уникальный идентификатор)
    const getRoleId = (role) => {
        const roleIds = {
            'admin-moderator': 'admin-moderator',
            'consultant-nutrition': 'consultant-nutrition',
            'consultant-sport': 'consultant-sport',
            'vyacheslav': 'vyacheslav',
            'developer': 'developer',
            'super-admin': 'super-admin',
            'user0': 'user0',
            'user1': 'user1',
            'user2': 'user2'
        };
        return roleIds[role] || role;
    };

    const currentRoleId = getRoleId(demoRole);

    // Определяем роли четко
    const isGeneralAdmin = demoRole === 'admin-moderator' || demoRole === 'developer';
    const isNutritionConsultant = demoRole === 'consultant-nutrition';
    const isSportConsultant = demoRole === 'consultant-sport';
    const isVyacheslav = demoRole === 'vyacheslav';
    const isSuperAdmin = demoRole === 'super-admin';
    const isUser0 = demoRole === 'user0';
    const isAnyAdmin = isGeneralAdmin || isNutritionConsultant || isSportConsultant || isVyacheslav || isSuperAdmin;

    // Кто может переключать режимы (только пользователи 1 и 2)
    const canSwitchModes = demoRole === 'user1' || demoRole === 'user2';

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
        const loadQuestions = () => {
            // Загружаем ВСЕ вопросы из единого хранилища
            const allQuestionsKey = 'allQuestions';
            const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

            let questionsToShow = [];

            if (isNutritionConsultant) {
                // Консультант питания видит ТОЛЬКО вопросы с recipientId = 'consultant-nutrition'
                questionsToShow = allQuestions.filter(q =>
                    q.recipientId === 'consultant-nutrition' && !q.archived
                );
                console.log('Консультант питания: вопросы с recipientId=consultant-nutrition:', questionsToShow.length);
            } else if (isVyacheslav) {
                // Вячеслав переключается между личными и общими
                if (vyacheslavView === 'personal') {
                    // Личные вопросы: recipientId = 'vyacheslav'
                    questionsToShow = allQuestions.filter(q =>
                        q.recipientId === 'vyacheslav' && !q.archived
                    );
                    console.log('Вячеслав (личные): вопросы с recipientId=vyacheslav:', questionsToShow.length);
                } else {
                    // Общие вопросы: recipientId пустой
                    questionsToShow = allQuestions.filter(q =>
                        (!q.recipientId || q.recipientId === '') && !q.archived
                    );
                    console.log('Вячеслав (общие): вопросы с пустым recipientId:', questionsToShow.length);
                }
            } else if (isGeneralAdmin || isSuperAdmin) {
                // Общие админы и главный админ видят только вопросы с пустым recipientId
                questionsToShow = allQuestions.filter(q =>
                    (!q.recipientId || q.recipientId === '') && !q.archived
                );
                console.log('Общий/Главный админ: вопросы с пустым recipientId:', questionsToShow.length);
            } else if (!isAnyAdmin) {
                // Обычные пользователи видят свои вопросы из текущей категории
                questionsToShow = allQuestions.filter(q =>
                    q.authorId === currentRoleId &&
                    q.mode === questionMode &&
                    !q.archivedByUser
                );
                console.log('Пользователь: свои вопросы в режиме', questionMode, ':', questionsToShow.length);
            }

            setQuestions(sortQuestions(questionsToShow));
        };

        loadQuestions();

        // Слушаем изменения в localStorage для синхронизации между вкладками/ролями
        const handleStorageChange = (e) => {
            if (e.key === 'allQuestions') {
                loadQuestions();
            }
        };

        window.addEventListener('storage', handleStorageChange);

        // Также проверяем изменения каждые 2 секунды (для синхронизации в одной вкладке)
        const interval = setInterval(loadQuestions, 2000);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
            clearInterval(interval);
        };
    }, [questionMode, vyacheslavView, isGeneralAdmin, isNutritionConsultant, isVyacheslav, isSuperAdmin, currentRoleId, isAnyAdmin]);

    // Устанавливаем режим по умолчанию для админа питания и Вячеслава
    useEffect(() => {
        if (isNutritionConsultant && questionMode !== 'nutrition') {
            setQuestionMode('nutrition');
        }
        if (isVyacheslav && questionMode !== 'vyacheslav' && questionMode !== 'general') {
            setQuestionMode('vyacheslav');
        }
    }, [isNutritionConsultant, isVyacheslav, questionMode]);

    // Сохраняем вопросы при изменении в единое хранилище
    useEffect(() => {
        if (questions.length > 0) {
            const allQuestionsKey = 'allQuestions';
            const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

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

            localStorage.setItem(allQuestionsKey, JSON.stringify(sortedMerged));
        }
    }, [questions]);

    const handleAskQuestion = () => {
        if (isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSportConsultant || isSuperAdmin) {
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
            authorId: currentRoleId,
            date: new Date().toISOString().split('T')[0],
            isAnswered: false,
            mode: questionMode,
            recipientId: questionMode === 'nutrition' ? 'consultant-nutrition' :
                        questionMode === 'vyacheslav' ? 'vyacheslav' :
                        '', // Пустая строка для общих вопросов
            archived: false,
            archivedByUser: false
        };

        // Сохраняем ВСЕ вопросы в одно общее хранилище
        const allQuestionsKey = 'allQuestions';
        const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

        // Добавляем новый вопрос
        const updatedAllQuestions = [question, ...allQuestions];

        // Сортируем
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem(allQuestionsKey, JSON.stringify(sortedQuestions));

        // Обновляем состояние с сортировкой
        const myQuestions = sortedQuestions.filter(q => q.authorId === currentRoleId && q.mode === questionMode && !q.archivedByUser);
        setQuestions(myQuestions);
        setNewQuestion('');

        // Показываем уведомление об отправке
        showQuestionSentNotification();
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

    const confirmAnswer = () => {
        if (!pendingAnswerData) return;

        const { questionId, answer } = pendingAnswerData;

        // Загружаем все вопросы из единого хранилища
        const allQuestionsKey = 'allQuestions';
        const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

        // Обновляем ответ у конкретного вопроса
        const updatedAllQuestions = allQuestions.map(q =>
            q.id === questionId
                ? {
                    ...q,
                    answer: answer,
                    isAnswered: true,
                    answeredBy: currentUser,
                    answerDate: new Date().toISOString().split('T')[0],
                    answerTimestamp: Date.now().toString()
                }
                : q
        );

        // Сортируем после обновления
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem(allQuestionsKey, JSON.stringify(sortedQuestions));

        // Обновляем локальное состояние
        const updatedLocalQuestions = questions.map(q =>
            q.id === questionId
                ? {
                    ...q,
                    answer: answer,
                    isAnswered: true,
                    answeredBy: currentUser,
                    answerDate: new Date().toISOString().split('T')[0],
                    answerTimestamp: Date.now().toString()
                }
                : q
        );
        setQuestions(sortQuestions(updatedLocalQuestions));

        setAnswerText({ ...answerText, [questionId]: '' });
        setShowAnswerConfirm(false);
        setPendingAnswerData(null);
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
    const executeDeleteQuestion = () => {
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

        // Загружаем все вопросы из единого хранилища
        const allQuestionsKey = 'allQuestions';
        const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

        // Удаляем вопрос
        const updatedAllQuestions = allQuestions.filter(q => q.id !== questionToDelete.id);
        const sortedQuestions = sortQuestions(updatedAllQuestions);
        localStorage.setItem(allQuestionsKey, JSON.stringify(sortedQuestions));

        // Обновляем локальное состояние
        const updatedLocalQuestions = questions.filter(q => q.id !== questionToDelete.id);
        setQuestions(sortQuestions(updatedLocalQuestions));

        setShowDeleteConfirm(false);
        setQuestionToDelete(null);
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
    const executeBulkDelete = () => {
        const isUserRole = !isGeneralAdmin && !isNutritionConsultant && !isVyacheslav && !isSportConsultant && !isSuperAdmin;

        // Загружаем все вопросы из единого хранилища
        const allQuestionsKey = 'allQuestions';
        const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

        // Для админов - помечаем как archived, для пользователей - как archivedByUser
        const updatedQuestions = allQuestions.map(q => {
            if (isUserRole) {
                // Пользователь архивирует только свои отвеченные вопросы в текущем режиме
                if (q.authorId === currentRoleId && q.mode === questionMode && q.isAnswered) {
                    return { ...q, archivedByUser: true, archivedByUserAt: new Date().toISOString() };
                }
            } else {
                // Админы архивируют отвеченные вопросы в своей области видимости
                let shouldArchive = false;

                if (isNutritionConsultant && q.recipientId === 'consultant-nutrition' && q.isAnswered) {
                    shouldArchive = true;
                } else if (isVyacheslav && vyacheslavView === 'personal' && q.recipientId === 'vyacheslav' && q.isAnswered) {
                    shouldArchive = true;
                } else if (isVyacheslav && vyacheslavView === 'general' && (!q.recipientId || q.recipientId === '') && q.isAnswered) {
                    shouldArchive = true;
                } else if ((isGeneralAdmin || isSuperAdmin) && (!q.recipientId || q.recipientId === '') && q.isAnswered) {
                    shouldArchive = true;
                }

                if (shouldArchive) {
                    return { ...q, archived: true, archivedAt: new Date().toISOString() };
                }
            }
            return q;
        });

        localStorage.setItem(allQuestionsKey, JSON.stringify(updatedQuestions));

        // Обновляем локальное отображение - убираем заархивированные вопросы
        const activeQuestions = questions.filter(q => !q.isAnswered);
        setQuestions(sortQuestions(activeQuestions));

        setShowBulkDeleteConfirm(false);
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

                {/* Переключатель для Обычных пользователей */}
                {canSwitchModes && (
                    <div className="mode-switcher">
                        <button
                            className={`mode-btn ${questionMode === 'general' ? 'active' : ''}`}
                            onClick={() => setQuestionMode('general')}
                        >
                            Общий
                        </button>
                        <button
                            className={`mode-btn ${questionMode === 'nutrition' ? 'active' : ''}`}
                            onClick={() => setQuestionMode('nutrition')}
                        >
                            Питание
                        </button>
                        <button
                            className={`mode-btn ${questionMode === 'vyacheslav' ? 'active' : ''}`}
                            onClick={() => setQuestionMode('vyacheslav')}
                        >
                            Вячеславу
                        </button>
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

            {/* Секция задания вопроса - показываем только для пользователей (не админов) */}
            {!isGeneralAdmin && !isNutritionConsultant && !isVyacheslav && !isSportConsultant && !isSuperAdmin && (
                <div className="ask-section">
                    <h3>💬 Задать вопрос</h3>
                    <div className="ask-description">
                        <p>
                            {questionMode === 'general' && 'Напишите свой вопрос в общий раздел.'}
                            {questionMode === 'nutrition' && 'Напишите вопрос по питанию. Ответит администратор питания.'}
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
                    <h3>
                        {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin)
                            ? `📋 Все вопросы (${unansweredCount} ожидают ответа, ${answeredCount} отвечено)`
                            : `📋 Мои вопросы (${questions.length})`
                        }
                    </h3>
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

                <div className="faq-list">
                    {questions.length === 0 ? (
                        <div className="no-questions">
                            <p>❓ {(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) ? 'Пока нет вопросов от пользователей' : 'У вас пока нет вопросов'}</p>
                            {!(isGeneralAdmin || isNutritionConsultant || isVyacheslav || isSuperAdmin) && <p className="hint-text">Задайте свой первый вопрос выше! ✍️</p>}
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
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default Questions;
