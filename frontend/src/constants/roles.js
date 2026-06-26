export const ROLES = {
    SUPER_ADMIN: 'SUPER_ADMIN',
    VYACHESLAV: 'VYACHESLAV',
    MODERATOR_ADMIN: 'MODERATOR_ADMIN',
    DEVELOPER: 'DEVELOPER',
    CONSULTANT_PITANIE: 'CONSULTANT_PITANIE',
    CONSULTANT_SPORT: 'CONSULTANT_SPORT',
    PREMIUM: 'PREMIUM',
    USER_2: 'USER_2',
    USER_1: 'USER_1',
    USER_0: 'USER_0'
};

export const ROLE_LEVELS = {
    [ROLES.USER_0]: 1,
    [ROLES.USER_1]: 2,
    [ROLES.USER_2]: 3,
    [ROLES.PREMIUM]: 3,
    [ROLES.CONSULTANT_PITANIE]: 4,
    [ROLES.CONSULTANT_SPORT]: 4,
    [ROLES.DEVELOPER]: 5,
    [ROLES.MODERATOR_ADMIN]: 6,
    [ROLES.VYACHESLAV]: 7,
    [ROLES.SUPER_ADMIN]: 8
};

export const ROLE_LABELS = {
    [ROLES.USER_0]: 'Пользователь Уровень 0',
    [ROLES.USER_1]: 'Пользователь Уровень 1',
    [ROLES.USER_2]: 'Пользователь Уровень 2',
    [ROLES.PREMIUM]: 'Премиум пользователь',
    [ROLES.CONSULTANT_PITANIE]: 'Консультант по питанию',
    [ROLES.CONSULTANT_SPORT]: 'Консультант по спорту',
    [ROLES.DEVELOPER]: 'Разработчик',
    [ROLES.MODERATOR_ADMIN]: 'Модератор / Администратор',
    [ROLES.VYACHESLAV]: 'Владелец Платформы',
    [ROLES.SUPER_ADMIN]: 'Главный Администратор'
};

export const ROLE_ICONS = {
    [ROLES.USER_0]: '👤',
    [ROLES.USER_1]: '📱',
    [ROLES.USER_2]: '📹',
    [ROLES.PREMIUM]: '⭐',
    [ROLES.CONSULTANT_PITANIE]: '🥗',
    [ROLES.CONSULTANT_SPORT]: '🏋️',
    [ROLES.DEVELOPER]: '💻',
    [ROLES.MODERATOR_ADMIN]: '🛡️',
    [ROLES.VYACHESLAV]: '👑',
    [ROLES.SUPER_ADMIN]: '🔱'
};