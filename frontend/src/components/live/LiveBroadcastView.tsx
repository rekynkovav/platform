// Главный компонент live трансляции
// Объединяет плеер, чат, управление и статистику

import React, { useState, useCallback, useEffect } from 'react';
import { LiveBroadcast } from '../../services/live/types';
import { useLiveBroadcast } from '../../hooks/live/useLiveBroadcast';
import { useLiveSession } from '../../contexts/live/LiveSessionContext';
import { LiveSessionProvider } from '../../contexts/live/LiveSessionContext';
import { LivePlayer } from './LivePlayer';
import { LiveChat } from './LiveChat';
import { BroadcastControls } from './BroadcastControls';
import {
  formatDuration,
  getStatusText,
  canJoinBroadcast,
  generateDemoBroadcast
} from '../../utils/live/helpers';

interface LiveBroadcastViewProps {
  broadcastId?: string;
  broadcast?: LiveBroadcast;
  autoPlay?: boolean;
  showChat?: boolean;
  showControls?: boolean;
  showStats?: boolean;
  layout?: 'player-first' | 'chat-first' | 'vertical' | 'horizontal';
  demoMode?: boolean;
  onError?: (error: Error) => void;
  onReady?: () => void;
}

export const LiveBroadcastView: React.FC<LiveBroadcastViewProps> = ({
  broadcastId,
  broadcast: initialBroadcast,
  autoPlay = true,
  showChat = true,
  showControls = true,
  showStats = false,
  layout = 'player-first',
  demoMode = false,
  onError,
  onReady
}) => {
  const [activeTab, setActiveTab] = useState<'player' | 'chat' | 'controls' | 'stats'>('player');
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showUserList, setShowUserList] = useState(false);
  const [showQuestions, setShowQuestions] = useState(true);
  const [showReactions, setShowReactions] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Используем переданную трансляцию или загружаем по ID
  const { broadcast, isLoading: broadcastLoading, error: broadcastError } = useLiveBroadcast(
    broadcastId || initialBroadcast?.id
  );

  const { state: liveSession } = useLiveSession();

  // Определяем текущую трансляцию
  const currentBroadcast = initialBroadcast || broadcast;

  // Проверка возможности присоединиться
  const canJoin = canJoinBroadcast(currentBroadcast);

  // Обработка ошибок
  useEffect(() => {
    if (broadcastError) {
      const errorMessage = broadcastError instanceof Error ? broadcastError.message : 'Ошибка загрузки трансляции';
      setError(errorMessage);
      if (onError) {
        onError(new Error(errorMessage));
      }
    }
  }, [broadcastError, onError]);

  // Обработка готовности
  useEffect(() => {
    if (!broadcastLoading && currentBroadcast) {
      setIsLoading(false);
      if (onReady) {
        onReady();
      }
    }
  }, [broadcastLoading, currentBroadcast, onReady]);

  // Обработка полноэкранного режима
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  // Переключение в полноэкранный режим
  const toggleFullscreen = useCallback(() => {
    const container = document.querySelector('.live-broadcast-container');
    if (!container) return;

    if (!document.fullscreenElement) {
      container.requestFullscreen().catch(err => {
        console.error('[LiveBroadcastView] Ошибка входа в полноэкранный режим:', err);
      });
    } else {
      document.exitFullscreen();
    }
  }, []);

  // Переключение вкладок
  const handleTabChange = useCallback((tab: 'player' | 'chat' | 'controls' | 'stats') => {
    setActiveTab(tab);
  }, []);

  // Обновление настроек чата
  const handleChatSettingsChange = useCallback((settings: {
    showUserList?: boolean;
    showQuestions?: boolean;
    showReactions?: boolean;
  }) => {
    if (settings.showUserList !== undefined) setShowUserList(settings.showUserList);
    if (settings.showQuestions !== undefined) setShowQuestions(settings.showQuestions);
    if (settings.showReactions !== undefined) setShowReactions(settings.showReactions);
  }, []);

  // Экспорт данных трансляции
  const handleExportData = useCallback(() => {
    if (!currentBroadcast) return;

    const data = {
      broadcast: currentBroadcast,
      timestamp: new Date().toISOString(),
      user: liveSession.user
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `broadcast-${currentBroadcast.id}-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [currentBroadcast, liveSession.user]);

  // Рефреш трансляции
  const handleRefresh = useCallback(() => {
    window.location.reload();
  }, []);

  // Рендер заголовка
  const renderHeader = useCallback(() => {
    if (!currentBroadcast) return null;

    return (
      <div className="broadcast-header">
        <div className="header-left">
          <h1 className="broadcast-title">{currentBroadcast.title}</h1>
          {currentBroadcast.description && (
            <p className="broadcast-description">{currentBroadcast.description}</p>
          )}
        </div>

        <div className="header-right">
          <div className="broadcast-status">
            <span className="status-label">Статус:</span>
            <span className={`status-value ${currentBroadcast.status}`}>
              {getStatusText(currentBroadcast.status)}
            </span>
          </div>

          <div className="broadcast-stats">
            <span className="stat-item">
              👁️ {currentBroadcast.viewerCount}
            </span>
            {currentBroadcast.startedAt && (
              <span className="stat-item">
                🕐 {formatDuration((Date.now() - new Date(currentBroadcast.startedAt).getTime()) / 1000)}
              </span>
            )}
          </div>
        </div>
      </div>
    );
  }, [currentBroadcast]);

  // Рендер навигации
  const renderNavigation = useCallback(() => {
    if (isFullscreen) return null;

    return (
      <div className="broadcast-navigation">
        <div className="nav-tabs">
          <button
            className={`nav-tab ${activeTab === 'player' ? 'active' : ''}`}
            onClick={() => handleTabChange('player')}
          >
            🎥 Плеер
          </button>
          {showChat && (
            <button
              className={`nav-tab ${activeTab === 'chat' ? 'active' : ''}`}
              onClick={() => handleTabChange('chat')}
            >
              💬 Чат
            </button>
          )}
          {showControls && (liveSession.isAdmin || liveSession.isModerator) && (
            <button
              className={`nav-tab ${activeTab === 'controls' ? 'active' : ''}`}
              onClick={() => handleTabChange('controls')}
            >
              ⚙️ Управление
            </button>
          )}
          {showStats && (
            <button
              className={`nav-tab ${activeTab === 'stats' ? 'active' : ''}`}
              onClick={() => handleTabChange('stats')}
            >
              📊 Статистика
            </button>
          )}
        </div>

        <div className="nav-actions">
          <button
            className="nav-action"
            onClick={toggleFullscreen}
            title={isFullscreen ? 'Выйти из полноэкранного режима' : 'Полноэкранный режим'}
          >
            {isFullscreen ? '⤢' : '⤡'}
          </button>
          <button
            className="nav-action"
            onClick={handleExportData}
            title="Экспорт данных"
          >
            💾
          </button>
          <button
            className="nav-action"
            onClick={handleRefresh}
            title="Обновить"
          >
            🔄
          </button>
        </div>
      </div>
    );
  }, [isFullscreen, activeTab, showChat, showControls, showStats, liveSession, handleTabChange, toggleFullscreen, handleExportData, handleRefresh]);

  // Рендер плеера
  const renderPlayer = useCallback(() => {
    if (!currentBroadcast) return null;

    return (
      <div className="player-section">
        <LivePlayer
          broadcast={currentBroadcast}
          autoPlay={autoPlay}
          onError={onError}
          onReady={onReady}
        />
      </div>
    );
  }, [currentBroadcast, autoPlay, onError, onReady]);

  // Рендер чата
  const renderChat = useCallback(() => {
    if (!currentBroadcast || !showChat) return null;

    return (
      <div className="chat-section">
        <LiveChat
          broadcastId={currentBroadcast.id}
          showReactions={showReactions}
          showQuestions={showQuestions}
          showUserList={showUserList}
        />
      </div>
    );
  }, [currentBroadcast, showChat, showReactions, showQuestions, showUserList]);

  // Рендер управления
  const renderControls = useCallback(() => {
    if (!currentBroadcast || !showControls || !(liveSession.isAdmin || liveSession.isModerator)) {
      return null;
    }

    return (
      <div className="controls-section">
        <BroadcastControls
          broadcast={currentBroadcast}
          showAdvanced={true}
        />
      </div>
    );
  }, [currentBroadcast, showControls, liveSession]);

  // Рендер статистики
  const renderStats = useCallback(() => {
    if (!currentBroadcast || !showStats) return null;

    return (
      <div className="stats-section">
        <div className="stats-panel">
          <h3>Статистика трансляции</h3>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-label">Зрители онлайн</div>
              <div className="stat-value">{currentBroadcast.viewerCount}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Максимум зрителей</div>
              <div className="stat-value">{currentBroadcast.maxViewers}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Длительность</div>
              <div className="stat-value">
                {currentBroadcast.startedAt
                  ? formatDuration((Date.now() - new Date(currentBroadcast.startedAt).getTime()) / 1000)
                  : '00:00'}
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Статус</div>
              <div className="stat-value">{getStatusText(currentBroadcast.status)}</div>
            </div>
          </div>
        </div>
      </div>
    );
  }, [currentBroadcast, showStats]);

  // Рендер основного содержимого
  const renderContent = useCallback(() => {
    if (isLoading) {
      return (
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <div className="loading-text">Загрузка трансляции...</div>
        </div>
      );
    }

    if (error) {
      return (
        <div className="error-container">
          <div className="error-icon">⚠️</div>
          <div className="error-title">Ошибка загрузки трансляции</div>
          <div className="error-message">{error}</div>
          <button className="error-retry" onClick={handleRefresh}>
            Попробовать снова
          </button>
        </div>
      );
    }

    if (!currentBroadcast) {
      return (
        <div className="no-broadcast">
          <div className="no-broadcast-icon">📺</div>
          <div className="no-broadcast-title">Трансляция не найдена</div>
          <div className="no-broadcast-message">
            Проверьте ID трансляции или попробуйте позже.
          </div>
        </div>
      );
    }

    if (!canJoin) {
      return (
        <div className="cannot-join">
          <div className="cannot-join-icon">🔒</div>
          <div className="cannot-join-title">Трансляция недоступна</div>
          <div className="cannot-join-message">
            Статус: {getStatusText(currentBroadcast.status)}<br />
            Присоединиться можно только к активным трансляциям.
          </div>
        </div>
      );
    }

    // Определяем layout
    switch (layout) {
      case 'player-first':
        return (
          <div className="content-layout player-first">
            <div className="main-content">
              {renderPlayer()}
            </div>
            {showChat && (
              <div className="sidebar-content">
                {renderChat()}
              </div>
            )}
          </div>
        );

      case 'chat-first':
        return (
          <div className="content-layout chat-first">
            {showChat && (
              <div className="sidebar-content">
                {renderChat()}
              </div>
            )}
            <div className="main-content">
              {renderPlayer()}
            </div>
          </div>
        );

      case 'vertical':
        return (
          <div className="content-layout vertical">
            {renderPlayer()}
            {showChat && renderChat()}
          </div>
        );

      case 'horizontal':
        return (
          <div className="content-layout horizontal">
            <div className="horizontal-row">
              {renderPlayer()}
              {showChat && renderChat()}
            </div>
          </div>
        );

      default:
        return (
          <div className="content-layout default">
            {renderPlayer()}
            {showChat && renderChat()}
          </div>
        );
    }
  }, [isLoading, error, currentBroadcast, canJoin, layout, showChat, renderPlayer, renderChat, handleRefresh]);

  // Рендер активной вкладки
  const renderActiveTab = useCallback(() => {
    if (isFullscreen) {
      return renderPlayer();
    }

    switch (activeTab) {
      case 'player':
        return renderContent();
      case 'chat':
        return renderChat();
      case 'controls':
        return renderControls();
      case 'stats':
        return renderStats();
      default:
        return renderContent();
    }
  }, [isFullscreen, activeTab, renderPlayer, renderContent, renderChat, renderControls, renderStats]);

  // Демо режим
  if (demoMode && !currentBroadcast) {
    const demoBroadcast = generateDemoBroadcast({
      id: broadcastId || 'demo-broadcast',
      status: 'live',
      title: 'Демо трансляция: Live Streaming System'
    });

    return (
      <LiveSessionProvider broadcast={demoBroadcast}>
        <LiveBroadcastView
          broadcast={demoBroadcast}
          autoPlay={autoPlay}
          showChat={showChat}
          showControls={showControls}
          showStats={showStats}
          layout={layout}
          demoMode={false}
          onError={onError}
          onReady={onReady}
        />
      </LiveSessionProvider>
    );
  }

  return (
    <div className={`live-broadcast-container ${isFullscreen ? 'fullscreen' : ''}`}>
      {/* Заголовок */}
      {!isFullscreen && renderHeader()}

      {/* Навигация */}
      {renderNavigation()}

      {/* Основное содержимое */}
      <div className="broadcast-content">
        {renderActiveTab()}
      </div>

      {/* Информация о демо режиме */}
      {demoMode && !isFullscreen && (
        <div className="demo-notice">
          🎮 Демо режим: Используются мок-данные. Для реальной трансляции подключите бэкенд.
        </div>
      )}
    </div>
  );
};

// Стили компонента
const styles = `
.live-broadcast-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.live-broadcast-container.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  border-radius: 0;
}

.broadcast-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
}

.header-left {
  flex: 1;
  margin-right: 20px;
}

.broadcast-title {
  margin: 0 0 10px 0;
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.broadcast-description {
  margin: 0;
  color: #aaa;
  font-size: 14px;
  line-height: 1.4;
}

.header-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 200px;
}

.broadcast-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  color: #888;
  font-size: 14px;
}

.status-value {
  font-weight: bold;
  font-size: 16px;
  padding: 4px 12px;
  border-radius: 20px;
  background: #333;
}

.status-value.live {
  background: #4CAF50;
  color: white;
}

.status-value.preparing {
  background: #2196F3;
  color: white;
}

.status-value.paused {
  background: #ff9800;
  color: white;
}

.status-value.ended {
  background: #f44336;
  color: white;
}

.status-value.idle {
  background: #666;
  color: white;
}

.broadcast-stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #aaa;
  font-size: 14px;
}

.broadcast-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
  min-height: 50px;
}

.nav-tabs {
  display: flex;
  gap: 2px;
}

.nav-tab {
  background: #333;
  color: white;
  border: none;
  padding: 12px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px 6px 0 0;
  transition: background 0.2s;
}

.nav-tab:hover {
  background: #444;
}

.nav-tab.active {
  background: #1a1a1a;
  color: #4CAF50;
  font-weight: bold;
}

.nav-actions {
  display: flex;
  gap: 8px;
}

.nav-action {
  background: #333;
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
}

.nav-action:hover {
  background: #444;
}

.broadcast-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
}

.loading-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top-color: #4CAF50;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: white;
  font-size: 16px;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.error-title {
  color: white;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
}

.error-message {
  color: #aaa;
  font-size: 14px;
  margin-bottom: 20px;
  max-width: 80%;
}

.error-retry {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.error-retry:hover {
  background: #45a049;
}

.no-broadcast, .cannot-join {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
}

.no-broadcast-icon, .cannot-join-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.no-broadcast-title, .cannot-join-title {
  color: white;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
}

.no-broadcast-message, .cannot-join-message {
  color: #aaa;
  font-size: 14px;
  max-width: 80%;
}

.content-layout {
  display: flex;
  height: 100%;
  gap: 20px;
  padding: 20px;
}

.content-layout.player-first {
  flex-direction: row;
}

.content-layout.chat-first {
  flex-direction: row-reverse;
}

.content-layout.vertical {
  flex-direction: column;
}

.content-layout.horizontal {
  flex-direction: column;
}

.horizontal-row {
  display: flex;
  gap: 20px;
  height: 50%;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.sidebar-content {
  width: 400px;
  min-width: 300px;
  max-width: 500px;
}

.player-section, .chat-section, .controls-section, .stats-section {
  height: 100%;
}

.controls-section, .stats-section {
  padding: 20px;
  overflow-y: auto;
}

.stats-panel {
  background: #2a2a2a;
  border-radius: 8px;
  padding: 20px;
}

.stats-panel h3 {
  margin: 0 0 20px 0;
  color: white;
  font-size: 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.stat-card {
  background: #333;
  border-radius: 6px;
  padding: 15px;
  text-align: center;
}

.stat-label {
  color: #888;
  font-size: 12px;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.demo-notice {
  background: #ff9800;
  color: white;
  padding: 10px 20px;
  text-align: center;
  font-size: 14px;
  border-top: 1px solid #333;
}

/* Адаптивность */
@media (max-width: 1200px) {
  .content-layout.player-first,
  .content-layout.chat-first {
    flex-direction: column;
  }

  .sidebar-content {
    width: 100%;
    max-width: none;
  }

  .broadcast-header {
    flex-direction: column;
    gap: 15px;
  }

  .header-right {
    width: 100%;
    min-width: auto;
  }
}

@media (max-width: 768px) {
  .broadcast-title {
    font-size: 20px;
  }

  .nav-tabs {
    flex-wrap: wrap;
  }

  .nav-tab {
    padding: 8px 12px;
    font-size: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
`;

// Добавляем стили в документ
if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = styles;
  document.head.appendChild(styleElement);
}

// Экспорт с провайдером контекста
export const LiveBroadcastViewWithProvider: React.FC<LiveBroadcastViewProps> = (props) => {
  return (
    <LiveSessionProvider broadcast={props.broadcast}>
      <LiveBroadcastView {...props} />
    </LiveSessionProvider>
  );
};

export default LiveBroadcastView;