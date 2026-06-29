import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import LivePage from '../components/live/LivePage';

// Mock child components
jest.mock('../components/live/LivePlayer', () => ({
  LivePlayer: (props) => <div data-testid="live-player">{JSON.stringify(props)}</div>,
}));
jest.mock('../components/live/LiveChat', () => ({
  LiveChat: (props) => <div data-testid="live-chat">{JSON.stringify(props)}</div>,
}));
jest.mock('../components/live/BroadcastControls', () => ({
  BroadcastControls: (props) => <div data-testid="broadcast-controls">{JSON.stringify(props)}</div>,
}));

// Mock LiveSessionContext
const mockConnect = jest.fn();
const mockDisconnect = jest.fn();
jest.mock('../context/live/LiveSessionContext', () => ({
  useLiveSession: jest.fn(),
}));

// Mock live API
jest.mock('../services/live', () => ({
  liveApi: {
    getActiveBroadcasts: jest.fn(),
  },
}));

// Mock CSS
jest.mock('../components/live/LivePage.css', () => ({}));

const { useLiveSession } = require('../context/live/LiveSessionContext');
const { liveApi } = require('../services/live');

const renderPage = (initialEntries = ['/live']) =>
  render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/live/:id?" element={<LivePage />} />
      </Routes>
    </MemoryRouter>
  );

describe('LivePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockConnect.mockResolvedValue(undefined);
    mockDisconnect.mockResolvedValue(undefined);
  });

  it('shows loading state initially', () => {
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockReturnValue(new Promise(() => {})); // never resolves

    renderPage();
    expect(screen.getByText('Загрузка трансляции...')).toBeInTheDocument();
  });

  it('loads active broadcast when no ID in URL', async () => {
    const broadcast = { id: 'b1', title: 'Test', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(mockConnect).toHaveBeenCalledWith('b1');
    });
  });

  it('loads broadcast by ID from URL', async () => {
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });

    renderPage(['/live/abc123']);

    await waitFor(() => {
      expect(mockConnect).toHaveBeenCalledWith('abc123');
    });
  });

  it('shows error when no active broadcasts', async () => {
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByText('Нет активных трансляций')).toBeInTheDocument();
    });
  });

  it('shows error on API failure', async () => {
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockRejectedValue(new Error('Network error'));

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('shows empty state when broadcast is null after load', async () => {
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([{ id: 'b1', status: 'ended' }]);
    // connectToBroadcast resolves but state.broadcast stays null (simulated)

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByText('Трансляция не найдена')).toBeInTheDocument();
    });
  });

  it('renders player and chat when broadcast loaded', async () => {
    const broadcast = { id: 'b1', title: 'Live Stream', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByTestId('live-player')).toBeInTheDocument();
      expect(screen.getByTestId('live-chat')).toBeInTheDocument();
    });
  });

  it('shows broadcast controls for admin', async () => {
    const broadcast = { id: 'b1', title: 'Admin Stream', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: true },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByTestId('broadcast-controls')).toBeInTheDocument();
    });
  });

  it('hides broadcast controls for non-admin', async () => {
    const broadcast = { id: 'b1', title: 'Stream', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(screen.getByTestId('live-player')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('broadcast-controls')).not.toBeInTheDocument();
  });

  it('disconnects on unmount', async () => {
    const broadcast = { id: 'b1', title: 'Test', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    const { unmount } = renderPage(['/live']);
    await waitFor(() => expect(mockConnect).toHaveBeenCalled());

    unmount();
    expect(mockDisconnect).toHaveBeenCalled();
  });

  it('passes correct props to LivePlayer', async () => {
    const broadcast = { id: 'b1', title: 'Stream', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      const player = screen.getByTestId('live-player');
      expect(player).toHaveTextContent('"autoPlay":true');
      expect(player).toHaveTextContent('"muted":false');
      expect(player).toHaveTextContent('"controls":true');
    });
  });

  it('passes correct props to LiveChat', async () => {
    const broadcast = { id: 'b1', title: 'Stream', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([broadcast]);

    renderPage(['/live']);

    await waitFor(() => {
      const chat = screen.getByTestId('live-chat');
      expect(chat).toHaveTextContent('"showReactions":true');
      expect(chat).toHaveTextContent('"showQuestions":true');
    });
  });

  it('prefers live broadcast over ended', async () => {
    const ended = { id: 'b1', status: 'ended' };
    const live = { id: 'b2', status: 'live' };
    useLiveSession.mockReturnValue({
      state: { broadcast: null, isAdmin: false },
      connectToBroadcast: mockConnect,
      disconnectFromBroadcast: mockDisconnect,
    });
    liveApi.getActiveBroadcasts.mockResolvedValue([ended, live]);

    renderPage(['/live']);

    await waitFor(() => {
      expect(mockConnect).toHaveBeenCalledWith('b2');
    });
  });
});
