// Tests for hooks/live/useLiveBroadcast.ts
// Covers: useLiveBroadcast, useLiveBroadcasts, useBroadcastStats, useBroadcastAvailability

import { renderHook, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mocks ──────────────────────────────────────────────────────────────────────

const mockGetBroadcast = jest.fn();
const mockGetAllBroadcasts = jest.fn();
const mockGetActiveBroadcasts = jest.fn();
const mockStartBroadcast = jest.fn();
const mockStopBroadcast = jest.fn();
const mockUpdateBroadcast = jest.fn();
const mockGetBroadcastStats = jest.fn();
const mockGetViewerStats = jest.fn();

jest.mock('../services/live', () => ({
  liveApi: {
    getBroadcast: (...args: any[]) => mockGetBroadcast(...args),
    getAllBroadcasts: (...args: any[]) => mockGetAllBroadcasts(...args),
    getActiveBroadcasts: (...args: any[]) => mockGetActiveBroadcasts(...args),
    startBroadcast: (...args: any[]) => mockStartBroadcast(...args),
    stopBroadcast: (...args: any[]) => mockStopBroadcast(...args),
    updateBroadcast: (...args: any[]) => mockUpdateBroadcast(...args),
    getBroadcastStats: (...args: any[]) => mockGetBroadcastStats(...args),
    getViewerStats: (...args: any[]) => mockGetViewerStats(...args),
  },
  liveWebSocket: { subscribe: jest.fn(), disconnect: jest.fn() },
  mediaService: {},
}));

const mockConnectToBroadcast = jest.fn().mockResolvedValue(undefined);
const mockDisconnectFromBroadcast = jest.fn();
const mockUpdateBroadcastStatus = jest.fn().mockResolvedValue(undefined);

jest.mock('../context/live/LiveSessionContext', () => ({
  useLiveSession: jest.fn(() => ({
    state: {
      broadcast: null,
      wsConnected: false,
      wsReconnecting: false,
      isAdmin: false,
      isViewer: false,
      isModerator: false,
    },
    connectToBroadcast: mockConnectToBroadcast,
    disconnectFromBroadcast: mockDisconnectFromBroadcast,
    updateBroadcastStatus: mockUpdateBroadcastStatus,
  })),
}));

import {
  useLiveBroadcast,
  useLiveBroadcasts,
  useBroadcastStats,
  useBroadcastAvailability,
} from '../hooks/live/useLiveBroadcast';

// ── Helpers ────────────────────────────────────────────────────────────────────

const mockBroadcast = {
  id: 'bc-1',
  title: 'Test Broadcast',
  description: 'Test',
  streamKey: 'key-001',
  ingestUrl: 'rtmp://test/live',
  playbackUrl: 'http://test.m3u8',
  streamType: 'video' as const,
  status: 'live' as const,
  viewerCount: 10,
  maxViewers: 20,
  createdBy: 'user-001',
  createdAt: new Date().toISOString(),
};

// ── useLiveBroadcast ───────────────────────────────────────────────────────────

describe('useLiveBroadcast', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetBroadcast.mockResolvedValue(mockBroadcast);
  });

  it('returns initial state with null broadcast', () => {
    const { result } = renderHook(() => useLiveBroadcast());

    expect(result.current.broadcast).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('returns action functions', () => {
    const { result } = renderHook(() => useLiveBroadcast());

    expect(typeof result.current.loadBroadcast).toBe('function');
    expect(typeof result.current.connect).toBe('function');
    expect(typeof result.current.disconnect).toBe('function');
    expect(typeof result.current.updateStatus).toBe('function');
    expect(typeof result.current.startBroadcast).toBe('function');
    expect(typeof result.current.stopBroadcast).toBe('function');
    expect(typeof result.current.updateBroadcast).toBe('function');
    expect(typeof result.current.clearError).toBe('function');
  });

  it('auto-loads broadcast when broadcastId is provided', async () => {
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockGetBroadcast).toHaveBeenCalledWith('bc-1');
    expect(result.current.broadcast).toEqual(mockBroadcast);
  });

  it('sets error when loadBroadcast fails', async () => {
    mockGetBroadcast.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Network error');
  });

  it('sets generic error for non-Error exceptions', async () => {
    mockGetBroadcast.mockRejectedValue('string error');

    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.error).toBe('Ошибка загрузки трансляции');
    });
  });

  it('connects to broadcast via context', async () => {
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await act(async () => {
      await result.current.connect();
    });

    expect(mockConnectToBroadcast).toHaveBeenCalledWith('bc-1');
  });

  it('sets error when connect fails', async () => {
    mockConnectToBroadcast.mockRejectedValue(new Error('Connect failed'));

    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await act(async () => {
      await result.current.connect();
    });

    expect(result.current.error).toBe('Connect failed');
  });

  it('disconnects and clears broadcast', async () => {
    mockGetBroadcast.mockResolvedValue(mockBroadcast);
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.broadcast).toBeTruthy();
    });

    act(() => {
      result.current.disconnect();
    });

    expect(mockDisconnectFromBroadcast).toHaveBeenCalled();
    expect(result.current.broadcast).toBeNull();
  });

  it('updates broadcast status', async () => {
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.broadcast).toBeTruthy();
    });

    await act(async () => {
      await result.current.updateStatus('ended');
    });

    expect(mockUpdateBroadcastStatus).toHaveBeenCalledWith('ended');
    expect(result.current.broadcast?.status).toBe('ended');
  });

  it('starts a new broadcast', async () => {
    mockStartBroadcast.mockResolvedValue({
      broadcast: mockBroadcast,
      streamKey: 'key-001',
      ingestUrl: 'rtmp://test/live',
      playbackUrl: 'http://test.m3u8',
    });

    const { result } = renderHook(() => useLiveBroadcast());

    let returnedBroadcast: any;
    await act(async () => {
      returnedBroadcast = await result.current.startBroadcast('Test', 'Desc');
    });

    expect(mockStartBroadcast).toHaveBeenCalledWith({
      title: 'Test',
      description: 'Desc',
      streamType: 'video',
      settings: {
        isChatEnabled: true,
        isReactionsEnabled: true,
        isRecording: true,
        isPublic: true,
      },
    });
    expect(result.current.broadcast).toEqual(mockBroadcast);
    expect(returnedBroadcast).toEqual(mockBroadcast);
  });

  it('sets error when startBroadcast fails and re-throws', async () => {
    mockStartBroadcast.mockRejectedValue(new Error('Start failed'));

    const { result } = renderHook(() => useLiveBroadcast());

    await expect(
      act(async () => {
        await result.current.startBroadcast('Test');
      })
    ).rejects.toThrow('Start failed');

    expect(result.current.error).toBe('Start failed');
  });

  it('clears error with clearError', async () => {
    mockGetBroadcast.mockRejectedValue(new Error('err'));
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.error).toBeTruthy();
    });

    act(() => {
      result.current.clearError();
    });

    expect(result.current.error).toBeNull();
  });

  it('computes isLive / isEnded / isPreparing / canJoin', async () => {
    mockGetBroadcast.mockResolvedValue({ ...mockBroadcast, status: 'live' });
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));

    await waitFor(() => {
      expect(result.current.broadcast).toBeTruthy();
    });

    expect(result.current.isLive).toBe(true);
    expect(result.current.isEnded).toBe(false);
    expect(result.current.canJoin).toBe(true);
  });

  it('returns session state values', () => {
    const { result } = renderHook(() => useLiveBroadcast('bc-1'));
    expect(typeof result.current.isConnected).toBe('boolean');
    expect(typeof result.current.isReconnecting).toBe('boolean');
    expect(typeof result.current.isAdmin).toBe('boolean');
    expect(typeof result.current.isViewer).toBe('boolean');
    expect(typeof result.current.isModerator).toBe('boolean');
  });
});

// ── useLiveBroadcasts ──────────────────────────────────────────────────────────

describe('useLiveBroadcasts', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetAllBroadcasts.mockResolvedValue([mockBroadcast]);
  });

  it('auto-loads broadcasts on mount', async () => {
    const { result } = renderHook(() => useLiveBroadcasts());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockGetAllBroadcasts).toHaveBeenCalled();
    expect(result.current.broadcasts).toHaveLength(1);
  });

  it('returns action functions', () => {
    const { result } = renderHook(() => useLiveBroadcasts());

    expect(typeof result.current.loadBroadcasts).toBe('function');
    expect(typeof result.current.loadActiveBroadcasts).toBe('function');
    expect(typeof result.current.updateBroadcastInList).toBe('function');
    expect(typeof result.current.removeBroadcastFromList).toBe('function');
    expect(typeof result.current.clearError).toBe('function');
  });

  it('filters by status', async () => {
    const broadcasts = [
      { ...mockBroadcast, id: 'bc-1', status: 'live' },
      { ...mockBroadcast, id: 'bc-2', status: 'ended' },
    ];
    mockGetAllBroadcasts.mockResolvedValue(broadcasts);

    const { result } = renderHook(() =>
      useLiveBroadcasts({ status: 'live' })
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.broadcasts).toHaveLength(1);
    expect(result.current.broadcasts[0].id).toBe('bc-1');
  });

  it('filters by createdBy', async () => {
    const broadcasts = [
      { ...mockBroadcast, id: 'bc-1', createdBy: 'user-1' },
      { ...mockBroadcast, id: 'bc-2', createdBy: 'user-2' },
    ];
    mockGetAllBroadcasts.mockResolvedValue(broadcasts);

    const { result } = renderHook(() =>
      useLiveBroadcasts({ createdBy: 'user-1' })
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.broadcasts).toHaveLength(1);
    expect(result.current.broadcasts[0].createdBy).toBe('user-1');
  });

  it('filters by limit', async () => {
    const broadcasts = [
      { ...mockBroadcast, id: 'bc-1' },
      { ...mockBroadcast, id: 'bc-2' },
      { ...mockBroadcast, id: 'bc-3' },
    ];
    mockGetAllBroadcasts.mockResolvedValue(broadcasts);

    const { result } = renderHook(() => useLiveBroadcasts({ limit: 2 }));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.broadcasts).toHaveLength(2);
  });

  it('sets error when loadBroadcasts fails', async () => {
    mockGetAllBroadcasts.mockRejectedValue(new Error('fail'));

    const { result } = renderHook(() => useLiveBroadcasts());

    await waitFor(() => {
      expect(result.current.error).toBe('fail');
    });
  });

  it('loadActiveBroadcasts loads active broadcasts', async () => {
    mockGetActiveBroadcasts.mockResolvedValue([mockBroadcast]);

    const { result } = renderHook(() => useLiveBroadcasts());

    await act(async () => {
      await result.current.loadActiveBroadcasts();
    });

    expect(mockGetActiveBroadcasts).toHaveBeenCalled();
    expect(result.current.broadcasts).toHaveLength(1);
  });

  it('updateBroadcastInList updates an item', async () => {
    mockGetAllBroadcasts.mockResolvedValue([mockBroadcast]);
    const { result } = renderHook(() => useLiveBroadcasts());

    await waitFor(() => {
      expect(result.current.broadcasts).toHaveLength(1);
    });

    act(() => {
      result.current.updateBroadcastInList({
        ...mockBroadcast,
        title: 'Updated',
      });
    });

    expect(result.current.broadcasts[0].title).toBe('Updated');
  });

  it('removeBroadcastFromList removes an item', async () => {
    mockGetAllBroadcasts.mockResolvedValue([
      { ...mockBroadcast, id: 'bc-1' },
      { ...mockBroadcast, id: 'bc-2' },
    ]);

    const { result } = renderHook(() => useLiveBroadcasts());

    await waitFor(() => {
      expect(result.current.broadcasts).toHaveLength(2);
    });

    act(() => {
      result.current.removeBroadcastFromList('bc-1');
    });

    expect(result.current.broadcasts).toHaveLength(1);
    expect(result.current.broadcasts[0].id).toBe('bc-2');
  });
});

// ── useBroadcastStats ──────────────────────────────────────────────────────────

describe('useBroadcastStats', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetBroadcastStats.mockResolvedValue({ viewerCount: 50, totalViews: 200 });
  });

  it('returns initial null stats', () => {
    const { result } = renderHook(() => useBroadcastStats());
    expect(result.current.stats).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it('auto-loads stats when broadcastId is provided', async () => {
    const { result } = renderHook(() => useBroadcastStats('bc-1'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockGetBroadcastStats).toHaveBeenCalledWith('bc-1');
    expect(result.current.stats).toEqual({ viewerCount: 50, totalViews: 200 });
  });

  it('sets error when stats loading fails', async () => {
    mockGetBroadcastStats.mockRejectedValue(new Error('Stats error'));

    const { result } = renderHook(() => useBroadcastStats('bc-1'));

    await waitFor(() => {
      expect(result.current.error).toBe('Stats error');
    });
  });

  it('loadViewerStats returns data', async () => {
    mockGetViewerStats.mockResolvedValue([{ viewerCount: 10 }]);

    const { result } = renderHook(() => useBroadcastStats('bc-1'));

    const data = await result.current.loadViewerStats('bc-1');
    expect(data).toEqual([{ viewerCount: 10 }]);
  });

  it('loadViewerStats returns empty on error', async () => {
    mockGetViewerStats.mockRejectedValue(new Error('fail'));

    const { result } = renderHook(() => useBroadcastStats('bc-1'));

    const data = await result.current.loadViewerStats('bc-1');
    expect(data).toEqual([]);
  });
});

// ── useBroadcastAvailability ───────────────────────────────────────────────────

describe('useBroadcastAvailability', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns initial state', () => {
    const { result } = renderHook(() => useBroadcastAvailability());
    expect(result.current.available).toBe(false);
    expect(result.current.checking).toBe(false);
  });

  it('returns checkAvailability function', () => {
    const { result } = renderHook(() => useBroadcastAvailability());
    expect(typeof result.current.checkAvailability).toBe('function');
  });

  it('marks available when broadcast exists', async () => {
    mockGetBroadcast.mockResolvedValue(mockBroadcast);

    const { result } = renderHook(() => useBroadcastAvailability('bc-1'));

    await waitFor(() => {
      expect(result.current.checking).toBe(false);
    });

    expect(result.current.available).toBe(true);
  });

  it('marks unavailable when broadcast does not exist', async () => {
    mockGetBroadcast.mockRejectedValue(new Error('Not found'));

    const { result } = renderHook(() => useBroadcastAvailability('bc-1'));

    await waitFor(() => {
      expect(result.current.checking).toBe(false);
    });

    expect(result.current.available).toBe(false);
  });
});
