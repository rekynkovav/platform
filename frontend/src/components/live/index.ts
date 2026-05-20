// Главный файл экспорта для live компонентов
// Централизованный экспорт всех компонентов live streaming системы

// Основные компоненты
export { LivePlayer } from './LivePlayer';
export { LiveChat } from './LiveChat';
export { BroadcastControls } from './BroadcastControls';
export { LiveBroadcastView, LiveBroadcastViewWithProvider } from './LiveBroadcastView';

// Типы компонентов
export type { LivePlayerProps } from './LivePlayer';
export type { LiveChatProps } from './LiveChat';
export type { BroadcastControlsProps } from './BroadcastControls';
export type { LiveBroadcastViewProps } from './LiveBroadcastView';

// Утилиты для компонентов
export {
  // Стили
  getPlayerStyles,
  getChatStyles,
  getControlsStyles,
  getBroadcastViewStyles,

  // Вспомогательные функции
  validatePlayerProps,
  validateChatProps,
  validateControlsProps,
  validateBroadcastViewProps,

  // Генераторы
  generatePlayerId,
  generateChatId,
  generateControlsId,

  // Константы
  PLAYER_DEFAULT_PROPS,
  CHAT_DEFAULT_PROPS,
  CONTROLS_DEFAULT_PROPS,
  BROADCAST_VIEW_DEFAULT_PROPS
} from './utils';

// Конфигурация компонентов
export const COMPONENTS_CONFIG = {
  // LivePlayer
  PLAYER: {
    DEFAULT_QUALITY: 'auto' as const,
    DEFAULT_AUTOPLAY: true,
    DEFAULT_MUTED: false,
    DEFAULT_CONTROLS: true,
    MAX_QUALITY: '1080p' as const,
    MIN_QUALITY: '360p' as const,
    SUPPORTED_QUALITIES: ['1080p', '720p', '480p', '360p', 'auto'] as const,
    SUPPORTED_FORMATS: ['hls', 'dash', 'mp4', 'webm'] as const
  },

  // LiveChat
  CHAT: {
    MAX_MESSAGE_LENGTH: 1000,
    MAX_MESSAGES: 200,
    MESSAGE_RATE_LIMIT: 5, // сообщений в секунду
    DEFAULT_SHOW_REACTIONS: true,
    DEFAULT_SHOW_QUESTIONS: true,
    DEFAULT_SHOW_USER_LIST: false,
    DEFAULT_COMPACT: false,
    EMOJI_PICKER_SIZE: 12,
    QUICK_REACTIONS: ['👍', '👏', '❤️', '🔥', '🎉', '🤔', '😂', '😮'] as const
  },

  // BroadcastControls
  CONTROLS: {
    DEFAULT_SHOW_ADVANCED: false,
    ALLOWED_STATUS_CHANGES: {
      'idle': ['preparing'],
      'preparing': ['live', 'idle'],
      'live': ['paused', 'ended'],
      'paused': ['live', 'ended'],
      'ended': [],
      'failed': ['preparing']
    } as const,
    QUALITY_OPTIONS: ['1080p', '720p', '480p', '360p', 'auto'] as const,
    TEST_FUNCTIONS_ENABLED: true
  },

  // LiveBroadcastView
  BROADCAST_VIEW: {
    DEFAULT_LAYOUT: 'player-first' as const,
    DEFAULT_SHOW_CHAT: true,
    DEFAULT_SHOW_CONTROLS: true,
    DEFAULT_SHOW_STATS: false,
    DEFAULT_AUTOPLAY: true,
    DEFAULT_DEMO_MODE: false,
    LAYOUT_OPTIONS: ['player-first', 'chat-first', 'vertical', 'horizontal'] as const,
    TAB_OPTIONS: ['player', 'chat', 'controls', 'stats'] as const
  }
} as const;

// Хелперы для работы с компонентами
export const ComponentHelpers = {
  // LivePlayer
  getPlayerDefaultProps: () => ({
    autoPlay: COMPONENTS_CONFIG.PLAYER.DEFAULT_AUTOPLAY,
    muted: COMPONENTS_CONFIG.PLAYER.DEFAULT_MUTED,
    controls: COMPONENTS_CONFIG.PLAYER.DEFAULT_CONTROLS
  }),

  getPlayerQualityOptions: () => COMPONENTS_CONFIG.PLAYER.SUPPORTED_QUALITIES,

  // LiveChat
  getChatDefaultProps: () => ({
    showReactions: COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_REACTIONS,
    showQuestions: COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_QUESTIONS,
    showUserList: COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_USER_LIST,
    compact: COMPONENTS_CONFIG.CHAT.DEFAULT_COMPACT
  }),

  getQuickReactions: () => COMPONENTS_CONFIG.CHAT.QUICK_REACTIONS,

  // BroadcastControls
  getControlsDefaultProps: () => ({
    showAdvanced: COMPONENTS_CONFIG.CONTROLS.DEFAULT_SHOW_ADVANCED
  }),

  getAllowedStatusChanges: (currentStatus: string) =>
    COMPONENTS_CONFIG.CONTROLS.ALLOWED_STATUS_CHANGES[currentStatus as keyof typeof COMPONENTS_CONFIG.CONTROLS.ALLOWED_STATUS_CHANGES] || [],

  // LiveBroadcastView
  getBroadcastViewDefaultProps: () => ({
    layout: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_LAYOUT,
    showChat: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_CHAT,
    showControls: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_CONTROLS,
    showStats: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_STATS,
    autoPlay: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_AUTOPLAY,
    demoMode: COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_DEMO_MODE
  }),

  getLayoutOptions: () => COMPONENTS_CONFIG.BROADCAST_VIEW.LAYOUT_OPTIONS,
  getTabOptions: () => COMPONENTS_CONFIG.BROADCAST_VIEW.TAB_OPTIONS
};

// Фабрика компонентов
export const ComponentFactory = {
  // Создание LivePlayer
  createPlayer: (props: any) => ({
    type: 'LivePlayer',
    component: LivePlayer,
    props: { ...ComponentHelpers.getPlayerDefaultProps(), ...props },
    config: COMPONENTS_CONFIG.PLAYER
  }),

  // Создание LiveChat
  createChat: (props: any) => ({
    type: 'LiveChat',
    component: LiveChat,
    props: { ...ComponentHelpers.getChatDefaultProps(), ...props },
    config: COMPONENTS_CONFIG.CHAT
  }),

  // Создание BroadcastControls
  createControls: (props: any) => ({
    type: 'BroadcastControls',
    component: BroadcastControls,
    props: { ...ComponentHelpers.getControlsDefaultProps(), ...props },
    config: COMPONENTS_CONFIG.CONTROLS
  }),

  // Создание LiveBroadcastView
  createBroadcastView: (props: any) => ({
    type: 'LiveBroadcastView',
    component: LiveBroadcastView,
    props: { ...ComponentHelpers.getBroadcastViewDefaultProps(), ...props },
    config: COMPONENTS_CONFIG.BROADCAST_VIEW
  }),

  // Создание полной трансляции
  createFullBroadcast: (broadcast: any, options: any = {}) => ({
    type: 'FullBroadcast',
    components: {
      player: ComponentFactory.createPlayer({ broadcast, ...options.player }),
      chat: ComponentFactory.createChat({ broadcastId: broadcast.id, ...options.chat }),
      controls: ComponentFactory.createControls({ broadcast, ...options.controls }),
      view: ComponentFactory.createBroadcastView({ broadcast, ...options.view })
    },
    layout: options.layout || COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_LAYOUT
  })
};

// Утилиты для валидации
export const ComponentValidators = {
  // Валидация LivePlayer
  validatePlayer: (props: any) => {
    const errors: string[] = [];

    if (!props.broadcast) {
      errors.push('Трансляция обязательна');
    }

    if (props.autoPlay && typeof props.autoPlay !== 'boolean') {
      errors.push('autoPlay должен быть boolean');
    }

    if (props.muted && typeof props.muted !== 'boolean') {
      errors.push('muted должен быть boolean');
    }

    if (props.controls && typeof props.controls !== 'boolean') {
      errors.push('controls должен быть boolean');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  },

  // Валидация LiveChat
  validateChat: (props: any) => {
    const errors: string[] = [];

    if (!props.broadcastId) {
      errors.push('ID трансляции обязателен');
    }

    if (props.maxHeight && typeof props.maxHeight !== 'string') {
      errors.push('maxHeight должен быть строкой');
    }

    if (props.showReactions && typeof props.showReactions !== 'boolean') {
      errors.push('showReactions должен ��ыть boolean');
    }

    if (props.showQuestions && typeof props.showQuestions !== 'boolean') {
      errors.push('showQuestions должен быть boolean');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  },

  // Валидация BroadcastControls
  validateControls: (props: any) => {
    const errors: string[] = [];

    if (!props.broadcast) {
      errors.push('Трансляция обязательна');
    }

    if (props.showAdvanced && typeof props.showAdvanced !== 'boolean') {
      errors.push('showAdvanced должен быть boolean');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  },

  // Валидация LiveBroadcastView
  validateBroadcastView: (props: any) => {
    const errors: string[] = [];

    if (!props.broadcastId && !props.broadcast) {
      errors.push('Требуется broadcastId или broadcast');
    }

    if (props.layout && !COMPONENTS_CONFIG.BROADCAST_VIEW.LAYOUT_OPTIONS.includes(props.layout)) {
      errors.push(`Недопустимый layout. Допустимые значения: ${COMPONENTS_CONFIG.BROADCAST_VIEW.LAYOUT_OPTIONS.join(', ')}`);
    }

    if (props.showChat && typeof props.showChat !== 'boolean') {
      errors.push('showChat должен быть boolean');
    }

    if (props.showControls && typeof props.showControls !== 'boolean') {
      errors.push('showControls должен быть boolean');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }
};

// Хуки для компонентов
export const useComponentHooks = {
  // Хук для LivePlayer
  usePlayer: (broadcast: any, options: any = {}) => {
    const playerProps = {
      broadcast,
      autoPlay: options.autoPlay ?? COMPONENTS_CONFIG.PLAYER.DEFAULT_AUTOPLAY,
      muted: options.muted ?? COMPONENTS_CONFIG.PLAYER.DEFAULT_MUTED,
      controls: options.controls ?? COMPONENTS_CONFIG.PLAYER.DEFAULT_CONTROLS,
      onError: options.onError,
      onReady: options.onReady,
      onPlay: options.onPlay,
      onPause: options.onPause,
      onEnded: options.onEnded
    };

    const validation = ComponentValidators.validatePlayer(playerProps);

    return {
      component: LivePlayer,
      props: playerProps,
      validation,
      config: COMPONENTS_CONFIG.PLAYER
    };
  },

  // Хук для LiveChat
  useChat: (broadcastId: string, options: any = {}) => {
    const chatProps = {
      broadcastId,
      maxHeight: options.maxHeight,
      showReactions: options.showReactions ?? COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_REACTIONS,
      showQuestions: options.showQuestions ?? COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_QUESTIONS,
      showUserList: options.showUserList ?? COMPONENTS_CONFIG.CHAT.DEFAULT_SHOW_USER_LIST,
      compact: options.compact ?? COMPONENTS_CONFIG.CHAT.DEFAULT_COMPACT
    };

    const validation = ComponentValidators.validateChat(chatProps);

    return {
      component: LiveChat,
      props: chatProps,
      validation,
      config: COMPONENTS_CONFIG.CHAT
    };
  },

  // Хук для BroadcastControls
  useControls: (broadcast: any, options: any = {}) => {
    const controlsProps = {
      broadcast,
      onUpdate: options.onUpdate,
      onStatusChange: options.onStatusChange,
      showAdvanced: options.showAdvanced ?? COMPONENTS_CONFIG.CONTROLS.DEFAULT_SHOW_ADVANCED
    };

    const validation = ComponentValidators.validateControls(controlsProps);

    return {
      component: BroadcastControls,
      props: controlsProps,
      validation,
      config: COMPONENTS_CONFIG.CONTROLS
    };
  },

  // Хук для LiveBroadcastView
  useBroadcastView: (broadcastOrId: any, options: any = {}) => {
    const props = typeof broadcastOrId === 'string'
      ? { broadcastId: broadcastOrId, ...options }
      : { broadcast: broadcastOrId, ...options };

    const broadcastViewProps = {
      broadcastId: props.broadcastId,
      broadcast: props.broadcast,
      autoPlay: props.autoPlay ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_AUTOPLAY,
      showChat: props.showChat ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_CHAT,
      showControls: props.showControls ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_CONTROLS,
      showStats: props.showStats ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_SHOW_STATS,
      layout: props.layout ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_LAYOUT,
      demoMode: props.demoMode ?? COMPONENTS_CONFIG.BROADCAST_VIEW.DEFAULT_DEMO_MODE,
      onError: props.onError,
      onReady: props.onReady
    };

    const validation = ComponentValidators.validateBroadcastView(broadcastViewProps);

    return {
      component: LiveBroadcastView,
      props: broadcastViewProps,
      validation,
      config: COMPONENTS_CONFIG.BROADCAST_VIEW
    };
  }
};

// Экспорт по умолчанию для удобного импорта
export default {
  // Компоненты
  LivePlayer,
  LiveChat,
  BroadcastControls,
  LiveBroadcastView,
  LiveBroadcastViewWithProvider,

  // Конфигурация
  COMPONENTS_CONFIG,
  ComponentHelpers,
  ComponentFactory,
  ComponentValidators,
  useComponentHooks,

  // Утилиты
  getPlayerStyles: () => ({}),
  getChatStyles: () => ({}),
  getControlsStyles: () => ({}),
  getBroadcastViewStyles: () => ({}),

  // Инициализация
  initializeComponents: (options: any = {}) => {
    console.log('[LiveComponents] Инициализация компонентов...');

    const config = {
      player: {
        ...COMPONENTS_CONFIG.PLAYER,
        ...options.player
      },
      chat: {
        ...COMPONENTS_CONFIG.CHAT,
        ...options.chat
      },
      controls: {
        ...COMPONENTS_CONFIG.CONTROLS,
        ...options.controls
      },
      broadcastView: {
        ...COMPONENTS_CONFIG.BROADCAST_VIEW,
        ...options.broadcastView
      }
    };

    console.log('[LiveComponents] Конфигурация загружена:', config);
    return config;
  }
};