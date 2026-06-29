import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mock all dependencies ──────────────────────────────────────────────────────
jest.mock('../services/live', () => ({
  mediaService: {
    getStream: jest.fn().mockResolvedValue({ url: 'http://test.m3u8', quality: 'auto', bitrate: 0, codec: 'h264' }),
    getAvailableQualities: jest.fn().mockReturnValue(['1080p', '720p', '480p', '360p', 'auto']),
    getPlayerStats: jest.fn().mockReturnValue({ bitrate: 2500, bufferLength: 10, droppedFrames: 0, latency: 1.5, networkActivity: false }),
  },
  liveWebSocket: { subscribe: jest.fn(), disconnect: jest.fn() },
  liveApi: {},
}));

jest.mock('../hooks/live/useHlsPlayer', () => ({
  useHlsPlayer: jest.fn(),
}));

jest.mock('../context/live/LiveSessionContext', () => ({
  usePlayerState: jest.fn(),
}));

jest.mock('../utils/live/helpers', () => ({
  formatDuration: (s: number) => `${Math.floor(s)}s`,
  formatBitrate: (b: number) => `${b} kbps`,
  getResolutionForQuality: (q: string) => {
    if (q === '1080p') return { width: 1920, height: 1080 };
    if (q === '720p') return { width: 1280, height: 720 };
    return { width: 640, height: 360 };
  },
}));

// ── Imports (after mocks) ─────────────────────────────────────────────────────
import { LivePlayer } from '../components/live/LivePlayer';
import { useHlsPlayer } from '../hooks/live/useHlsPlayer';
import { usePlayerState } from '../context/live/LiveSessionContext';

// ── Mock data ─────────────────────────────────────────────────────────────────
const mockBroadcast = {
  id: 'broadcast-001',
  title: 'Тестовая трансляция',
  description: 'Описание тестовой трансляции',
  streamKey: 'key-001',
  ingestUrl: 'rtmp://test/live',
  playbackUrl: 'http://test.m3u8',
  streamType: 'video' as const,
  status: 'live' as const,
  viewerCount: 100,
  maxViewers: 200,
  startedAt: new Date().toISOString(),
  createdBy: 'user-001',
  createdAt: new Date().toISOString(),
};

const defaultHlsReturn = {
  state: {
    currentQuality: 'auto' as const,
    availableQualities: ['1080p', '720p', '480p', '360p', 'auto'],
    isLoading: false,
    error: null,
    stats: { bitrate: 2500, bufferLength: 10, droppedFrames: 0, latency: 1.5, networkActivity: false },
  },
  videoRef: { current: document.createElement('video') },
  containerRef: { current: document.createElement('div') },
  switchQuality: jest.fn(),
  autoSwitchQuality: jest.fn(),
  hlsRef: { current: null },
};

const defaultPlayerReturn = {
  playerState: {
    isPlaying: false,
    currentTime: 0,
    volume: 1.0,
    quality: 'auto' as const,
    isFullscreen: false,
    isMuted: false,
  },
  setPlayerState: jest.fn(),
};

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('LivePlayer', () => {
  beforeEach(() => {
    // mockClear preserves mockReturnValue, clearAllMocks kills it
    (useHlsPlayer as jest.Mock).mockClear();
    (usePlayerState as jest.Mock).mockClear();
    (useHlsPlayer as jest.Mock).mockReturnValue(defaultHlsReturn);
    (usePlayerState as jest.Mock).mockReturnValue(defaultPlayerReturn);
  });

  it('рендерит заголовок трансляции', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('Тестовая трансляция')).toBeInTheDocument();
  });

  it('рендерит описание если есть', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('Описание тестовой трансляции')).toBeInTheDocument();
  });

  it('не рендерит описание если нет', () => {
    const broadcast = { ...mockBroadcast, description: undefined };
    render(<LivePlayer broadcast={broadcast} />);
    expect(screen.queryByText('Описание тестовой трансляции')).not.toBeInTheDocument();
  });

  it('показывает ошибку если broadcast = null', () => {
    // @ts-expect-error — testing null broadcast
    render(<LivePlayer broadcast={null} />);
    expect(screen.getByText('Трансляция не найдена')).toBeInTheDocument();
  });

  it('показывает loading spinner', () => {
    (useHlsPlayer as jest.Mock).mockReturnValue({
      ...defaultHlsReturn,
      state: { ...defaultHlsReturn.state, isLoading: true },
    });

    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('Загрузка трансляции...')).toBeInTheDocument();
  });

  it('скрывает loading spinner когда не загружается', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.queryByText('Загрузка трансляции...')).not.toBeInTheDocument();
  });

  it('показывает ошибку HLS', () => {
    (useHlsPlayer as jest.Mock).mockReturnValue({
      ...defaultHlsReturn,
      state: { ...defaultHlsReturn.state, error: 'Ошибка воспроизведения: NETWORK_ERROR' },
    });

    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('Ошибка воспроизведения: NETWORK_ERROR')).toBeInTheDocument();
  });

  it('скрывает ошибку если нет error state', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.queryByText(/Ошибка/)).not.toBeInTheDocument();
  });

  it('показывает кнопки качества', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('Авто')).toBeInTheDocument();
    expect(screen.getByText('1080p')).toBeInTheDocument();
    expect(screen.getByText('720p')).toBeInTheDocument();
  });

  it('скрывает quality controls если < 2 качеств', () => {
    (useHlsPlayer as jest.Mock).mockReturnValue({
      ...defaultHlsReturn,
      state: { ...defaultHlsReturn.state, availableQualities: ['auto'] },
    });

    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.queryByText('Качество:')).not.toBeInTheDocument();
  });

  it('вызывает switchQuality при клике на кнопку качества', () => {
    const mockSwitchQuality = jest.fn();
    (useHlsPlayer as jest.Mock).mockReturnValue({
      ...defaultHlsReturn,
      switchQuality: mockSwitchQuality,
    });

    render(<LivePlayer broadcast={mockBroadcast} />);
    fireEvent.click(screen.getByText('720p'));
    expect(mockSwitchQuality).toHaveBeenCalledWith('720p');
  });

  it('показывает количество зрителей', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('👁️ 100')).toBeInTheDocument();
    expect(screen.getByText('📊 200')).toBeInTheDocument();
  });

  it('показывает play/pause кнопку', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('▶️')).toBeInTheDocument();
  });

  it('показывает mute/unmute кнопку', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('🔊')).toBeInTheDocument();
  });

  it('показывает muted иконку когда muted', () => {
    (usePlayerState as jest.Mock).mockReturnValue({
      ...defaultPlayerReturn,
      playerState: { ...defaultPlayerReturn.playerState, isMuted: true },
    });

    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('🔇')).toBeInTheDocument();
  });

  it('кнопка play содержит ▶️', () => {
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('▶️')).toBeInTheDocument();
  });

  it('кнопка pause содержит ⏸️ когда isPlaying', () => {
    (usePlayerState as jest.Mock).mockReturnValue({
      ...defaultPlayerReturn,
      playerState: { ...defaultPlayerReturn.playerState, isPlaying: true },
    });
    render(<LivePlayer broadcast={mockBroadcast} />);
    expect(screen.getByText('⏸️')).toBeInTheDocument();
  });

  it('вызывает onError', () => {
    const onError = jest.fn();
    render(<LivePlayer broadcast={mockBroadcast} onError={onError} />);
    // onError is passed to useHlsPlayer, which calls it on HLS errors
    expect(useHlsPlayer).toHaveBeenCalledWith(
      expect.objectContaining({ onError })
    );
  });

  it('вызывает onReady', () => {
    const onReady = jest.fn();
    render(<LivePlayer broadcast={mockBroadcast} onReady={onReady} />);
    expect(useHlsPlayer).toHaveBeenCalledWith(
      expect.objectContaining({ onReady })
    );
  });

  it('передаёт autoPlay и muted в useHlsPlayer', () => {
    render(<LivePlayer broadcast={mockBroadcast} autoPlay={false} muted={true} />);
    expect(useHlsPlayer).toHaveBeenCalledWith(
      expect.objectContaining({ autoPlay: false, muted: true })
    );
  });
});
