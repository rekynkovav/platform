/**
 * Chat room configuration — single source of truth.
 *
 * `rooms` lists backend slugs accessible from that route.
 * `showSwitcher: true` renders the room-switcher bar when the user
 * has permission to more than one room.
 */

export const CHAT_CONFIG = {
  /** /chat — user1 + user2 with switcher for USER_2+ */
  general: {
    rooms: [
      { slug: 'user1', label: 'Основной', minLevel: 1 },
      { slug: 'user2', label: 'Продвинутый', minLevel: 3 },
    ],
    defaultRoom: 'user1',
    showSwitcher: true,
  },

  /** /chat/nutrition — single room, no switcher */
  nutrition: {
    rooms: [{ slug: 'nutrition', label: 'Чат Питание', minLevel: 1 }],
    defaultRoom: 'nutrition',
    showSwitcher: false,
  },
};

/** Human-readable title per room slug. */
export const ROOM_TITLES = {
  user1: 'Общий чат',
  user2: 'Чат продвинутых',
  nutrition: 'Чат Питание',
};
