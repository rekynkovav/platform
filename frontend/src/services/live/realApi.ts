/**
 * Real Live API service — calls actual backend endpoints.
 * Replaces mockApi.ts for production use.
 *
 * Broadcast CRUD → backend /api/broadcast/*
 * Chat → backend /api/broadcast/{id}/chat
 * Reactions → backend /api/broadcast/{id}/reaction
 * Linking → backend /api/broadcast/{id}/link/{eventId}
 * Viewer tracking → backend BroadcastStatsWebSocketHandler
 */

import {
  LiveBroadcast,
  StartBroadcastRequest,
  StartBroadcastResponse,
  UpdateBroadcastRequest,
  LiveChatMessage,
  ViewerStats,
  BroadcastStatus,
  StreamType
} from './types';
import { broadcastApi } from '../api';
import { getToken, getUserId, getUsername, getUserRoles } from '../../keycloak';

// User context from Keycloak JWT
function getCurrentUser(): { id: string; username: string; role: string } {
  try {
    const token = getToken();
    if (token) {
      return {
        id: getUserId(),
        username: getUsername(),
        role: getUserRoles().find(
          (r: string) => r.startsWith('SUPER_ADMIN') || r.startsWith('MODERATOR') || r.startsWith('VYACHESLAV')
        ) || 'USER_1'
      };
    }
  } catch {}
  return { id: 'anonymous', username: 'Гость', role: 'USER_1' };
}

// Backend DTO shape (before mapping to LiveBroadcast)
interface BackendBroadcastDto {
  id?: string;
  broadcastId?: string;
  title?: string;
  description?: string;
  streamKey?: string;
  rtmpStreamKey?: string;
  ingestUrl?: string;
  playbackUrl?: string;
  hlsPlaylistUrl?: string;
  streamType?: string;
  status?: string;
  viewerCount?: number;
  maxViewers?: number;
  startedAt?: string;
  endedAt?: string;
  duration?: number;
  isChatEnabled?: boolean;
  isReactionsEnabled?: boolean;
  isRecording?: boolean;
  isPublic?: boolean;
  ownerId?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  linkedEventId?: string;
}

// Backend chat message DTO
interface BackendChatMessageDto {
  id?: string;
  broadcastId?: string;
  userId?: string;
  username?: string;
  userRole?: string;
  content?: string;
  type?: string;
  timestamp?: string;
}

/**
 * Map backend LiveBroadcastDto → frontend LiveBroadcast type.
 * Backend fields don't perfectly match, so we normalize.
 */
function mapBackendBroadcast(raw: BackendBroadcastDto): LiveBroadcast {
  return {
    id: raw.id || raw.broadcastId || '',
    title: raw.title || '',
    description: raw.description || '',
    streamKey: raw.streamKey || raw.rtmpStreamKey || raw.id || '',
    ingestUrl: raw.ingestUrl || '',
    playbackUrl: raw.playbackUrl || raw.hlsPlaylistUrl || '',
    streamType: (raw.streamType as StreamType) || 'video',
    status: normalizeStatus(raw.status),
    viewerCount: raw.viewerCount || 0,
    maxViewers: raw.maxViewers || 0,
    startedAt: raw.startedAt || undefined,
    endedAt: raw.endedAt || undefined,
    duration: raw.duration || undefined,
    settings: {
      isChatEnabled: raw.isChatEnabled ?? true,
      isReactionsEnabled: raw.isReactionsEnabled ?? true,
      isRecording: raw.isRecording ?? false,
      isPublic: raw.isPublic ?? true
    },
    createdBy: raw.ownerId || raw.createdBy || '',
    createdAt: raw.createdAt || new Date().toISOString(),
    updatedAt: raw.updatedAt || undefined,
    linkedEventId: raw.linkedEventId || undefined
  };
}

function normalizeStatus(status: string | undefined): BroadcastStatus {
  const s = (status || '').toUpperCase();
  if (s === 'LIVE') return 'live';
  if (s === 'ENDED') return 'ended';
  if (s === 'IDLE') return 'idle';
  if (s === 'PREPARING') return 'preparing';
  if (s === 'PAUSED') return 'paused';
  return 'idle';
}

/**
 * Real Live API — calls backend REST endpoints.
 */
export class RealLiveApi {
  private static instance: RealLiveApi;

  private constructor() {}

  static getInstance(): RealLiveApi {
    if (!RealLiveApi.instance) {
      RealLiveApi.instance = new RealLiveApi();
    }
    return RealLiveApi.instance;
  }

  // ---- Broadcast CRUD ----

  async startBroadcast(request: StartBroadcastRequest): Promise<StartBroadcastResponse> {
    const response = await broadcastApi.start({
      title: request.title,
      description: request.description || '',
      streamType: request.streamType || 'video'
    });

    const data = response.data;
    const broadcast: LiveBroadcast = {
      id: data.streamKey || '',
      title: request.title,
      description: request.description || '',
      streamKey: data.streamKey || '',
      ingestUrl: data.streamUrl || '',
      playbackUrl: `/api/broadcast/${data.streamKey}/playlist.m3u8`,
      streamType: request.streamType || 'video',
      status: 'live',
      viewerCount: 0,
      maxViewers: 0,
      startedAt: new Date().toISOString(),
      settings: {
        isChatEnabled: request.settings?.isChatEnabled ?? true,
        isReactionsEnabled: request.settings?.isReactionsEnabled ?? true,
        isRecording: request.settings?.isRecording ?? true,
        isPublic: request.settings?.isPublic ?? true
      },
      createdBy: getCurrentUser().id,
      createdAt: new Date().toISOString()
    };

    return {
      broadcast,
      streamKey: broadcast.streamKey,
      ingestUrl: broadcast.ingestUrl,
      playbackUrl: broadcast.playbackUrl
    };
  }

  async getBroadcast(broadcastId: string): Promise<LiveBroadcast> {
    const response = await broadcastApi.getById(broadcastId);
    return mapBackendBroadcast(response.data);
  }

  async updateBroadcast(
    broadcastId: string,
    updates: UpdateBroadcastRequest
  ): Promise<LiveBroadcast> {
    // Backend doesn't have a generic PATCH endpoint for broadcasts yet.
    // For status changes, use start/stop endpoints.
    if (updates.status === 'ended') {
      await broadcastApi.end(broadcastId);
    }

    // Re-fetch to get updated state
    return this.getBroadcast(broadcastId);
  }

  async stopBroadcast(broadcastId: string): Promise<LiveBroadcast> {
    await broadcastApi.end(broadcastId);
    return this.getBroadcast(broadcastId);
  }

  async deleteBroadcast(broadcastId: string): Promise<void> {
    const { api } = await import('../api');
    await api.delete(`/broadcast/${broadcastId}`);
  }

  async getActiveBroadcasts(): Promise<LiveBroadcast[]> {
    const response = await broadcastApi.getActive();
    const data = response.data;
    return Array.isArray(data) ? data.map(mapBackendBroadcast) : [];
  }

  async getAllBroadcasts(): Promise<LiveBroadcast[]> {
    // Backend doesn't have GET /broadcast (all), use active for now
    return this.getActiveBroadcasts();
  }

  // ---- Chat (backend /api/broadcast/{id}/chat) ----

  async getChatMessages(broadcastId: string, limit = 50): Promise<LiveChatMessage[]> {
    try {
      const { api } = await import('../api');
      const response = await api.get(`/broadcast/${broadcastId}/chat`, {
        limit
      });
      const data = response.data;
      if (!Array.isArray(data)) return [];

      const currentUserId = getCurrentUser().id;
      return data.map((msg: BackendChatMessageDto) => ({
        id: msg.id || '',
        broadcastId: msg.broadcastId || broadcastId,
        userId: msg.userId || '',
        username: msg.username || 'User',
        userRole: msg.userRole || 'USER',
        content: msg.content || '',
        type: (msg.type || 'message') as LiveChatMessage['type'],
        timestamp: msg.timestamp || new Date().toISOString(),
        isOwn: msg.userId === currentUserId
      }));
    } catch {
      return [];
    }
  }

  async sendChatMessage(
    broadcastId: string,
    content: string,
    type: LiveChatMessage['type'] = 'message'
  ): Promise<LiveChatMessage> {
    const { api } = await import('../api');
    const response = await api.post(`/broadcast/${broadcastId}/chat`, {
      content,
      type
    });

    const msg = response.data;
    const user = getCurrentUser();
    return {
      id: msg.id || `chat-${Date.now()}`,
      broadcastId,
      userId: msg.userId || user.id,
      username: msg.username || user.username,
      userRole: msg.userRole || user.role,
      content: msg.content || content,
      type: (msg.type || type) as LiveChatMessage['type'],
      timestamp: msg.timestamp || new Date().toISOString(),
      isOwn: true
    };
  }

  // ---- Reactions (backend /api/broadcast/{id}/reaction) ----

  async sendReaction(broadcastId: string, emoji: string): Promise<void> {
    const { api } = await import('../api');
    await api.post(`/broadcast/${broadcastId}/reaction`, { emoji });
  }

  async getReactionSummary(broadcastId: string): Promise<{ counts: Record<string, number>; total: number }> {
    try {
      const { api } = await import('../api');
      const response = await api.get(`/broadcast/${broadcastId}/reaction`);
      return {
        counts: response.data.counts || {},
        total: response.data.total || 0
      };
    } catch {
      return { counts: {}, total: 0 };
    }
  }

  // ---- Stats ----

  async getViewerStats(broadcastId: string): Promise<ViewerStats[]> {
    // Backend doesn't have viewer stats history endpoint yet.
    // Return empty — real stats come via WebSocket.
    return [];
  }

  async getBroadcastStats(broadcastId: string): Promise<Record<string, unknown> | null> {
    try {
      const response = await broadcastApi.getStats(broadcastId);
      return response.data;
    } catch {
      return null;
    }
  }

  // ---- Integration ----

  async linkToEvent(broadcastId: string, eventId: string): Promise<LiveBroadcast> {
    const { api } = await import('../api');
    await api.post(`/broadcast/${broadcastId}/link/${eventId}`, {});
    return this.getBroadcast(broadcastId);
  }

  async unlinkFromEvent(broadcastId: string): Promise<LiveBroadcast> {
    const { api } = await import('../api');
    await api.delete(`/broadcast/${broadcastId}/link`);
    return this.getBroadcast(broadcastId);
  }

  async archiveBroadcast(broadcastId: string): Promise<{ mediaFileId: string }> {
    const { api } = await import('../api');
    const response = await api.get(`/broadcast/${broadcastId}/archive`);
    return { mediaFileId: response.data.mediaFileId || broadcastId };
  }

  // ---- Viewer simulation removed (real tracking via WebSocket) ----

  resetDemoData(): void {
    // No-op — data is in backend now
  }
}

export const realLiveApi = RealLiveApi.getInstance();
export type { RealLiveApi as LiveApiService };
