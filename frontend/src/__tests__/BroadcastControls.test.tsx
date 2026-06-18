import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mock all dependencies ──────────────────────────────────────────────────────

jest.mock('../services/live', () => ({
  mediaService: {
    switchQuality: () => {},
    getPlayerStats: () => ({
      bitrate: 2500,
      bufferLength: 5.2,
      droppedFrames: 3,
      latency: 1.1,
      networkActivity: false,
    }),
    simulateNetworkIssues: () => {},
    restoreNetwork: () => {},
    reset: () => {},
  },
}));

jest.mock('../hooks/live/useLiveBroadcast', () => ({
  useLiveBroadcast: jest.fn(),  // bare jest.fn() — set in beforeEach
}));

jest.mock('../context/live/LiveSessionContext', () => ({
  useLiveSession: jest.fn(),  // bare jest.fn() — set in beforeEach
}));

jest.mock('../utils/live/helpers', () => ({
  formatDuration: (s: number) => `${Math.floor(s)}s`,
  getStatusColor: () => '#4CAF50',
  getStatusText: () => 'В эфире',
  canControlBroadcast: jest.fn(),  // bare jest.fn() — set in beforeEach
  getResolutionForQuality: () => ({ width: 1280, height: 720 }),
  formatBitrate: (b: number) => `${b} kbps`,
}));

// ── Imports (after mocks) ─────────────────────────────────────────────────────

import { BroadcastControls } from '../components/live/BroadcastControls';
import { useLiveBroadcast } from '../hooks/live/useLiveBroadcast';
import { useLiveSession } from '../context/live/LiveSessionContext';
import { canControlBroadcast } from '../utils/live/helpers';

// ── Shared mock data ──────────────────────────────────────────────────────────

const mockBroadcast = {
  id: 'bc-1',
  title: 'Тестовая трансляция',
  description: 'Описание тестовой трансляции',
  status: 'live' as const,
  streamType: 'video' as const,
  streamKey: 'key-123',
  ingestUrl: 'rtmp://example.com/live',
  playbackUrl: 'https://example.com/stream.m3u8',
  viewerCount: 42,
  maxViewers: 100,
  createdBy: 'user-1',
  createdAt: '2026-01-15T12:00:00Z',
  settings: {
    isPublic: true,
    isChatEnabled: true,
    isReactionsEnabled: true,
    isRecording: false,
  },
};

const mockUseLiveBroadcastReturn = {
  broadcast: mockBroadcast,
  loading: false,
  error: null,
  isConnected: true,
  isReconnecting: false,
  isAdmin: true,
  isViewer: false,
  isModerator: false,
  loadBroadcast: jest.fn(),
  connect: jest.fn(),
  disconnect: jest.fn(),
  updateStatus: jest.fn().mockResolvedValue(undefined),
  startBroadcast: jest.fn().mockResolvedValue(mockBroadcast),
  stopBroadcast: jest.fn().mockResolvedValue(undefined),
  updateBroadcast: jest.fn().mockResolvedValue(mockBroadcast),
  isLive: true,
  isEnded: false,
  isPreparing: false,
  canJoin: true,
  canControl: true,
  clearError: jest.fn(),
};

const mockLiveSession = {
  state: {
    broadcast: mockBroadcast,
    isAdmin: true,
    isViewer: false,
    isModerator: true,
    wsConnected: true,
    wsReconnecting: false,
    chatMessages: [],
    unreadCount: 0,
    isChatPaused: false,
    playerState: {
      isPlaying: true,
      currentTime: 120,
      volume: 1.0,
      quality: 'auto',
      isFullscreen: false,
      isMuted: false,
    },
    reactions: {},
    polls: [],
    questions: [],
  },
  dispatch: jest.fn(),
  connectToBroadcast: jest.fn(),
  disconnectFromBroadcast: jest.fn(),
  sendChatMessage: jest.fn(),
  sendReaction: jest.fn(),
  startBroadcast: jest.fn(),
  stopBroadcast: jest.fn(),
  updateBroadcastStatus: jest.fn(),
  toggleChatPause: jest.fn(),
  markChatAsRead: jest.fn(),
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('BroadcastControls', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useLiveBroadcast as jest.Mock).mockReturnValue(mockUseLiveBroadcastReturn);
    (useLiveSession as jest.Mock).mockReturnValue(mockLiveSession);
    (canControlBroadcast as jest.Mock).mockReturnValue(true);
  });

  // ── 1. Renders without crashing ─────────────────────────────────────────────

  it('renders without crashing', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);
    expect(screen.getByText('Управление трансляцией')).toBeInTheDocument();
  });

  // ── 2. Shows access denied when user has no control rights ──────────────────

  it('shows access denied when user has no control rights', () => {
    (canControlBroadcast as jest.Mock).mockReturnValue(false);

    render(<BroadcastControls broadcast={mockBroadcast} />);

    expect(
      screen.getByText(/У вас нет прав для управления этой трансляцией/),
    ).toBeInTheDocument();
  });

  // ── 3. Displays broadcast info ──────────────────────────────────────────────

  it('displays broadcast information (ID, viewers, status)', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);

    expect(screen.getByText('bc-1')).toBeInTheDocument();
    expect(screen.getByText('42 / 100')).toBeInTheDocument();
    expect(screen.getByText('В эфире')).toBeInTheDocument();
  });

  // ── 4. Enters edit mode when "Редактировать" is clicked ────────────────────

  it('enters edit mode when edit button is clicked', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);

    const editBtn = screen.getByText(/Редактировать/);
    fireEvent.click(editBtn);

    // Edit form should now be visible
    expect(screen.getByText('Название:')).toBeInTheDocument();
    expect(screen.getByText('Описание:')).toBeInTheDocument();
    expect(screen.getByText('Публичная трансляция')).toBeInTheDocument();
  });

  // ── 5. Cancels editing when cancel button is clicked ────────────────────────

  it('cancels editing when cancel button is clicked', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);

    // Enter edit mode
    fireEvent.click(screen.getByText(/Редактировать/));
    expect(screen.getByText('Название:')).toBeInTheDocument();

    // Cancel
    fireEvent.click(screen.getByText('Отмена'));
    expect(screen.queryByText('Название:')).not.toBeInTheDocument();
  });

  // ── 6. Shows start button disabled when broadcast is live ────────────────────

  it('disables start button when broadcast status is live', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);
    const startBtn = screen.getByText(/Начать трансляцию/);
    expect(startBtn).toBeDisabled();
  });

  // ── 7. Shows stop button enabled when broadcast is live ──────────────────────

  it('enables stop button when broadcast status is live', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);
    const stopBtn = screen.getByText(/Остановить трансляцию/);
    expect(stopBtn).not.toBeDisabled();
  });

  // ── 8. Calls onStatusChange when status button is clicked ───────────────────

  it('calls onStatusChange when a status button is clicked', async () => {
    const onStatusChange = jest.fn();

    render(
      <BroadcastControls
        broadcast={mockBroadcast}
        onStatusChange={onStatusChange}
      />,
    );

    // For 'live' status, the status controls show "Пауза" and "Завершить"
    const pauseBtn = screen.getByText('Пауза');
    fireEvent.click(pauseBtn);

    await waitFor(() => {
      expect(onStatusChange).toHaveBeenCalledWith('paused');
    });
  });

  // ── 9. Shows advanced quality controls when showAdvanced is true ────────────

  it('shows quality controls when showAdvanced is true', () => {
    render(<BroadcastControls broadcast={mockBroadcast} showAdvanced />);

    expect(screen.getByText('Управление качеством:')).toBeInTheDocument();
    expect(screen.getByText('1080p')).toBeInTheDocument();
    expect(screen.getByText('720p')).toBeInTheDocument();
    expect(screen.getByText('Авто')).toBeInTheDocument();
  });

  // ── 10. Hides quality controls when showAdvanced is false ───────────────────

  it('hides quality controls when showAdvanced is false', () => {
    render(<BroadcastControls broadcast={mockBroadcast} />);

    expect(screen.queryByText('Управление качеством:')).not.toBeInTheDocument();
  });

  // ── 11. Shows test functions section in advanced mode ────────────────────────

  it('shows test functions section when showAdvanced is true', () => {
    render(<BroadcastControls broadcast={mockBroadcast} showAdvanced />);

    expect(screen.getByText('Тестовые функции:')).toBeInTheDocument();
    expect(screen.getByText(/Получить статистику/)).toBeInTheDocument();
  });

  // ── 12. Calls onUpdate after saving edited broadcast ────────────────────────

  it('calls onUpdate after saving edited broadcast', async () => {
    const onUpdate = jest.fn();

    render(
      <BroadcastControls broadcast={mockBroadcast} onUpdate={onUpdate} />,
    );

    // Enter edit mode
    fireEvent.click(screen.getByText(/Редактировать/));

    // Modify the title
    const titleInput = screen.getByDisplayValue('Тестовая трансляция');
    fireEvent.change(titleInput, { target: { value: 'Новый заголовок' } });

    // Save
    fireEvent.click(screen.getByText('Сохранить'));

    await waitFor(() => {
      expect(mockUseLiveBroadcastReturn.updateBroadcast).toHaveBeenCalled();
    });
  });
});
