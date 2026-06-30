// Tests for services/live/realApi.ts
// Covers: RealLiveApi singleton methods

import '@testing-library/jest-dom';

// ── Mocks ──────────────────────────────────────────────────────────────────────

const mockBroadcastStart = jest.fn();
const mockBroadcastGetById = jest.fn();
const mockBroadcastEnd = jest.fn();
const mockBroadcastGetActive = jest.fn();
const mockBroadcastGetStats = jest.fn();
const mockApiGet = jest.fn();
const mockApiPost = jest.fn();
const mockApiDelete = jest.fn();

jest.mock('../services/api', () => ({
  api: {
    get: (...args: any[]) => mockApiGet(...args),
    post: (...args: any[]) => mockApiPost(...args),
    delete: (...args: any[]) => mockApiDelete(...args),
  },
  broadcastApi: {
    start: (...args: any[]) => mockBroadcastStart(...args),
    getById: (...args: any[]) => mockBroadcastGetById(...args),
    end: (...args: any[]) => mockBroadcastEnd(...args),
    getActive: (...args: any[]) => mockBroadcastGetActive(...args),
    getStats: (...args: any[]) => mockBroadcastGetStats(...args),
  },
}));

jest.mock('../keycloak', () => ({
  getToken: jest.fn().mockReturnValue('mock-token'),
  getUserId: jest.fn().mockReturnValue('user-123'),
  getUsername: jest.fn().mockReturnValue('testuser'),
  getUserRoles: jest.fn().mockReturnValue(['USER_1']),
}));

// ── Import after mocks ─────────────────────────────────────────────────────────

import { realLiveApi, RealLiveApi } from '../services/live/realApi';

// ── Tests ──────────────────────────────────────────────────────────────────────

describe('RealLiveApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('is a singleton', () => {
    const instance1 = RealLiveApi.getInstance();
    const instance2 = RealLiveApi.getInstance();
    expect(instance1).toBe(instance2);
  });

  it('exported instance is the singleton', () => {
    expect(realLiveApi).toBe(RealLiveApi.getInstance());
  });

  // ── startBroadcast ────────────────────────────────────────────────────────

  describe('startBroadcast', () => {
    it('creates a broadcast and returns formatted response', async () => {
      mockBroadcastStart.mockResolvedValue({
        data: {
          streamKey: 'key-123',
          streamUrl: 'rtmp://ingest.test/live',
        },
      });

      const result = await realLiveApi.startBroadcast({
        title: 'My Stream',
        description: 'Test stream',
        streamType: 'video',
        settings: {
          isChatEnabled: true,
          isReactionsEnabled: true,
          isRecording: true,
          isPublic: true,
        },
      });

      expect(mockBroadcastStart).toHaveBeenCalledWith({
        title: 'My Stream',
        description: 'Test stream',
        streamType: 'video',
      });

      expect(result.broadcast).toBeDefined();
      expect(result.broadcast.id).toBe('key-123');
      expect(result.broadcast.title).toBe('My Stream');
      expect(result.broadcast.status).toBe('live');
      expect(result.broadcast.playbackUrl).toContain('key-123');
      expect(result.streamKey).toBe('key-123');
      expect(result.ingestUrl).toBe('rtmp://ingest.test/live');
    });
  });

  // ── getBroadcast ──────────────────────────────────────────────────────────

  describe('getBroadcast', () => {
    it('fetches and maps broadcast data', async () => {
      mockBroadcastGetById.mockResolvedValue({
        data: {
          id: 'bc-1',
          title: 'Stream 1',
          status: 'LIVE',
          viewerCount: 42,
          maxViewers: 100,
          ownerId: 'user-1',
          isChatEnabled: true,
          isReactionsEnabled: true,
          isRecording: false,
          isPublic: true,
        },
      });

      const result = await realLiveApi.getBroadcast('bc-1');

      expect(mockBroadcastGetById).toHaveBeenCalledWith('bc-1');
      expect(result.id).toBe('bc-1');
      expect(result.title).toBe('Stream 1');
      expect(result.status).toBe('live');
      expect(result.viewerCount).toBe(42);
    });
  });

  // ── updateBroadcast ───────────────────────────────────────────────────────

  describe('updateBroadcast', () => {
    it('calls end for ended status', async () => {
      mockBroadcastEnd.mockResolvedValue({ data: {} });
      mockBroadcastGetById.mockResolvedValue({
        data: { id: 'bc-1', status: 'ENDED' },
      });

      await realLiveApi.updateBroadcast('bc-1', { status: 'ended' });

      expect(mockBroadcastEnd).toHaveBeenCalledWith('bc-1');
    });

    it('re-fetches broadcast after update', async () => {
      mockBroadcastGetById.mockResolvedValue({
        data: { id: 'bc-1', status: 'IDLE' },
      });

      await realLiveApi.updateBroadcast('bc-1', { title: 'New Title' });

      expect(mockBroadcastGetById).toHaveBeenCalledWith('bc-1');
    });
  });

  // ── stopBroadcast ─────────────────────────────────────────────────────────

  describe('stopBroadcast', () => {
    it('calls end and re-fetches', async () => {
      mockBroadcastEnd.mockResolvedValue({ data: {} });
      mockBroadcastGetById.mockResolvedValue({
        data: { id: 'bc-1', status: 'ENDED' },
      });

      const result = await realLiveApi.stopBroadcast('bc-1');

      expect(mockBroadcastEnd).toHaveBeenCalledWith('bc-1');
      expect(result.status).toBe('ended');
    });
  });

  // ── getActiveBroadcasts ───────────────────────────────────────────────────

  describe('getActiveBroadcasts', () => {
    it('returns mapped active broadcasts', async () => {
      mockBroadcastGetActive.mockResolvedValue({
        data: [
          { id: 'bc-1', title: 'Stream 1', status: 'LIVE' },
          { id: 'bc-2', title: 'Stream 2', status: 'PREPARING' },
        ],
      });

      const result = await realLiveApi.getActiveBroadcasts();

      expect(result).toHaveLength(2);
      expect(result[0].id).toBe('bc-1');
      expect(result[0].status).toBe('live');
      expect(result[1].status).toBe('preparing');
    });

    it('returns empty array for non-array response', async () => {
      mockBroadcastGetActive.mockResolvedValue({ data: null });

      const result = await realLiveApi.getActiveBroadcasts();

      expect(result).toEqual([]);
    });
  });

  // ── getAllBroadcasts ───────────────────────────────────────────────────────

  describe('getAllBroadcasts', () => {
    it('delegates to getActiveBroadcasts', async () => {
      mockBroadcastGetActive.mockResolvedValue({ data: [] });

      const result = await realLiveApi.getAllBroadcasts();

      expect(mockBroadcastGetActive).toHaveBeenCalled();
      expect(result).toEqual([]);
    });
  });

  // ── getChatMessages ───────────────────────────────────────────────────────

  describe('getChatMessages', () => {
    it('fetches and maps chat messages', async () => {
      mockApiGet.mockResolvedValue({
        data: [
          {
            id: 'msg-1',
            broadcastId: 'bc-1',
            userId: 'user-123',
            username: 'Alice',
            userRole: 'USER',
            content: 'Hello!',
            type: 'message',
            timestamp: '2026-01-01T00:00:00Z',
          },
        ],
      });

      const result = await realLiveApi.getChatMessages('bc-1');

      expect(mockApiGet).toHaveBeenCalledWith('/broadcast/bc-1/chat', { limit: 50 });
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('msg-1');
      expect(result[0].content).toBe('Hello!');
    });

    it('returns empty array on error', async () => {
      mockApiGet.mockRejectedValue(new Error('fail'));

      const result = await realLiveApi.getChatMessages('bc-1');
      expect(result).toEqual([]);
    });

    it('returns empty array for non-array response', async () => {
      mockApiGet.mockResolvedValue({ data: 'invalid' });

      const result = await realLiveApi.getChatMessages('bc-1');
      expect(result).toEqual([]);
    });
  });

  // ── sendChatMessage ───────────────────────────────────────────────────────

  describe('sendChatMessage', () => {
    it('sends message and returns formatted response', async () => {
      mockApiPost.mockResolvedValue({
        data: {
          id: 'msg-new',
          userId: 'user-123',
          username: 'testuser',
          content: 'Hi!',
          type: 'message',
          timestamp: '2026-01-01T00:00:00Z',
        },
      });

      const result = await realLiveApi.sendChatMessage('bc-1', 'Hi!', 'message');

      expect(mockApiPost).toHaveBeenCalledWith('/broadcast/bc-1/chat', {
        content: 'Hi!',
        type: 'message',
      });
      expect(result.id).toBe('msg-new');
      expect(result.isOwn).toBe(true);
      expect(result.content).toBe('Hi!');
    });
  });

  // ── sendReaction ──────────────────────────────────────────────────────────

  describe('sendReaction', () => {
    it('posts reaction to backend', async () => {
      mockApiPost.mockResolvedValue({ data: {} });

      await realLiveApi.sendReaction('bc-1', '👍');

      expect(mockApiPost).toHaveBeenCalledWith('/broadcast/bc-1/reaction', { emoji: '👍' });
    });
  });

  // ── getReactionSummary ────────────────────────────────────────────────────

  describe('getReactionSummary', () => {
    it('returns reaction counts and total', async () => {
      mockApiGet.mockResolvedValue({
        data: { counts: { '👍': 5, '❤️': 3 }, total: 8 },
      });

      const result = await realLiveApi.getReactionSummary('bc-1');

      expect(result.counts['👍']).toBe(5);
      expect(result.total).toBe(8);
    });

    it('returns defaults on error', async () => {
      mockApiGet.mockRejectedValue(new Error('fail'));

      const result = await realLiveApi.getReactionSummary('bc-1');
      expect(result).toEqual({ counts: {}, total: 0 });
    });
  });

  // ── getViewerStats ────────────────────────────────────────────────────────

  describe('getViewerStats', () => {
    it('returns empty array (not yet implemented)', async () => {
      const result = await realLiveApi.getViewerStats('bc-1');
      expect(result).toEqual([]);
    });
  });

  // ── getBroadcastStats ─────────────────────────────────────────────────────

  describe('getBroadcastStats', () => {
    it('returns stats data', async () => {
      mockBroadcastGetStats.mockResolvedValue({
        data: { viewerCount: 42, totalViews: 1000 },
      });

      const result = await realLiveApi.getBroadcastStats('bc-1');
      expect(result).toEqual({ viewerCount: 42, totalViews: 1000 });
    });

    it('returns null on error', async () => {
      mockBroadcastGetStats.mockRejectedValue(new Error('fail'));

      const result = await realLiveApi.getBroadcastStats('bc-1');
      expect(result).toBeNull();
    });
  });

  // ── linkToEvent / unlinkFromEvent ─────────────────────────────────────────

  describe('linkToEvent', () => {
    it('links broadcast to event', async () => {
      mockApiPost.mockResolvedValue({ data: {} });
      mockBroadcastGetById.mockResolvedValue({
        data: { id: 'bc-1', linkedEventId: 'evt-1' },
      });

      const result = await realLiveApi.linkToEvent('bc-1', 'evt-1');

      expect(mockApiPost).toHaveBeenCalledWith('/broadcast/bc-1/link/evt-1', {});
      expect(result).toBeDefined();
    });
  });

  describe('unlinkFromEvent', () => {
    it('unlinks broadcast from event', async () => {
      mockApiDelete.mockResolvedValue({ data: {} });
      mockBroadcastGetById.mockResolvedValue({
        data: { id: 'bc-1', linkedEventId: undefined },
      });

      const result = await realLiveApi.unlinkFromEvent('bc-1');

      expect(mockApiDelete).toHaveBeenCalledWith('/broadcast/bc-1/link');
      expect(result).toBeDefined();
    });
  });

  // ── archiveBroadcast ──────────────────────────────────────────────────────

  describe('archiveBroadcast', () => {
    it('archives broadcast and returns media file id', async () => {
      mockApiGet.mockResolvedValue({
        data: { mediaFileId: 'media-123' },
      });

      const result = await realLiveApi.archiveBroadcast('bc-1');

      expect(mockApiGet).toHaveBeenCalledWith('/broadcast/bc-1/archive');
      expect(result.mediaFileId).toBe('media-123');
    });
  });

  // ── normalizeStatus (private, tested via getBroadcast) ────────────────────

  describe('normalizeStatus', () => {
    it('normalizes various status strings', async () => {
      const testCases = [
        { input: 'LIVE', expected: 'live' },
        { input: 'live', expected: 'live' },
        { input: 'ENDED', expected: 'ended' },
        { input: 'IDLE', expected: 'idle' },
        { input: 'PREPARING', expected: 'preparing' },
        { input: 'PAUSED', expected: 'paused' },
        { input: 'UNKNOWN', expected: 'idle' },
        { input: '', expected: 'idle' },
      ];

      for (const tc of testCases) {
        mockBroadcastGetById.mockResolvedValue({
          data: { id: 'bc-1', status: tc.input },
        });

        const result = await realLiveApi.getBroadcast('bc-1');
        expect(result.status).toBe(tc.expected);
      }
    });
  });

  // ── resetDemoData ─────────────────────────────────────────────────────────

  describe('resetDemoData', () => {
    it('is a no-op', () => {
      expect(() => realLiveApi.resetDemoData()).not.toThrow();
    });
  });
});
