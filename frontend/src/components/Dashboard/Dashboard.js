import React, { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import './Dashboard.css';
import { dashboardApi } from '../../services/api';
import useAudioRecorder from '../../hooks/useAudioRecorder';
import { usePermissions } from '../../hooks/usePermissions';
import ConfirmModal from '../common/ConfirmModal';
import AddContentModal from '../common/AddContentModal';
import {
    initialBroadcastsGuests,
    initialExercises,
    initialRecommendations,
    initialHardening,
    initialAbout,
    initialNewbies,
    initialHeartVoice,
    initialReflection,
    initialInterviews,
    initialParticipants,
    initialMotivation,
    initialReviews,
    initialMusic,
    initialMovies,
    initialLiterature,
    initialAboutTimoshenko,
    initialChallenge
} from './initialData';

const Dashboard = () => {
    const [hoveredMenu, setHoveredMenu] = useState(null);
    const [hoveredSubmenu, setHoveredSubmenu] = useState(null);
    const [isOverCard, setIsOverCard] = useState(false);
    const [isOverDropdown, setIsOverDropdown] = useState(false);
    const [isOverSubmenu, setIsOverSubmenu] = useState(false);
    const [isOverSubmenuTrigger, setIsOverSubmenuTrigger] = useState(false);
    const isOverSubmenuRef = useRef(false);
    const isOverSubmenuTriggerRef = useRef(false);
    const hideTimeoutRef = useRef(null);
    const submenuShowTimeoutRef = useRef(null);
    const submenuHideTimeoutRef = useRef(null);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [submenuPosition, setSubmenuPosition] = useState({ top: 0, left: 0 });
    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedMenuId, setSelectedMenuId] = useState(null);
    const [newItemTitle, setNewItemTitle] = useState('');
    const [newItemDescription, setNewItemDescription] = useState('');
    const [newItemMediaUrl, setNewItemMediaUrl] = useState('');
    const [newItemMediaType, setNewItemMediaType] = useState('');
    const blobUrlsRef = useRef([]);
    const [menuData, setMenuData] = useState({});
    const [error, setError] = useState(null);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [itemToDelete, setItemToDelete] = useState(null);
    const [editingItem, setEditingItem] = useState(null);

    // Хук записи аудио
    const {
        isRecording,
        recordingTime,
        audioBlobUrl,
        startRecording,
        stopRecording,
        resetRecording
    } = useAudioRecorder();

    const { hasLevel } = usePermissions();

    // Определяем права через usePermissions
    const isAdmin = hasLevel(6);
    const canEdit = hasLevel(6);
    const isSuperAdmin = hasLevel(8);

    const initialBroadcasts = [
                {
                    id: 1,
                    title: 'Эфир #1: Основы здорового питания',
                    description: 'Практический эфир про здоровое питание для начинающих с научным обоснованием и пошаговым планом внедрения правильных привычек в повседневную жизнь',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 2,
                    title: 'Эфир #2: Утренняя зарядка',
                    description: 'Глубокое погружение в тему физических упражнений с практическими упражнениями для всей семьи и демонстрацией правильной техники выполнения',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 3,
                    title: 'Эфир #3: Медитация и осознанность',
                    description: 'Секреты и техники медитации на основе личного опыта с разбором типичных ошибок начинающих практиков и ответами на самые частые вопросы',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 4,
                    title: 'Эфир #4: Йога для начинающих',
                    description: 'Мастер-класс по йоге для начинающих с пошаговым планом освоения базовых асан и дыхательных практик для ежедневного применения',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 5,
                    title: 'Эфир #5: Закаливание организма',
                    description: 'Разбор ошибок в закаливании с научным обоснованием методик контрастного душа и холодных обливаний для укрепления иммунитета и повышения энергии',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 6,
                    title: 'Эфир #6: Правильный сон',
                    description: 'Как начать заниматься улучшением качества сна с практическими упражнениями и техниками релаксации для глубокого восстановления организма ночью',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 7,
                    title: 'Эфир #7: Дыхательные практики',
                    description: 'Продвинутые методы дыхательных практик с гостями-экспертами и демонстрацией техник для управления стрессом и повышения жизненной энергии тела',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 8,
                    title: 'Эфир #8: Растяжка и гибкость',
                    description: 'Научный подход к растяжке на основе личного опыта с практическими упражнениями для продвинутых и разбором анатомических особенностей мышц',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 9,
                    title: 'Эфир #9: Кардио тренировки',
                    description: 'Личный опыт в кардио тренировках с разбором кейсов успешных трансформаций и пошаговым планом построения выносливости для начинающих спортсменов',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 10,
                    title: 'Эфир #10: Силовые упражнения',
                    description: 'Трансформация через силовые упражнения с научным обоснованием принципов прогрессивной нагрузки и домашними заданиями для самостоятельной практики дома',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 11,
                    title: 'Эфир #11: Психология здоровья',
                    description: 'Ежедневные практики для психологического здоровья с ответами на вопросы о связи тела и разума в процессе оздоровления',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 12,
                    title: 'Эфир #12: Мотивация к изменениям',
                    description: 'Интенсив по мотивации с гостями-экспертами и разбором психологических барьеров на пути к здоровому образу жизни и личностному росту',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 13,
                    title: 'Эфир #13: Энергия тела',
                    description: 'Практический эфир про энергию тела для всей семьи с демонстрацией техник восстановления жизненных сил и баланса энергетических центров',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 14,
                    title: 'Эфир #14: Баланс жизни',
                    description: 'Глубокое погружение в тему баланса жизни на основе личного опыта с практическими упражнениями для гармонизации всех сфер существования',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 15,
                    title: 'Эфир #15: Осознанность в действии',
                    description: 'Секреты и техники осознанности для начинающих с научным обоснованием влияния практик на работу мозга и качество жизни человека',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 16,
                    title: 'Эфир #16: Питьевой режим',
                    description: 'Мастер-класс по питьевому режиму с разбором кейсов и пошаговым планом оптимизации водного баланса для улучшения здоровья организма',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 17,
                    title: 'Эфир #17: Детокс организма',
                    description: 'Разбор ошибок в детоксе с научным обоснованием методов очищения и практическими упражнениями для безопасного выведения токсинов из тела',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 18,
                    title: 'Эфир #18: Витамины и минералы',
                    description: 'Как начать заниматься оптимизацией витаминного баланса для продвинутых с разбором кейсов дефицитных состояний и их коррекции питанием',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 19,
                    title: 'Эфир #19: Суперфуды в рационе',
                    description: 'Продвинутые методы использования суперфудов с гостями-экспертами и демонстрацией рецептов для максимальной пользы здоровью каждый день',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 20,
                    title: 'Эфир #20: Спортивное питание',
                    description: 'Научный подход к спортивному питанию на основе личного опыта с практическими упражнениями по составлению индивидуального рациона спортсмена',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 21,
                    title: 'Эфир #21: Бег для здоровья',
                    description: 'Личный опыт в беге с разбором кейсов и ответами на вопросы о правильной технике и профилактике травм коленей',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 22,
                    title: 'Эфир #22: Плавание',
                    description: 'Трансформация через плавание для всей семьи с пошаговым планом освоения техники и домашними заданиями для улучшения координации движений',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 23,
                    title: 'Эфир #23: Велоспорт',
                    description: 'Ежедневные практики велоспорта с научным обоснованием пользы для сердечно-сосудистой системы и практическими упражнениями для начинающих велосипедистов',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 24,
                    title: 'Эфир #24: Танцы и движение',
                    description: 'Интенсив по танцам с гостями-экспертами и демонстрацией техник для развития пластичности тела и эмоционального раскрепощения через движение',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 25,
                    title: 'Эфир #25: Пилатес',
                    description: 'Практический эфир про пилатес для начинающих на основе личного опыта с разбором кейсов укрепления глубоких мышц кора',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 26,
                    title: 'Эфир #26: Функциональный тренинг',
                    description: 'Глубокое погружение в тему функционального тренинга для продвинутых с научным обоснованием принципов и практическими упражнениями для дома',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 27,
                    title: 'Эфир #27: Восстановление после тренировок',
                    description: 'Секреты и техники восстановления с ответами на вопросы о роли сна и питания в процессе регенерации мышечной ткани',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 28,
                    title: 'Эфир #28: Массаж и самомассаж',
                    description: 'Мастер-класс по массажу для всей семьи с демонстрацией техник и пошаговым планом освоения приемов для снятия мышечного напряжения',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 29,
                    title: 'Эфир #29: Сауна и баня',
                    description: 'Разбор ошибок в использовании сауны с научным обоснованием влияния на организм и практическими упражнениями для безопасного парения',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 30,
                    title: 'Эфир #30: Контрастный душ',
                    description: 'Как начать заниматься контрастным душем для начинающих с разбором кейсов укрепления иммунитета и повышения стрессоустойчивости организма',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 31,
                    title: 'Эфир #31: Интервальное голодание',
                    description: 'Продвинутые методы интервального голодания с гостями-экспертами на основе личного опыта и научных исследований влияния на метаболизм человека',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 32,
                    title: 'Эфир #32: Вегетарианство',
                    description: 'Научный подход к вегетарианству с практическими упражнениями по составлению сбалансированного рациона и домашними заданиями для плавного перехода',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 33,
                    title: 'Эфир #33: Сыроедение',
                    description: 'Личный опыт в сыроедении с разбором кейсов и ответами на вопросы о рисках и пользе употребления термически необработанной пищи',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 34,
                    title: 'Эфир #34: Кето диета',
                    description: 'Трансформация через кето диету для продвинутых с научным обоснованием механизмов кетоза и пошаговым планом входа в состояние жиросжигания',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 35,
                    title: 'Эфир #35: Палео диета',
                    description: 'Ежедневные практики палео диеты с демонстрацией рецептов и разбором принципов питания предков для современного здорового человека',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 36,
                    title: 'Эфир #36: Стресс-менеджмент',
                    description: 'Интенсив по управлению стрессом с гостями-экспертами на основе личного опыта и практическими упражнениями для снижения уровня кортизола',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 37,
                    title: 'Эфир #37: Работа с эмоциями',
                    description: 'Практический эфир про работу с эмоциями для всей семьи с научным обоснованием связи чувств и физического здоровья организма',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 38,
                    title: 'Эфир #38: Позитивное мышление',
                    description: 'Глубокое погружение в тему позитивного мышления для начинающих с разбором кейсов трансформации жизни через изменение внутренних установок',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 39,
                    title: 'Эфир #39: Целеполагание',
                    description: 'Секреты и техники целеполагания с ответами на вопросы о правильной постановке целей и пошаговым планом их достижения',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 40,
                    title: 'Эфир #40: Тайм-менеджмент',
                    description: 'Мастер-класс по тайм-менеджменту на основе личного опыта с практическими упражнениями для оптимизации распорядка дня и повышения продуктивности',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 41,
                    title: 'Эфир #41: Осанка и здоровье спины',
                    description: 'Разбор ошибок в поддержании осанки с научным обоснованием влияния на внутренние органы и домашними заданиями для укрепления мышц',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 42,
                    title: 'Эфир #42: Здоровье глаз',
                    description: 'Как начать заниматься улучшением зрения для продвинутых с разбором кейсов восстановления и практическими упражнениями для глазных мышц',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 43,
                    title: 'Эфир #43: Здоровье зубов',
                    description: 'Продвинутые методы ухода за зубами с гостями-экспертами и демонстрацией техник для профилактики кариеса и укрепления эмали естественным путем',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 44,
                    title: 'Эфир #44: Здоровье кожи',
                    description: 'Научный подход к здоровью кожи для всей семьи с практическими упражнениями по уходу и пошаговым планом очищения организма изнутри',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 45,
                    title: 'Эфир #45: Здоровье волос',
                    description: 'Личный опыт в уходе за волосами с разбором кейсов восстановления и ответами на вопросы о влиянии питания на состояние',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 46,
                    title: 'Эфир #46: Гормональный баланс',
                    description: 'Трансформация через восстановление гормонального баланса для начинающих с научным обоснованием влияния образа жизни на эндокринную систему организма',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 47,
                    title: 'Эфир #47: Иммунитет',
                    description: 'Ежедневные практики укрепления иммунитета на основе личного опыта с домашними заданиями и разбором факторов влияющих на защитные силы',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 48,
                    title: 'Эфир #48: Пищеварение',
                    description: 'Интенсив по улучшению пищеварения с гостями-экспертами и практическими упражнениями для нормализации работы желудочно-кишечного тракта человека',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 49,
                    title: 'Эфир #49: Детоксикация печени',
                    description: 'Практический эфир про детоксикацию печени для продвинутых с научным обоснованием методов и пошаговым планом безопасного очищения органа',
                    mediaUrl: '',
                    mediaType: 'audio',
                    createdAt: new Date().toISOString()
                },
                {
                    id: 50,
                    title: 'Эфир #50: Долголетие',
                    description: 'Глубокое погружение в тему долголетия с разбором кейсов долгожителей и ответами на вопросы о секретах здоровой активной жизни',
                    mediaUrl: '',
                    mediaType: 'video',
                    createdAt: new Date().toISOString()
                }
            ];

    useEffect(() => {
        const loadDashboardData = async () => {
            try {
                const data = await dashboardApi.getMenuData();
                setMenuData(data);
                setError(null);
            } catch (err) {
                console.error('Error loading dashboard data from API:', err);
                // Fallback для разработки
                if (process.env.NODE_ENV === 'development') {
                    try {
                        const savedData = process.env.NODE_ENV === 'development'
                            ? sessionStorage.getItem('dashboardMenuData')
                            : null;
                        if (savedData) {
                            const parsedData = JSON.parse(savedData);
                            // Проверяем, есть ли уже данные, если нет или их мало - добавляем/обновляем
                            let needsUpdate = false;

                            if (!parsedData.broadcasts || parsedData.broadcasts.length < 50) {
                                parsedData.broadcasts = initialBroadcasts;
                                needsUpdate = true;
                            }
                            if (!parsedData['broadcasts-guests'] || parsedData['broadcasts-guests'].length < 50) {
                                parsedData['broadcasts-guests'] = initialBroadcastsGuests;
                                needsUpdate = true;
                            }
                            if (!parsedData.exercises || parsedData.exercises.length < 50) {
                                parsedData.exercises = initialExercises;
                                needsUpdate = true;
                            }
                            if (!parsedData.recommendations || parsedData.recommendations.length < 50) {
                                parsedData.recommendations = initialRecommendations;
                                needsUpdate = true;
                            }
                            if (!parsedData.hardening || parsedData.hardening.length < 50) {
                                parsedData.hardening = initialHardening;
                                needsUpdate = true;
                            }
                            if (!parsedData.about || parsedData.about.length < 50) {
                                parsedData.about = initialAbout;
                                needsUpdate = true;
                            }
                            if (!parsedData.newbies || parsedData.newbies.length < 50) {
                                parsedData.newbies = initialNewbies;
                                needsUpdate = true;
                            }
                            if (!parsedData['heart-voice'] || parsedData['heart-voice'].length < 50) {
                                parsedData['heart-voice'] = initialHeartVoice;
                                needsUpdate = true;
                            }
                            if (!parsedData.reflection || parsedData.reflection.length < 50) {
                                parsedData.reflection = initialReflection;
                                needsUpdate = true;
                            }
                            if (!parsedData.interviews || parsedData.interviews.length < 50) {
                                parsedData.interviews = initialInterviews;
                                needsUpdate = true;
                            }
                            if (!parsedData.participants || parsedData.participants.length < 50) {
                                parsedData.participants = initialParticipants;
                                needsUpdate = true;
                            }
                            if (!parsedData.motivation || parsedData.motivation.length < 50) {
                                parsedData.motivation = initialMotivation;
                                needsUpdate = true;
                            }
                            if (!parsedData.reviews || parsedData.reviews.length < 50) {
                                parsedData.reviews = initialReviews;
                                needsUpdate = true;
                            }
                            if (!parsedData.music || parsedData.music.length < 50) {
                                parsedData.music = initialMusic;
                                needsUpdate = true;
                            }
                            if (!parsedData.movies || parsedData.movies.length < 50) {
                                parsedData.movies = initialMovies;
                                needsUpdate = true;
                            }
                            if (!parsedData.literature || parsedData.literature.length < 50) {
                                parsedData.literature = initialLiterature;
                                needsUpdate = true;
                            }
                            if (!parsedData['about-timoshenko'] || parsedData['about-timoshenko'].length < 50) {
                                parsedData['about-timoshenko'] = initialAboutTimoshenko;
                                needsUpdate = true;
                            }
                            if (!parsedData.challenge || parsedData.challenge.length < 50) {
                                parsedData.challenge = initialChallenge;
                                needsUpdate = true;
                            }

                            if (needsUpdate) {
                                if (process.env.NODE_ENV === 'development') {
                                    sessionStorage.setItem('dashboardMenuData', JSON.stringify(parsedData));
                                }
                            }
                            setMenuData(parsedData);
                        } else {
                            // Инициализация со всеми данными
                            const initialData = {
                                broadcasts: initialBroadcasts,
                                'broadcasts-guests': initialBroadcastsGuests,
                                exercises: initialExercises,
                                recommendations: initialRecommendations,
                                hardening: initialHardening,
                                about: initialAbout,
                                newbies: initialNewbies,
                                'heart-voice': initialHeartVoice,
                                reflection: initialReflection,
                                interviews: initialInterviews,
                                participants: initialParticipants,
                                motivation: initialMotivation,
                                reviews: initialReviews,
                                music: initialMusic,
                                movies: initialMovies,
                                literature: initialLiterature,
                                'about-timoshenko': initialAboutTimoshenko,
                                challenge: initialChallenge
                            };
                            setMenuData(initialData);
                            if (process.env.NODE_ENV === 'development') {
                                sessionStorage.setItem('dashboardMenuData', JSON.stringify(initialData));
                            }
                        }
                        setError(null);
                    } catch (localError) {
                        console.error('Error loading dashboard data from sessionStorage:', localError);
                        setError('Не удалось загрузить данные дашборда');
                    }
                }
            }
        };
        loadDashboardData();
    }, []);

    // Очистка blob URLs при размонтировании
    useEffect(() => {
        return () => {
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    const saveMenuData = useCallback(async (data) => {
        try {
            setMenuData(data);
            await dashboardApi.saveMenuData(data);
            setError(null);
        } catch (err) {
            console.error('Ошибка сохранения данных дашборда:', err);
            // Fallback для разработки
            if (process.env.NODE_ENV === 'development') {
                try {
                    if (process.env.NODE_ENV === 'development') {
                        sessionStorage.setItem('dashboardMenuData', JSON.stringify(data));
                    }
                } catch (localError) {
                    console.error('Error saving to sessionStorage:', localError);
                    setError('Не удалось сохранить данные дашборда');
                }
            } else {
                setError('Не удалось сохранить данные дашборда');
            }
        }
    }, []);

    const navigationMenus = useMemo(() => [
        {
            id: 'broadcasts',
            title: 'Эфиры',
            items: [
                {
                    id: 'with-guests',
                    title: 'С Гостями',
                    description: 'Эфиры с приглашенными гостями',
                    hasSubmenu: true,
                    submenuItems: menuData['broadcasts-guests'] || []
                },
                ...(menuData['broadcasts'] || [])
            ]
        },
        { id: 'exercises', title: 'Упражнения', items: menuData['exercises'] || [] },
        { id: 'recommendations', title: 'Рекомендации', items: menuData['recommendations'] || [] },
        { id: 'hardening', title: 'Закаливание', items: menuData['hardening'] || [] },
        { id: 'about', title: 'О нас', items: menuData['about'] || [] },
        { id: 'newbies', title: 'Для новичков', items: menuData['newbies'] || [] },
        { id: 'heart-voice', title: 'Важные материалы\nголос сердца', items: menuData['heart-voice'] || [] },
        { id: 'reflection', title: 'Для размышления', items: menuData['reflection'] || [] },
        { id: 'interviews', title: 'Интервью с\nИнтересными личностями', items: menuData['interviews'] || [] },
        { id: 'participants', title: 'От участников проекта', items: menuData['participants'] || [] },
        { id: 'motivation', title: 'Мотивация', items: menuData['motivation'] || [] },
        { id: 'reviews', title: 'Отзывы', items: menuData['reviews'] || [] },
        { id: 'music', title: 'Музыка', items: menuData['music'] || [] },
        { id: 'movies', title: 'Фильмы', items: menuData['movies'] || [] },
        { id: 'literature', title: 'Литература', items: menuData['literature'] || [] },
        { id: 'about-timoshenko', title: 'О Тимошенко', items: menuData['about-timoshenko'] || [] },
        { id: 'challenge', title: 'Годовой челлендж\nФизических нагрузок', items: menuData['challenge'] || [] }
    ], [menuData]);

    const currentMenu = useMemo(() =>
        navigationMenus.find(m => m.id === hoveredMenu)
    , [navigationMenus, hoveredMenu]);

    const handleMenuEnter = useCallback((menuId, event) => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        const rect = event.currentTarget.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        // Более точные размеры меню с учетом min-width: 400px
        const menuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
        const menuHeight = Math.min(500, viewportHeight * 0.7);

        let top = rect.bottom + window.scrollY;
        let left = rect.left + window.scrollX;

        // Проверка выхода за правую границу
        if (left + menuWidth > viewportWidth - 20) {
            left = viewportWidth - menuWidth - 20;
        }

        // Проверка выхода за левую границу
        if (left < 20) {
            left = 20;
        }

        // Проверка выхода за нижнюю границу
        if (top + menuHeight > viewportHeight + window.scrollY - 20) {
            // Показываем меню сверху от карточки
            top = rect.top + window.scrollY - menuHeight - 10;
        }

        // Проверка выхода за верхнюю границу
        if (top < window.scrollY + 20) {
            top = window.scrollY + 20;
        }

        setMenuPosition({ top, left });
        setHoveredMenu(menuId);
        setHoveredSubmenu(null);
        setIsOverCard(true);
    }, []);

    const handleCardLeave = useCallback(() => {
        setIsOverCard(false);
    }, []);

    const handleDropdownEnter = useCallback(() => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        setIsOverDropdown(true);
    }, []);

    const handleDropdownLeave = useCallback(() => {
        setIsOverDropdown(false);
    }, []);

    // Эффект для управления видимостью меню
    useEffect(() => {
        if (!isOverCard && !isOverDropdown && !isOverSubmenu && !isOverSubmenuTrigger) {
            hideTimeoutRef.current = setTimeout(() => {
                setHoveredMenu(null);
                setHoveredSubmenu(null);
            }, 100);
            return () => {
                if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);
            };
        }
    }, [isOverCard, isOverDropdown, isOverSubmenu, isOverSubmenuTrigger]);

    const handleSubmenuEnter = useCallback((submenuId, event) => {
        if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);

        const rect = event.currentTarget.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const submenuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
        const submenuHeight = Math.min(500, viewportHeight * 0.7);

        let top = rect.top + window.scrollY;
        let left = rect.right + window.scrollX;

        if (left + submenuWidth > viewportWidth - 20) {
            left = rect.left + window.scrollX - submenuWidth;
        }
        if (left < 20) left = 20;
        if (top + submenuHeight > viewportHeight + window.scrollY - 20) {
            top = viewportHeight + window.scrollY - submenuHeight - 20;
        }
        if (top < window.scrollY + 20) {
            top = window.scrollY + 20;
        }

        setSubmenuPosition({ top, left });
        setHoveredSubmenu(submenuId);
    }, []);

    const handleSubmenuItemEnter = useCallback((submenuId, event) => {
        // Очищаем таймеры скрытия
        if (submenuHideTimeoutRef.current) {
            clearTimeout(submenuHideTimeoutRef.current);
            submenuHideTimeoutRef.current = null;
        }

        setIsOverSubmenuTrigger(true);
        isOverSubmenuTriggerRef.current = true;

        // Сохраняем rect ДО setTimeout, чтобы event.currentTarget не стал null
        const rect = event.currentTarget.getBoundingClientRect();
        const scrollX = window.scrollX;
        const scrollY = window.scrollY;

        // Устанавливаем таймер показа с задержкой 1 секунда
        submenuShowTimeoutRef.current = setTimeout(() => {
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;

            const submenuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
            const submenuHeight = Math.min(500, viewportHeight * 0.7);

            let top = rect.top + scrollY;
            let left = rect.right + scrollX;

            if (left + submenuWidth > viewportWidth - 20) {
                left = rect.left + scrollX - submenuWidth;
            }
            if (left < 20) left = 20;
            if (top + submenuHeight > viewportHeight + scrollY - 20) {
                top = viewportHeight + scrollY - submenuHeight - 20;
            }
            if (top < scrollY + 20) top = scrollY + 20;

            setSubmenuPosition({ top, left });
            setHoveredSubmenu(submenuId);
        }, 1000);
    }, []);

    const handleSubmenuItemLeave = useCallback(() => {
        // Очищаем таймер показа
        if (submenuShowTimeoutRef.current) {
            clearTimeout(submenuShowTimeoutRef.current);
            submenuShowTimeoutRef.current = null;
        }

        setIsOverSubmenuTrigger(false);
        isOverSubmenuTriggerRef.current = false;

        // Устанавливаем таймер скрытия с увеличенной задержкой
        submenuHideTimeoutRef.current = setTimeout(() => {
            if (!isOverSubmenuRef.current) {
                setHoveredSubmenu(null);
            }
        }, 500);
    }, []);

    const handleSubmenuMouseEnter = useCallback(() => {
        if (submenuHideTimeoutRef.current) {
            clearTimeout(submenuHideTimeoutRef.current);
            submenuHideTimeoutRef.current = null;
        }
        setIsOverSubmenu(true);
        isOverSubmenuRef.current = true;
    }, []);

    const handleSubmenuMouseLeave = useCallback(() => {
        setIsOverSubmenu(false);
        isOverSubmenuRef.current = false;
        submenuHideTimeoutRef.current = setTimeout(() => {
            if (!isOverSubmenuTriggerRef.current) {
                setHoveredSubmenu(null);
            }
        }, 300);
    }, []);

    const handleAddContent = useCallback((menuId, submenuId = null) => {
        setSelectedMenuId(submenuId || menuId);
        setShowAddModal(true);
        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        resetRecording();
        // Мгновенно закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [resetRecording]);

    const cleanupBlobUrls = useCallback(() => {
        // Очищаем текущий URL
        if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
            URL.revokeObjectURL(newItemMediaUrl);
        }

        // Очищаем все blob URLs из ref
        blobUrlsRef.current.forEach(url => {
            if (url && url.startsWith('blob:')) {
                URL.revokeObjectURL(url);
            }
        });
        blobUrlsRef.current = [];
    }, [newItemMediaUrl]);

    const handleSaveContent = useCallback(() => {
        if (!newItemTitle.trim()) return;

        try {
            const updatedData = { ...menuData };

            if (editingItem) {
                // Редактирование существующего элемента
                if (!updatedData[selectedMenuId]) {
                    updatedData[selectedMenuId] = [];
                }
                updatedData[selectedMenuId] = updatedData[selectedMenuId].map(item =>
                    item.id === editingItem.id
                        ? {
                            ...item,
                            title: newItemTitle,
                            description: newItemDescription,
                            mediaUrl: newItemMediaUrl,
                            mediaType: newItemMediaType,
                            editedAt: new Date().toISOString()
                        }
                        : item
                );
                setEditingItem(null);
            } else {
                // Создание нового элемента
                const newItem = {
                    id: Date.now(),
                    title: newItemTitle,
                    description: newItemDescription,
                    mediaUrl: newItemMediaUrl,
                    mediaType: newItemMediaType
                };

                if (!updatedData[selectedMenuId]) {
                    updatedData[selectedMenuId] = [];
                }
                updatedData[selectedMenuId].push(newItem);
            }

            saveMenuData(updatedData);

            cleanupBlobUrls();
            setNewItemTitle('');
            setNewItemDescription('');
            setNewItemMediaUrl('');
            setNewItemMediaType('');
            resetRecording();
            setShowAddModal(false);
            setSelectedMenuId(null);
        } catch (err) {
            console.error('Ошибка сохранения контента:', err);
            setError('Не удалось сохранить контент');
        }
    }, [newItemTitle, newItemDescription, newItemMediaUrl, newItemMediaType, menuData, selectedMenuId, editingItem, saveMenuData, cleanupBlobUrls, resetRecording]);

    const handleFileUpload = useCallback((e, type) => {
        try {
            const file = e.target.files[0];
            if (file) {
                const url = URL.createObjectURL(file);
                blobUrlsRef.current.push(url);
                setNewItemMediaUrl(url);
                setNewItemMediaType(type);
            }
        } catch (err) {
            console.error('Ошибка загрузки файла:', err);
            setError('Не удалось загрузить файл');
        }
    }, []);

    // Обработчик записи аудио через хук
    const handleStartRecording = useCallback(async () => {
        try {
            await startRecording();
        } catch (err) {
            setError('Не удалось получить доступ к микрофону');
        }
    }, [startRecording]);

    const handleStopRecording = useCallback(() => {
        stopRecording();
    }, [stopRecording]);

    // Когда хук создаёт blob URL, используем его
    useEffect(() => {
        if (audioBlobUrl) {
            blobUrlsRef.current.push(audioBlobUrl);
            setNewItemMediaUrl(audioBlobUrl);
            setNewItemMediaType('audio');
        }
    }, [audioBlobUrl]);

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handleDeleteItem = useCallback((menuId, itemId, itemTitle) => {
        if (!canEdit) {
            alert('У вас нет прав на удаление');
            return;
        }
        setItemToDelete({ menuId, itemId, itemTitle });
        setShowDeleteConfirm(true);
        // Мгновенно закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [canEdit]);

    const executeDeleteItem = useCallback(() => {
        if (!itemToDelete) return;

        try {
            const updatedData = { ...menuData };
            if (updatedData[itemToDelete.menuId]) {
                updatedData[itemToDelete.menuId] = updatedData[itemToDelete.menuId].filter(
                    item => item.id !== itemToDelete.itemId
                );
                saveMenuData(updatedData);
            }

            setShowDeleteConfirm(false);
            setItemToDelete(null);
        } catch (err) {
            console.error('Ошибка удаления элемента:', err);
            setError('Не удалось удалить элемент');
        }
    }, [itemToDelete, menuData, saveMenuData]);

    const handleEditItem = useCallback((menuId, item) => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование');
            return;
        }
        setEditingItem(item);
        setSelectedMenuId(menuId);
        setNewItemTitle(item.title);
        setNewItemDescription(item.description || '');
        setNewItemMediaUrl(item.mediaUrl || '');
        setNewItemMediaType(item.mediaType || '');
        setShowAddModal(true);
        // Закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [canEdit]);

    const handleDownloadBroadcast = useCallback((item) => {
        if (!isSuperAdmin) {
            alert('Только главный админ может скачивать эфиры');
            return;
        }

        // Если есть mediaUrl, скачиваем файл
        if (item.mediaUrl) {
            const link = document.createElement('a');
            link.href = item.mediaUrl;
            link.download = `${item.title}.${item.mediaType === 'audio' ? 'mp3' : 'mp4'}`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } else {
            alert('У этого эфира нет медиафайла для скачивания');
        }

        // Закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [isSuperAdmin]);

    const handleItemClick = useCallback((item) => {
        // Если в description есть ссылка, открываем её
        if (item.description && item.description.trim()) {
            const url = item.description.trim();
            // Проверяем, начинается ли с http:// или https://
            if (url.startsWith('http://') || url.startsWith('https://')) {
                window.open(url, '_blank');
            } else {
                // Если нет протокола, добавляем https://
                window.open('https://' + url, '_blank');
            }
        }
    }, []);

    const cancelDeleteItem = useCallback(() => {
        setShowDeleteConfirm(false);
        setItemToDelete(null);
    }, []);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    const handleCloseAddModal = useCallback(() => {
        if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
            URL.revokeObjectURL(newItemMediaUrl);
        }
        setShowAddModal(false);
        setSelectedMenuId(null);
        setEditingItem(null);
        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        resetRecording();
    }, [newItemMediaUrl, resetRecording]);

    return (
        <div className="dashboard">
            {/* Компонент для отображения ошибок */}
            {error && (
                <div className="error-container">
                    <div className="error-alert">
                        <div className="error-alert-content">
                            <div className="error-alert-title">Ошибка</div>
                            <div className="error-alert-message">{error}</div>
                        </div>
                        <button className="error-alert-close" onClick={clearError}>
                            ✕
                        </button>
                    </div>
                </div>
            )}
            <div className="navigation-menus-section">
                <div className="navigation-menus-grid">
                    {navigationMenus.map((menu) => (
                        <div
                            key={menu.id}
                            className="navigation-menu-card"
                            onMouseEnter={(e) => handleMenuEnter(menu.id, e)}
                            onMouseLeave={handleCardLeave}
                        >
                            <div className="menu-card-header">
                                <span className="menu-card-title">{menu.title}</span>
                            </div>
                        </div>
                    ))}
                </div>

                {hoveredMenu && currentMenu && (
                    <div
                        className="menu-dropdown-positioned"
                        style={{ top: menuPosition.top, left: menuPosition.left }}
                        onMouseEnter={handleDropdownEnter}
                        onMouseLeave={handleDropdownLeave}
                    >
                        {isAdmin && (
                            <div className="dropdown-add-section">
                                <button
                                    className="dropdown-add-btn"
                                    onClick={() => handleAddContent(hoveredMenu)}
                                >
                                    Добавить
                                </button>
                            </div>
                        )}
                        {currentMenu.items.length > 0 ? (
                            currentMenu.items.map((item) => (
                                <div
                                    key={item.id}
                                    className={`menu-dropdown-item ${item.hasSubmenu ? 'has-submenu' : ''}`}
                                    onClick={(e) => {
                                        if (!item.hasSubmenu) {
                                            handleItemClick(item);
                                        }
                                    }}
                                    onMouseEnter={(e) => {
                                        if (item.hasSubmenu) {
                                            handleSubmenuItemEnter('broadcasts-guests', e);
                                        }
                                    }}
                                    onMouseLeave={() => {
                                        if (item.hasSubmenu) {
                                            handleSubmenuItemLeave();
                                        }
                                    }}
                                    style={{ cursor: 'pointer' }}
                                >
                                    <div className="menu-item-content">
                                        <div>
                                            <div className="menu-item-title">
                                                {item.hasSubmenu && <span className="submenu-icon">📁 </span>}
                                                {item.title}
                                                {item.editedAt && <span className="edited-badge"> (изменено)</span>}
                                            </div>
                                            <div className="menu-item-description">{item.description}</div>
                                        </div>
                                        {canEdit && !item.hasSubmenu && (
                                            <div className="menu-item-actions">
                                                {isSuperAdmin && hoveredMenu === 'broadcasts' && (
                                                    <button
                                                        className="download-item-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleDownloadBroadcast(item);
                                                        }}
                                                        title="Скачать эфир"
                                                    >
                                                        ⬇️
                                                    </button>
                                                )}
                                                <button
                                                    className="edit-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleEditItem(hoveredMenu, item);
                                                    }}
                                                    title="Редактировать"
                                                >
                                                    ✏️
                                                </button>
                                                <button
                                                    className="delete-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDeleteItem(hoveredMenu, item.id, item.title);
                                                    }}
                                                    title="Удалить"
                                                >
                                                    🗑️
                                                </button>
                                            </div>
                                        )}
                                        {item.hasSubmenu && <span className="submenu-arrow">▶</span>}
                                    </div>
                                </div>
                            ))
                        ) : (
                            !isAdmin && <div className="empty-dropdown">Контент пока не добавлен</div>
                        )}
                    </div>
                )}

                {hoveredSubmenu && (
                    <div
                        className="menu-submenu-positioned"
                        style={{ top: submenuPosition.top, left: submenuPosition.left }}
                        onMouseEnter={handleSubmenuMouseEnter}
                        onMouseLeave={handleSubmenuMouseLeave}
                    >
                        {isAdmin && (
                            <div className="submenu-add-section">
                                <button
                                    className="submenu-add-btn"
                                    onClick={() => handleAddContent('broadcasts', 'broadcasts-guests')}
                                >
                                    Добавить эфир с гостем
                                </button>
                            </div>
                        )}
                        {(menuData['broadcasts-guests'] || []).map((item) => (
                            <div
                                key={item.id}
                                className="menu-dropdown-item"
                                onClick={() => handleItemClick(item)}
                                style={{ cursor: 'pointer' }}
                            >
                                <div className="menu-item-content">
                                    <div>
                                        <div className="menu-item-title">
                                            {item.title}
                                            {item.editedAt && <span className="edited-badge"> (изменено)</span>}
                                        </div>
                                        <div className="menu-item-description">{item.description}</div>
                                    </div>
                                    {canEdit && (
                                        <div className="menu-item-actions">
                                            {isSuperAdmin && (
                                                <button
                                                    className="download-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDownloadBroadcast(item);
                                                    }}
                                                    title="Скачать эфир"
                                                >
                                                    ⬇️
                                                </button>
                                            )}
                                            <button
                                                className="edit-item-btn"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleEditItem('broadcasts-guests', item);
                                                }}
                                                title="Редактировать"
                                            >
                                                ✏️
                                            </button>
                                            <button
                                                className="delete-item-btn"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDeleteItem('broadcasts-guests', item.id, item.title);
                                                }}
                                                title="Удалить"
                                            >
                                                🗑️
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        {(!menuData['broadcasts-guests'] || menuData['broadcasts-guests'].length === 0) && !isAdmin && (
                            <div className="empty-submenu">Пока нет эфиров</div>
                        )}
                    </div>
                )}
            </div>

            {/* Модальное окно подтверждения удаления (переиспользуемое) */}
            <ConfirmModal
                isOpen={showDeleteConfirm}
                title="Подтверждение удаления"
                message="Вы действительно хотите удалить эту запись?"
                itemLabel={itemToDelete?.itemTitle || ''}
                onConfirm={executeDeleteItem}
                onCancel={cancelDeleteItem}
            />

            {/* Встроенная модалка добавления контента с записью аудио */}
            <AddContentModal
                isOpen={showAddModal}
                onClose={handleCloseAddModal}
                editingItem={editingItem}
                selectedMenuId={selectedMenuId}
                newItemTitle={newItemTitle}
                setNewItemTitle={setNewItemTitle}
                newItemDescription={newItemDescription}
                setNewItemDescription={setNewItemDescription}
                newItemMediaUrl={newItemMediaUrl}
                setNewItemMediaUrl={setNewItemMediaUrl}
                newItemMediaType={newItemMediaType}
                setNewItemMediaType={setNewItemMediaType}
                isRecording={isRecording}
                recordingTime={recordingTime}
                handleStartRecording={handleStartRecording}
                handleStopRecording={handleStopRecording}
                handleFileUpload={handleFileUpload}
                handleSaveContent={handleSaveContent}
                formatRecordingTime={formatRecordingTime}
                resetRecording={resetRecording}
            />
        </div>
    );
};

export default Dashboard;
