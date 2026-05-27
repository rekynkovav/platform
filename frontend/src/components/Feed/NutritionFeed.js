import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { getUserRoles } from '../../keycloak';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import ContentModal from '../Common/ContentModal';
import { feedApi } from '../../services/api';
import './MainFeed.css';
import './NutritionFeed.css';

// Генератор тестовых постов для меню питания
const generateTestPosts = (menuId, count) => {
    const posts = [];
    const titles = {
        nutrition: [
            'Белковый завтрак', 'Углеводное окно', 'Жиры в рационе', 'Вода и пищеварение', 'Витамин D',
            'Железо и энергия', 'Клетчатка', 'Омега-3', 'Антиоксиданты', 'Микроэлементы',
            'Кислотно-щелочной баланс', 'Гликемический индекс', 'Инсулиновый отклик', 'Метаболизм', 'Детокс',
            'Пищевые добавки', 'Завтрак чемпионов', 'Правильный ужин', 'Перекусы', 'Режим питания',
            'Размер порций', 'Калорийность', 'Макросы', 'Сбалансированность', 'Сезонные продукты',
            'Органические продукты', 'ГМО', 'Пищевая ценность', 'Энергетический баланс', 'Питание и сон'
        ],
        recipes: [
            'Салат Цезарь', 'Окрошка', 'Борщ', 'Пельмени', 'Рагу',
            'Запеканка', 'Суп-пюре', 'Стейк', 'Ризотто', 'Паста',
            'Греческий салат', 'Винегрет', 'Щи', 'Уха', 'Котлеты',
            'Тефтели', 'Голубцы', 'Ленивые вареники', 'Сырники', 'Омлет',
            'Яичница', 'Блины', 'Оладьи', 'Вареники', 'Пирог',
            'Шарлотка', 'Торт', 'Пирожные', 'Мороженое', 'Компот'
        ],
        'living-kitchen': [
            'Сыроедение', 'Веганство', 'Кето-диета', 'Палео', 'Интервальное голодание',
            'Монодиеты', 'Раздельное питание', 'Дробное питание', 'Смузи', 'Соки',
            'Фреши', 'Зеленые коктейли', 'Проростки', 'Ферментация', 'Квашение',
            'Маринование', 'Засолка', 'Сушка', 'Замораживание', 'Консервирование',
            'Термообработка', 'Пароварка', 'Мультиварка', 'Аэрогриль', 'Тандыр',
            'Мангал', 'Барбекю', 'Традиционная кухня', 'Авторские блюда', 'Фьюжн'
        ]
    };

    const descriptions = {
        nutrition: [
            'Важный элемент здорового рациона. Рекомендуется употреблять ежедневно для поддержания энергии.',
            'Помогает восстановить силы после физических нагрузок. Оптимальное время приёма - 30 минут после тренировки.',
            'Незаменимые жирные кислоты для работы мозга и сердечно-сосудистой системы.',
            'Основа жизни. Рекомендуется пить не менее 2 литров в день.',
            'Солнечный витамин. Особенно важен в зимний период для жителей северных широт.',
            'Отвечает за транспорт кислорода. Часто встречается дефицит у вегетарианцев.',
            'Пищевые волокна для здорового пищеварения и очищения организма.',
            'Полиненасыщенные жирные кислоты. Содержатся в жирной рыбе и льняном масле.',
            'Защищают клетки от окисления. Больше всего в ягодах и тёмном шоколаде.',
            'Цинк, магний, калий - важные микроэлементы для работы нервной системы.',
            'Баланс pH в организме. Нарушение может приводить к различным заболеваниям.',
            'Показатель скорости повышения сахара в крови после приёма пищи.',
            'Важен для контроля аппетита и профилактики диабета 2 типа.',
            'Ускорение обмена веществ. С возрастом замедляется, но можно поддерживать.',
            'Естественное очищение организма от токсинов и шлаков.',
            'Биологически активные добавки. Помогают восполнить дефицит нутриентов.',
            'Зарядит энергией на весь день. Идеально для активных людей.',
            'Лёгкий ужин способствует качественному сну и восстановлению.',
            'Полезные перекусы между основными приёмами пищи.',
            'Регулярность приёмов пищи влияет на метаболизм и самочувствие.',
            'Контроль порций помогает избежать переедания и лишнего веса.',
            'Подсчёт калорий - основа контроля питания.',
            'Белки, жиры, углеводы - три кита сбалансированного рациона.',
            'Баланс нутриентов обеспечивает нормальное функционирование организма.',
            'Сезонные продукты содержат максимум полезных веществ.',
            'Выращенные без химии продукты безопаснее и вкуснее.',
            'Генетически модифицированные организмы - за и против.',
            'Белки, жиры, углеводы, витамины - всё, что нужно знать о составе.',
            'Баланс между потреблением и расходом энергии.',
            'Правильное питание улучшает качество сна и общее самочувствие.'
        ],
        recipes: [
            'Классический салат с курицей, сыром пармезан и соусом цезарь.',
            'Освежающий холодный суп на кефире с овощами и зеленью.',
            'Традиционный украин��кий суп со свеклой и сметаной.',
            'Аппетитные пельмени с мясом - любимое блюдо многих.',
            'Тушёные овощи с мясом в духовке - полезно и вкусно.',
            'Запеканка из творога или картофеля - простое семейное блюдо.',
            'Нежный крем-суп из овощей или грибов.',
            'Сочный стейк из говядины или рыбы на гриле.',
            'Итальянское блюдо из риса с овощами и сыром.',
            'Паста с различными соусами - быстро и сытно.',
            'Свежий салат с огурцами, помидорами, оливками и фетой.',
            'Свёкла с морковью и картофелем под маслом.',
            'Щи из свежей капусты - традиционное русское блюдо.',
            'Рыбный суп из речной или морской рыбы.',
            'Котлеты из фарша с хлебом и луком - классика.',
            'Шарики из фарша с рисом в томатном соусе.',
            'Ленивые голубцы с капустой и мясом.',
            'Творожные вареники - вкусный десерт или завтрак.',
            'Творожные оладьи на завтрак - любят и взрослые, и дети.',
            'Омлет с зеленью и овощами - идеальный завтрак.',
            'Яичница с беконом, помидорами или грибами.',
            'Тонкие блины на молоке или кефире.',
            'Пышные оладьи на дрожжах или кефире.',
            'Вареники с вишней или картошкой - традиционное блюдо.',
            'Яблочный пирог из дрожжевого или песочного теста.',
            'Пирог с яблоками и корицей - ароматный десерт.',
            'Торт на день рождения - бисквит с кремом.',
            'Пирожные эклеры, макаруны, картошка.',
            'Домашнее мороженое из сливок и мороженого.',
            'Напиток из сухофруктов - полезный и вкусный.'
        ],
        'living-kitchen': [
            'Питание без термической обработки сохраняет все ферменты и витамины.',
            'Отказ от продуктов животного происхождения - тренд современности.',
            'Диета с высоким содержанием жиров и низким содержанием углеводов.',
            'Питание как у древних людей - мясо, овощи, фрукты.',
            'Чередование периодов еды и голода - новый подход к питанию.',
            'Монодиеты на одном продукте - быстрый результат, но не для всех.',
            'Раздельное употребление белков и углеводов для лучшего пищеварения.',
            'Частые приёмы пищи небольшими порциями.',
            'Напитки из фруктов и овощей - коктейли здоровья.',
            'Свежевыжатые соки - концентрированная польза.',
            'Фреши из зелени и овощей - детокс-напитки.',
            'Зелёные коктейли с шпинатом, сельдереем и фруктами.',
            'Проростки семян - концентрированная энергия жизни.',
            'Квашеная капуста, кимчи - ферментированные продукты.',
            'Квашение - древний способ консервации и обогащения витаминами.',
            'Маринованные овощи - вкусно и полезно.',
            'Засолка рыбы и мяса - традиционный метод консервации.',
            'Сушка овощей, фруктов, грибов на зиму.',
            'Быстрая заморозка сохраняет максимум полезных веществ.',
            'Консервирование в банках - запасы на зиму.',
            'Минимальная обработка сохраняет питательную ценность.',
            'Приготовление на пару - здоровый способ термообработки.',
            'Умный помощник на кухне - готовит без масла.',
            'Готовим без жира на горячем воздухе.',
            'Традиционная восточная печь для выпечки хлеба и мяса.',
            'Приготовление мяса и овощей на углях.',
            'Американский метод приготовления на открытом огне.',
            'Народные рецепты разных стран мира.',
            'Авторские рецепты шеф-поваров.',
            'Смешение кухонь разных народов - кулинарное искусство.'
        ]
    };

    for (let i = 0; i < count; i++) {
        posts.push({
            id: Date.now() + i,
            title: titles[menuId][i] || `Пункт ${i + 1}`,
            description: descriptions[menuId][i] || 'Описание пункта меню'
        });
    }
    return posts;
};

const NutritionFeed = () => {
    const [posts, setPosts] = useState([]);
    const [newPost, setNewPost] = useState({ type: 'text', title: '', content: '', url: '' });
    const [showAddForm, setShowAddForm] = useState(false);
    const [userRoles, setUserRoles] = useState([]);
    const [notification, setNotification] = useState(null);
    const [error, setError] = useState(null);
    const clearError = () => {
        setError(null);
    };
    const feedEndRef = useRef(null);
    const addFormRef = useRef(null);
    const [activeAudioPostId, setActiveAudioPostId] = useState(null);
    const [, forceUpdate] = useState(0);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDelete, setPostToDelete] = useState(null);

    // Состояния для редактирования
    const [editingPost, setEditingPost] = useState(null);
    const [editingMenuItem, setEditingMenuItem] = useState(null);

    // Состояние для ContentModal
    const [showContentModal, setShowContentModal] = useState(false);

    // Состояния для навигационных меню
    const [hoveredMenu, setHoveredMenu] = useState(null);
    const [isOverCard, setIsOverCard] = useState(false);
    const [isOverDropdown, setIsOverDropdown] = useState(false);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedMenuId, setSelectedMenuId] = useState(null);
    const [menuData, setMenuData] = useState({});
    const [itemToDelete, setItemToDelete] = useState(null);
    const [showDeleteItemConfirm, setShowDeleteItemConfirm] = useState(false);
    const hideTimeoutRef = useRef(null);
    const blobUrlsRef = useRef([]);

    const {
        playTrack,
        pauseTrack,
        currentTrack,
        isPlaying,
        stopTrack,
        volume,
        playbackRate,
        setVolume,
        setPlaybackRate,
        seek,
        currentTime,
        duration,
        formatTime
    } = useAudioPlayer();

    // Функция для обновления ролей из sessionStorage (только для development)
    const updateRoles = () => {
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        let roles = [];

        if (demoRole === 'MODERATOR_ADMIN' || demoRole === 'CONSULTANT_PITANIE' || demoRole === 'DEVELOPER' || demoRole === 'SUPER_ADMIN' || demoRole === 'VYACHESLAV') {
            roles = ['MODERATOR_ADMIN', 'USER_0'];
        } else if (demoRole === 'USER_0' || demoRole === 'USER_1' || demoRole === 'USER_2') {
            roles = ['USER_0'];
        } else {
            roles = getUserRoles();
        }

        setUserRoles(roles);
        return roles;
    };

    // Синхронизируем activeAudioPostId с currentTrack
    useEffect(() => {
        if (currentTrack) {
            setActiveAudioPostId(currentTrack.id);
        } else if (!currentTrack && activeAudioPostId) {
            const timer = setTimeout(() => {
                setActiveAudioPostId(null);
            }, 500);
            return () => clearTimeout(timer);
        }
    }, [currentTrack]);

    // Загрузка постов и ролей
    useEffect(() => {
        const loadPosts = async () => {
            try {
                const data = await feedApi.getNutritionFeed();
                setPosts(data);
            } catch (err) {
                console.error('Failed to load nutrition feed:', err);
                setError('Не удалось загрузить ленту питания');
                // Fallback к демо-данным
                const initialPosts = [
                    {
                        id: 1,
                        type: 'text',
                        title: '🥗 Основы здорового питания',
                        content: 'Правильное питание - основа здоровья. Узнайте о принципах сбалансированного рациона.',
                        date: '2025-03-15',
                        author: 'Admin'
                    },
                    {
                        id: 2,
                        type: 'text',
                        title: '🍎 Польза фруктов',
                        content: 'Фрукты - источник витаминов и минералов. Включайте их в свой ежедневный рацион.',
                        date: '2025-03-16',
                        author: 'Admin'
                    }
                ];
                setPosts(initialPosts);
            }
        };

        loadPosts();
        updateRoles();
    }, []);

    useEffect(() => {
        if (feedEndRef.current) {
            feedEndRef.current.scrollIntoView({ behavior: 'auto' });
        }
    }, [posts]);

    useEffect(() => {
        if (showAddForm && addFormRef.current) {
            setTimeout(() => {
                addFormRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 100);
        }
    }, [showAddForm]);

    // Cleanup при размонтировании
    useEffect(() => {
        return () => {
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    // Функция для определения типа файла
    const getFileType = (file) => {
        if (file.type.startsWith('audio/')) return 'audio';
        if (file.type.startsWith('video/')) return 'video';
        return 'text';
    };

    // Обработка загрузки файла
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const fileType = getFileType(file);
        const url = URL.createObjectURL(file);
        blobUrlsRef.current.push(url);

        setNewPost({
            ...newPost,
            type: fileType,
            url: url,
            title: newPost.title || file.name.replace(/\.[^/.]+$/, '')
        });
    };

    const handleAddPost = () => {
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        const isNutritionConsultant = demoRole === 'CONSULTANT_PITANIE';

        if (!isNutritionConsultant) {
            alert('Только консультант питания может добавлять записи');
            return;
        }

        setShowContentModal(true);
    };

    // Функция для открытия модального окна подтверждения
    const confirmDeletePost = (post) => {
        setPostToDelete(post);
        setShowDeleteConfirm(true);
    };

    // Функция для фактического удаления
    const executeDeletePost = async () => {
        if (!postToDelete) return;

        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        const isNutritionConsultant = demoRole === 'CONSULTANT_PITANIE';

        if (!isNutritionConsultant) {
            alert('Только консультант питания может удалять записи');
            setShowDeleteConfirm(false);
            setPostToDelete(null);
            return;
        }

        try {
            await feedApi.deletePost(postToDelete.id);
            setPosts(prev => prev.filter(post => post.id !== postToDelete.id));

            if (currentTrack?.id === postToDelete.id) {
                stopTrack();
            }

        setShowDeleteConfirm(false);
        setPostToDelete(null);
        forceUpdate(prev => prev + 1);
    };

    // Функция для отмены удаления
    const cancelDeletePost = () => {
        setShowDeleteConfirm(false);
        setPostToDelete(null);
    };

    // Функция для начала редактирования поста
    const handleEditPost = (post) => {
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        const isNutritionConsultant = demoRole === 'CONSULTANT_PITANIE';

        if (!isNutritionConsultant) {
            alert('Только консультант питания может редактировать записи');
            return;
        }

        setEditingPost(post);
        setShowContentModal(true);
    };

    // Обновляем handleSavePost для поддержки редактирования
    const handleSavePost = async (data) => {
        try {
            if (editingPost) {
                // Редактирование существующего поста
                const updatedPostData = {
                    type: data.mediaType || 'text',
                    title: data.title,
                    content: data.description,
                    url: data.mediaUrl || '',
                    editedAt: new Date().toISOString()
                };

                const updatedPost = await feedApi.updatePost(editingPost.id, updatedPostData);
                setPosts(prev => prev.map(post =>
                    post.id === editingPost.id ? updatedPost : post
                ));
                setEditingPost(null);
            } else {
                // Создание нового поста
                const postData = {
                    type: data.mediaType || 'text',
                    title: data.title,
                    content: data.description,
                    url: data.mediaUrl || '',
                    date: new Date().toISOString().split('T')[0],
                    author: 'Admin'
                };

                const newPost = await feedApi.createPost(postData);
                setPosts(prev => [...prev, newPost]);
            }

            setShowContentModal(false);

            setTimeout(() => {
                if (feedEndRef.current) {
                    feedEndRef.current.scrollIntoView({ behavior: 'smooth' });
                }
            }, 100);
        } catch (err) {
            console.error('Failed to save post:', err);
            alert('Не удалось сохранить запись');
        }
    };

    // Функция для скачивания материала (только для SUPER_ADMIN)
    const handleDownloadPost = async (post) => {
        const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
            ? sessionStorage.getItem('demoRole')
            : null;
        const isSuperAdmin = demoRole === 'SUPER_ADMIN' || demoRole === 'VYACHESLAV';

        if (!isSuperAdmin) {
            setError('Только главный админ может скачивать материалы');
            return;
        }

        try {
            if (post.type === 'audio' || post.type === 'video') {
                const response = await fetch(post.url);
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${post.title}.${post.type === 'audio' ? 'mp3' : 'mp4'}`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            } else {
                const content = `${post.title}\n\n${post.content}\n\nДата: ${post.date}\nАвтор: ${post.author}`;
                const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${post.title}.txt`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (error) {
            console.error('Ошибка при скачивании:', error);
            setError('Не удалось скачать файл');
        }
    };

    const handlePlayAudio = (post) => {
        if (activeAudioPostId === post.id && currentTrack?.id === post.id && isPlaying) {
            pauseTrack();
        } else if (activeAudioPostId === post.id && currentTrack?.id === post.id && !isPlaying) {
            playTrack({ id: post.id, title: post.title, url: post.url });
        } else {
            setActiveAudioPostId(post.id);
            playTrack({ id: post.id, title: post.title, url: post.url });
        }
    };

    const handleSeek = (e, postId) => {
        const newTime = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            seek(newTime);
        }
    };

    const handleVolumeChangeLocal = (e, postId) => {
        const newVolume = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setVolume(newVolume);
        }
    };

    const handleSpeedChangeLocal = (e, postId) => {
        const newSpeed = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setPlaybackRate(newSpeed);
        }
    };

    const getCurrentTimeForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return currentTime;
        }
        return 0;
    };

    const getDurationForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return duration;
        }
        return 0;
    };

    const isThisPlaying = (postId) => {
        return currentTrack?.id === postId && isPlaying;
    };

    const demoRole = process.env.REACT_APP_ENVIRONMENT === 'development'
        ? sessionStorage.getItem('demoRole')
        : null;
    const isNutritionConsultant = demoRole === 'CONSULTANT_PITANIE';

    // Загрузка данных меню
    useEffect(() => {
        const loadMenuData = async () => {
            try {
                const data = await nutritionApi.getMenuData();
                setMenuData(data);
            } catch (err) {
                console.error('Failed to load menu data:', err);
                // Fallback для разработки
                if (process.env.NODE_ENV === 'development') {
                    const initialData = {
                        nutrition: generateTestPosts('nutrition', 30),
                        recipes: generateTestPosts('recipes', 30),
                        'living-kitchen': generateTestPosts('living-kitchen', 30)
                    };
                    setMenuData(initialData);
                }
            }
        };
        loadMenuData();
    }, []);
    }, []);

    const saveMenuData = useCallback(async (data) => {
        setMenuData(data);
        try {
            await nutritionApi.saveMenuData(data);
        } catch (err) {
            console.error('Failed to save menu data:', err);
            // Fallback для разработки
            if (process.env.NODE_ENV === 'development') {
                if (process.env.NODE_ENV === 'development') {
                    sessionStorage.setItem('dashboardMenuData', JSON.stringify(data));
                }
            }
        }
    }, []);

    const nutritionMenus = useMemo(() => [
        { id: 'nutrition', title: 'Питание', items: menuData['nutrition'] || [] },
        { id: 'recipes', title: 'Рецепты', items: menuData['recipes'] || [] },
        { id: 'living-kitchen', title: 'Живая кухня', items: menuData['living-kitchen'] || [] }
    ], [menuData]);

    const currentMenu = useMemo(() =>
        nutritionMenus.find(m => m.id === hoveredMenu)
    , [nutritionMenus, hoveredMenu]);

    const handleMenuEnter = useCallback((menuId, event) => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        const rect = event.currentTarget.getBoundingClientRect();
        const windowWidth = window.innerWidth;

        // Вычисляем позицию с учетом ширины меню (1200px)
        let leftPos = rect.left + window.scrollX - 320;

        // Если меню выходит за правый край - выравниваем по правому краю
        if (leftPos + 1200 > windowWidth - 20) {
            leftPos = windowWidth - 1200 - 20;
        }
        // Если меню выходит за левый край - выравниваем по левому краю
        if (leftPos < 20) {
            leftPos = 20;
        }

        setMenuPosition({
            top: rect.top + window.scrollY,
            left: leftPos
        });
        setHoveredMenu(menuId);
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

    useEffect(() => {
        if (!isOverCard && !isOverDropdown) {
            const timeout = setTimeout(() => {
                setHoveredMenu(null);
            }, 100);
            hideTimeoutRef.current = timeout;
            return () => clearTimeout(timeout);
        }
    }, [isOverCard, isOverDropdown]);

    const handleAddContent = useCallback((menuId) => {
        setSelectedMenuId(menuId);
        setShowAddModal(true);
        setHoveredMenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, []);

    const handleSaveContent = useCallback((data) => {
        const updatedData = { ...menuData };

        if (editingMenuItem) {
            // Редактирование существующего элемента меню
            const menuId = editingMenuItem.type || selectedMenuId;
            if (updatedData[menuId]) {
                updatedData[menuId] = updatedData[menuId].map(item =>
                    item.id === editingMenuItem.id
                        ? { ...item, title: data.title, description: data.description, mediaUrl: data.mediaUrl, mediaType: data.mediaType }
                        : item
                );
            }
            setEditingMenuItem(null);
        } else {
            // Добавление нового элемента
            if (!updatedData[selectedMenuId]) {
                updatedData[selectedMenuId] = [];
            }
            updatedData[selectedMenuId].push({
                id: Date.now(),
                title: data.title,
                description: data.description,
                mediaUrl: data.mediaUrl,
                mediaType: data.mediaType
            });
        }

        saveMenuData(updatedData);

        setShowAddModal(false);
        setSelectedMenuId(null);
    }, [menuData, selectedMenuId, saveMenuData, editingMenuItem]);

    const handleDeleteItem = useCallback((menuId, itemId, itemTitle) => {
        setItemToDelete({ menuId, itemId, itemTitle });
        setShowDeleteItemConfirm(true);
        setHoveredMenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, []);

    const executeDeleteItem = useCallback(() => {
        if (!itemToDelete) return;

        const updatedData = { ...menuData };
        if (updatedData[itemToDelete.menuId]) {
            updatedData[itemToDelete.menuId] = updatedData[itemToDelete.menuId].filter(
                item => item.id !== itemToDelete.itemId
            );
            saveMenuData(updatedData);
        }

        setShowDeleteItemConfirm(false);
        setItemToDelete(null);
    }, [itemToDelete, menuData, saveMenuData]);

    const cancelDeleteItem = useCallback(() => {
        setShowDeleteItemConfirm(false);
        setItemToDelete(null);
    }, []);

    return (
        <div className="nutrition-feed-container">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            {/* Модальное окно подтверждения удаления элемента меню */}
            {showDeleteItemConfirm && itemToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteItem}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить эту запись?</p>
                            <div className="item-preview">
                                <strong>{itemToDelete.itemTitle}</strong>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteItem}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteItem}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Боковая панель с меню */}
            <div className="nutrition-sidebar">
                {nutritionMenus.map((menu) => (
                    <div
                        key={menu.id}
                        className="nutrition-menu-card"
                        onMouseEnter={(e) => handleMenuEnter(menu.id, e)}
                        onMouseLeave={handleCardLeave}
                    >
                        <span className="nutrition-menu-title">{menu.title}</span>
                    </div>
                ))}

                {hoveredMenu && currentMenu && (
                    <div
                        className="nutrition-menu-dropdown"
                        style={{ top: menuPosition.top, left: menuPosition.left }}
                        onMouseEnter={handleDropdownEnter}
                        onMouseLeave={handleDropdownLeave}
                    >
                        {isNutritionConsultant && (
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
                                <div key={item.id} className="menu-dropdown-item">
                                    <div className="menu-item-content">
                                        <div>
                                            <div className="menu-item-title">{item.title}</div>
                                            <div className="menu-item-description">{item.description}</div>
                                        </div>
                                        {isNutritionConsultant && (
                                                <div className="menu-item-actions">
                                                <button
                                                    className="edit-item-btn"
                                                    onClick={() => {
                                                        setEditingMenuItem({ ...item, type: hoveredMenu });
                                                        setShowAddModal(true);
                                                        setHoveredMenu(null);
                                                    }}
                                                    title="Редактировать"
                                                >
                                                    ✏️
                                                </button>
                                                <button
                                                    className="delete-item-btn"
                                                    onClick={() => handleDeleteItem(hoveredMenu, item.id, item.title)}
                                                    title="Удалить"
                                                >
                                                    ✕
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))
                        ) : (
                            !isNutritionConsultant && <div className="empty-dropdown">Контент пока не добавлен</div>
                        )}
                    </div>
                )}
            </div>

            {/* Основная лента */}
            <div className="main-feed">
            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && postToDelete && (
                <div className="modal-overlay" onClick={cancelDeletePost}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить публикацию?</p>
                            <div className="post-preview">
                                <strong>{postToDelete.title}</strong>
                                <small>{postToDelete.date}</small>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeletePost}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeletePost}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {notification && (
                <div className={`event-notification ${notification.includes('удалена') ? 'delete-notification' : ''}`}>
                    <div className="notification-content">
                        <div className="notification-text">
                            <strong>{notification.includes('удалена') ? 'Запись удалена' : 'Новая запись!'}</strong>
                            <span>{notification}</span>
                        </div>
                        <div className="notification-close" onClick={() => setNotification(null)}>✕</div>
                    </div>
                </div>
            )}

            <div className="feed-container">
                <div className="feed-list">
                    {posts.map((post) => (
                        <div key={post.id} className="feed-post">
                            <div className="post-header">
                                <div className="post-title">{post.title}</div>
                                {isNutritionConsultant && (
                                    <div className="post-actions">
                                        {(demoRole === 'SUPER_ADMIN' || demoRole === 'VYACHESLAV') && (
                                            <button
                                                className="download-post"
                                                onClick={() => handleDownloadPost(post)}
                                                title="Скачать материал"
                                            >
                                                📥
                                            </button>
                                        )}
                                        <button
                                            className="edit-post"
                                            onClick={() => handleEditPost(post)}
                                            title="Редактировать запись"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            className="delete-post"
                                            onClick={() => confirmDeletePost(post)}
                                            title="Удалить запись"
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                )}
                            </div>
                            <div className="post-date">{post.date}</div>
                            <div className="post-content">{post.content}</div>

                            {post.type === 'audio' && (
                                <div className="post-audio">
                                    {activeAudioPostId === post.id ? (
                                        <div className="inline-audio-player">
                                            <div className="inline-audio-header">
                                                <span className="inline-audio-title">🎵 {post.title}</span>
                                                <button className="inline-stop-btn" onClick={() => stopTrack()}>
                                                    ✕
                                                </button>
                                            </div>
                                            <div className="inline-progress">
                                                <span>{formatTime(getCurrentTimeForPost(post.id))}</span>
                                                <input
                                                    type="range"
                                                    className="inline-seek"
                                                    min="0"
                                                    max={getDurationForPost(post.id) || 100}
                                                    value={getCurrentTimeForPost(post.id)}
                                                    onChange={(e) => handleSeek(e, post.id)}
                                                />
                                                <span>{formatTime(getDurationForPost(post.id))}</span>
                                            </div>
                                            <div className="inline-controls">
                                                <button className="inline-play" onClick={() => handlePlayAudio(post)}>
                                                    {isThisPlaying(post.id) ? '⏸️' : '▶️'}
                                                </button>
                                                <div className="inline-volume">
                                                    <span>🔊</span>
                                                    <input
                                                        type="range"
                                                        min="0"
                                                        max="1"
                                                        step="0.01"
                                                        value={volume}
                                                        onChange={(e) => handleVolumeChangeLocal(e, post.id)}
                                                    />
                                                </div>
                                                <div className="inline-speed">
                                                    <span>⚡</span>
                                                    <select value={playbackRate} onChange={(e) => handleSpeedChangeLocal(e, post.id)}>
                                                        <option value="0.5">0.5x</option>
                                                        <option value="0.75">0.75x</option>
                                                        <option value="1.0">1.0x</option>
                                                        <option value="1.25">1.25x</option>
                                                        <option value="1.5">1.5x</option>
                                                        <option value="2.0">2.0x</option>
                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <button className="play-audio-btn" onClick={() => handlePlayAudio(post)}>
                                            ▶️ Слушать аудио
                                        </button>
                                    )}
                                </div>
                            )}

                            {post.type === 'video' && post.url && (
                                <div className="post-video">
                                    <video controls width="100%">
                                        <source src={post.url} type="video/mp4" />
                                    </video>
                                </div>
                            )}
                        </div>
                    ))}
                    <div ref={feedEndRef} className="feed-end-marker" />
                </div>

                {isNutritionConsultant && (
                    <div className="admin-section" ref={addFormRef}>
                        <div className="admin-panel">
                            <button className="add-post-btn add-event-style" onClick={handleAddPost}>
                                + Добавить запись
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <ContentModal
                isOpen={showContentModal}
                onClose={() => {
                    setShowContentModal(false);
                    setEditingPost(null);
                }}
                onSave={handleSavePost}
                initialData={editingPost ? {
                    title: editingPost.title,
                    description: editingPost.content,
                    mediaUrl: editingPost.url,
                    mediaType: editingPost.type
                } : null}
                isEditing={!!editingPost}
            />

            {/* Модальное окно для добавления контента в меню */}
            <ContentModal
                isOpen={showAddModal}
                onClose={() => {
                    setShowAddModal(false);
                    setSelectedMenuId(null);
                    setEditingMenuItem(null);
                }}
                onSave={handleSaveContent}
                initialData={editingMenuItem ? {
                    title: editingMenuItem.title,
                    description: editingMenuItem.description,
                    mediaUrl: editingMenuItem.mediaUrl,
                    mediaType: editingMenuItem.mediaType
                } : null}
                isEditing={!!editingMenuItem}
            />
            </div>
        </div>
    );
};

export default NutritionFeed;
