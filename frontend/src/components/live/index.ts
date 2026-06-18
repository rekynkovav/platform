/**
 * Live streaming components — barrel export.
 *
 * Provides a single import point for all live broadcast UI components,
 * their prop types, and convenience re-exports of services/hooks.
 *
 * Components:
 * - {@link LivePlayer} — HLS/DASH video player with quality switching
 * - {@link LiveChat} — Real-time chat with reactions and Q&A
 * - {@link BroadcastControls} — Admin/moderator broadcast management
 * - {@link LiveBroadcastView} — Combined view with player, chat, and controls
 *
 * @module components/live
 */

// Components
export { LivePlayer } from './LivePlayer';
export { LiveChat } from './LiveChat';
export { BroadcastControls } from './BroadcastControls';
export { default as LiveBroadcastView } from './LiveBroadcastViewSimple';

// Component prop types
export type { LivePlayerProps } from './LivePlayer';
export type { LiveChatProps } from './LiveChat';
export type { BroadcastControlsProps } from './BroadcastControls';

// BroadcastView props (used by utils.ts)
import { LiveBroadcast } from '../../services/live/types';

/**
 * Props for the combined LiveBroadcastView component.
 */
export interface LiveBroadcastViewProps {
  broadcastId?: string;
  broadcast: LiveBroadcast | null;
  autoPlay?: boolean;
  showChat?: boolean;
  showControls?: boolean;
  showStats?: boolean;
  layout?: 'player-first' | 'chat-first' | 'side-by-side';
  demoMode?: boolean;
}

// Re-export services for convenience
export { liveApi, liveWebSocket, mediaService } from '../../services/live';
export { useLiveSession, useBroadcast, useChatMessages, usePlayerState } from '../../context/live/LiveSessionContext';
export { useLiveBroadcast, useLiveBroadcasts } from '../../hooks/live/useLiveBroadcast';
export { useLiveChat } from '../../hooks/live/useLiveChat';
