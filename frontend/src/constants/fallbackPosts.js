// Начальные посты для ленты (используются при ошибке загрузки с сервера)

const fallbackPosts = [
    {
        id: 1,
        type: 'text',
        title: '📄 О стремлении человека не есть',
        content: 'Понимание истинных потребностей тела и духа. Человек может существовать без физической пищи, черпая энергию из духовных источников.',
        date: '2025-03-15',
        author: 'Admin'
    },
    {
        id: 2,
        type: 'text',
        title: '📄 Основа сила человека - дух',
        content: 'Духовная сила - это основа всего. Развивая дух, мы развиваем себя.',
        date: '2025-03-16',
        author: 'Admin'
    },
    {
        id: 3,
        type: 'audio',
        title: '🎵 Эфир - 01.01.2025',
        content: 'Новогодний эфир. Подведение итогов и планы на будущее.',
        url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
        date: '2025-01-01',
        author: 'Admin'
    },
    {
        id: 4,
        type: 'audio',
        title: '🎵 Эфир - 13.01.2025',
        content: 'Практики дыхания и медитации.',
        url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
        date: '2025-01-13',
        author: 'Admin'
    },
    {
        id: 5,
        type: 'audio',
        title: '🎵 Эфир - 22.01.2025',
        content: 'Сила движения и его значение в жизни.',
        url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
        date: '2025-01-22',
        author: 'Admin'
    },
    {
        id: 6,
        type: 'text',
        title: '📄 Значение движения',
        content: 'Движение - это жизнь. Ежедневная физическая активность необходима для поддержания здоровья и энергии.',
        date: '2025-03-17',
        author: 'Admin'
    },
    {
        id: 7,
        type: 'text',
        title: '📄 Дыхание - основа жизни человека',
        content: 'Правильное дыхание - ключ к здоровью и долголетию.',
        date: '2025-03-18',
        author: 'Admin'
    },
    {
        id: 8,
        type: 'text',
        title: '📄 О силе человека',
        content: 'Источники внутренней силы. Как развивать и сохранять силу духа.',
        date: '2025-03-19',
        author: 'Admin'
    }
];

export default fallbackPosts;
