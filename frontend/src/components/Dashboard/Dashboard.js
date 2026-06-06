import React, { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import './Dashboard.css';
import { dashboardApi } from '../../services/api';
import useAudioRecorder from '../../hooks/useAudioRecorder';
import { usePermissions } from '../../hooks/usePermissions';
import ConfirmModal from '../common/ConfirmModal';
import AddContentModal from '../common/AddContentModal';
import InfoModal from '../common/InfoModal';
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

    // Состояния для модального окна информации
    const [showInfo, setShowInfo] = useState(false);
    const [infoTitle, setInfoTitle] = useState('');
    const [infoMessage, setInfoMessage] = useState('');

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

    const showInfoModal = (title, message) => {
        setInfoTitle(title);
        setInfoMessage(message);
        setShowInfo(true);
    };

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
                    mediaType: '