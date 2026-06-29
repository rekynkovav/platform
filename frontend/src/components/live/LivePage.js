// LivePage — основная страница live трансляции
// Загружает broadcast из API, рендерит плеер + чат + управление

import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { LivePlayer } from './LivePlayer';
import { LiveChat } from './LiveChat';
import { BroadcastControls } from './BroadcastControls';
import { useLiveSession } from '../../context/live/LiveSessionContext';
import './LivePage.css';

/**
 * LivePage — full live broadcast page.
 *
 * Loads active broadcast from API (or by ID from URL params),
 * connects to the live session, and renders player + chat + controls.
 *
 * Layout:
 * - Player (left/main area)
 * - Chat (right sidebar)
 * - BroadcastControls (below player, admin only)
 *
 * @returns {JSX.Element} The live broadcast page
 */
function LivePage() {
  const { id: broadcastIdFromUrl } = useParams();
  const {
    state,
    connectToBroadcast,
    disconnectFromBroadcast
  } = useLiveSession();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const loadBroadcast = async () => {
      setLoading(true);
      setError(null);

      try {
        if (broadcastIdFromUrl) {
          // Load specific broadcast by ID from URL
          await connectToBroadcast(broadcastIdFromUrl);
        } else {
          // Load active broadcast (most recent LIVE one)
          const { liveApi } = await import('../../services/live');
          const activeBroadcasts = await liveApi.getActiveBroadcasts();
          const liveBroadcast = activeBroadcasts.find(b => b.status === 'live')
            || activeBroadcasts[0];

          if (liveBroadcast) {
            await connectToBroadcast(liveBroadcast.id);
          } else {
            if (!cancelled) {
              setError('Нет активных трансляций');
            }
          }
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Ошибка загрузки трансляции');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadBroadcast();

    return () => {
      cancelled = true;
      disconnectFromBroadcast();
    };
  }, [broadcastIdFromUrl, connectToBroadcast, disconnectFromBroadcast]);

  // Loading state
  if (loading) {
    return (
      <div className="live-page">
        <div className="live-page-loading">
          <div className="loading-spinner" />
          <div className="loading-text">Загрузка трансляции...</div>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="live-page">
        <div className="live-page-error">
          <div className="error-icon">⚠️</div>
          <div className="error-text">{error}</div>
          <button
            className="error-retry"
            onClick={() => window.location.reload()}
          >
            Попробовать снова
          </button>
        </div>
      </div>
    );
  }

  const broadcast = state.broadcast;

  // No broadcast loaded
  if (!broadcast) {
    return (
      <div className="live-page">
        <div className="live-page-empty">
          <div className="empty-icon">📡</div>
          <div className="empty-text">Трансляция не найдена</div>
        </div>
      </div>
    );
  }

  return (
    <div className="live-page">
      <div className="live-page-main">
        {/* Player area */}
        <div className="live-page-player">
          <LivePlayer
            broadcast={broadcast}
            autoPlay={true}
            muted={false}
            controls={true}
          />
        </div>

        {/* Broadcast controls (admin/moderator only) */}
        {state.isAdmin && (
          <div className="live-page-controls">
            <BroadcastControls broadcast={broadcast} />
          </div>
        )}
      </div>

      {/* Chat sidebar */}
      <div className="live-page-chat">
        <LiveChat
          broadcastId={broadcast.id}
          maxHeight="calc(100vh - 200px)"
          showReactions={true}
          showQuestions={true}
        />
      </div>
    </div>
  );
}

export default LivePage;
