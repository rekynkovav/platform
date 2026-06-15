/**
 * Real Live API service — calls actual backend endpoints.
 * Replaces mockApi.ts for production use.
 *
 * Broadcast CRUD → backend /api/broadcast/*
 * Chat → local state (backend broadcast-chat endpoint: TODO)
 * Viewer tracking → backend BroadcastStatsWebSocketHandler
 */

import {
  LiveBroadcast,
  StartBroadcastRequest,
  StartBroadcastResponse,
  UpdateBroadcastRequest,
  LiveChatMessage,
  ViewerStats,
  BroadcastStatus
} from './types';
import { broadcastApi } from '../api';

// Broadcast chat messages stored locally (no backend endpoint yet)
const chatStore = new Map<string, LiveChatMessage[]>();

// User context from Keycloak JWT
function getCurrentUser(): { id: string; username: string; role: string } {
  try {
    const token = localStorage.getItem('keycloak-token');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        id: payload.sub || 'unknown',
        username: payload.preferred_username || payload.name || 'User',
        role: (payload.realm_access?.roles || []).find(
          (r: string) => r.startsWith('SUPER_ADMIN') || r.startsWith('MODERATOR') || r.startsWith('VYACHESLAV')
        ) || 'USER_1'
      };
    }
  } catch {}
  return { id: 'anonymous', username: 'Гость', role: 'USER_1' };
}

/**
 * Map backend LiveBroadcastDto → frontend LiveBroadcast type.
 * Backend fields don't perfectly match, so we normalize.
 */
function mapBackendBroadcast(raw: any): LiveBroadcast {
  return {
    id: raw.id || raw.broadcastId || '',
    title: raw.title || '',
    description: raw.description || '',
    streamKey: raw.streamKey || raw.rtmpStreamKey || raw.id || '',
    ingestUrl: raw.ingestUrl || '',
    playbackUrl: raw.playbackUrl || raw.hlsPlaylistUrl || '',
    streamType: (raw.streamType as any) || 'video',
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
    updatedAt: raw.updatedAt || undefined
  };
}

function normalizeStatus(status: any): BroadcastStatus {
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

  // ---- Chat (local state, no backend broadcast-chat endpoint) ----

  async getChatMessages(broadcastId: string, limit = 50): Promise<LiveChatMessage[]> {
    const messages = chatStore.get(broadcastId) || [];
    return messages.slice(-limit);
  }

  async sendChatMessage(
    broadcastId: string,
    content: string,
    type: LiveChatMessage['type'] = 'message'
  ): Promise<LiveChatMessage> {
    const user = getCurrentUser();
    const message: LiveChatMessage = {
      id: `chat-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
      broadcastId,
      userId: user.id,
      username: user.username,
      userRole: user.role,
      content,
      type,
      timestamp: new Date().toISOString(),
      isOwn: true
    };

    const messages = chatStore.get(broadcastId) || [];
    messages.push(message);
    chatStore.set(broadcastId, messages);

    return message;
  }

  async sendReaction(broadcastId: string, emoji: string): Promise<void> {
    // TODO: backend broadcast reaction endpoint
    console.log('[RealLiveApi] Reaction:', emoji, 'broadcast:', broadcastId);
  }

  // ---- Stats ----

  async getViewerStats(broadcastId: string): Promise<ViewerStats[]> {
    // Backend doesn't have viewer stats history endpoint yet.
    // Return empty — real stats come via WebSocket.
    return [];
  }

  async getBroadcastStats(broadcastId: string): Promise<any> {
    try {
      const response = await broadcastApi.getStats(broadcastId);
      return response.data;
    } catch {
      return null;
    }
  }

  // ---- Integration ----

  async linkToEvent(broadcastId: string, eventId: string): Promise<LiveBroadcast> {
    // TODO: backend endpoint for linking
    return this.getBroadcast(broadcastId);
  }

  async archiveBroadcast(broadcastId: string): Promise<{ mediaFileId: string }> {
    const { api } = await import('../api');
    const response = await api.get(`/broadcast/${broadcastId}/archive`);
    return { mediaFileId: response.data.mediaFileId || broadcastId };
  }

  // ---- Viewer simulation removed (real tracking via WebSocket) ----

  resetDemoData(): void {
    chatStore.clear();
  }
}

export const realLiveApi = RealLiveApi.getInstance();
export type { RealLiveApi as LiveApiService };
