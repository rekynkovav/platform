// Live streaming services — barrel export
// Real services replacing mocks

// Types
export * from './types';

// Real services (replacing mocks)
export { realWebSocket as liveWebSocket } from './realWebSocket';
export { realLiveApi as liveApi } from './realApi';
// Media service: realMediaService now uses actual HLS playback
export { realMediaService as mediaService } from './realMedia';

// Also export mocks for backward compatibility
export { mockWebSocket } from './mockWebSocket';
export { mockLiveApi } from './mockApi';
export { mockMediaService } from './mockMedia';

// Types for TypeScript
export type { RealWebSocketService as LiveWebSocketService } from './realWebSocket';
export type { RealLiveApi as LiveApiService } from './realApi';
export type { RealMediaService as MediaService } from './realMedia';

// Config
export const LIVE_CONFIG = {
  WS_RECONNECT_DELAY: 3000,
  WS_MAX_RECONNECT_ATTEMPTS: 5,
  WS_PING_INTERVAL: 30000,
  CHAT_HISTORY_LIMIT: 200,
  CHAT_MESSAGE_LIMIT: 1000,
  CHAT_RATE_LIMIT: 5,
  PLAYER_BUFFER_TARGET: 10,
  PLAYER_MAX_BUFFER: 30,
  PLAYER_QUALITY_SWITCH_THRESHOLD: 0.75,
  BROADCAST_MAX_DURATION: 24 * 60 * 60,
  BROADCAST_AUTO_ARCHIVE_AFTER: 7 * 24 * 60 * 60,
  DEMO_MODE_ENABLED: false,  // Real mode now
} as const;
