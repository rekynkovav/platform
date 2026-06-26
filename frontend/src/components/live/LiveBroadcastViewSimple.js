// Простая JavaScript версия LiveBroadcastView без styled-jsx
import React from 'react';
import './LiveBroadcastView.css';

/**
 * LiveBroadcastView — simplified live broadcast display without styled-jsx dependencies.
 *
 * Renders a basic live broadcast interface with a placeholder player area,
 * viewer count display, optional chat panel with demo messages, and stream
 * statistics. Used as a lightweight alternative to EnhancedLiveBroadcastView.
 *
 * @param {Object} broadcast - The broadcast data object with title, viewerCount, type, etc.
 * @param {boolean} [autoPlay=true] - Whether to start playback automatically
 * @param {boolean} [showChat=true] - Whether to display the chat panel
 * @param {boolean} [showControls=true] - Whether to display stream controls
 * @param {boolean} [showStats=false] - Whether to display stream statistics
 * @param {string} [layout='player-first'] - Layout mode: 'player-first' or 'chat-first'
 * @param {boolean} [demoMode=true] - Whether to use mock data for demonstration
 * @returns {JSX.Element} The simplified live broadcast view
 */
const LiveBroadcastView = ({ broadcast, autoPlay = true, showChat = true, showControls = true, showStats = false, layout = 'player-first', demoMode = true }) => {
  return (
    <div className="live-broadcast-simple">
      <div className="simple-header">
        <h2>🔴 Live трансляция: {broadcast?.title || 'Демо трансляция'}</h2>
        <div className="simple-status">
          <span className="status-badge">LIVE</span>
          <span className="viewer-count">👁️ {broadcast?.viewerCount || 1} зрителей</span>
        </div>
      </div>

      <div className="simple-player">
        <div className="player-placeholder">
          <div className="placeholder-content">
            <div className="play-icon">▶️</div>
            <div className="placeholder-text">
              {broadcast?.type === 'audio' ? 'Аудио трансляция' : 'Видео трансляция'}
            </div>
            <div className="demo-notice">🎮 Демо режим: Live streaming система</div>
          </div>
        </div>
      </div>

      {showChat && (
        <div className="simple-chat">
          <div className="chat-header">
            <h3>💬 Чат трансляции</h3>
            <span className="chat-count">3 участника</span>
          </div>
          <div className="chat-messages">
            <div className="message">
              <span className="user">👤 User1:</span>
              <span className="text">Отличная трансляция!</span>
            </div>
            <div className="message">
              <span className="user">👤 Admin:</span>
              <span className="text">Спасибо за просмотр!</span>
            </div>
            <div className="message">
              <span className="user">👤 User2:</span>
              <span className="text">Когда следующая трансляция?</span>
            </div>
          </div>
          <div className="chat-input">
            <input type="text" placeholder="Написать сообщение..." />
            <button>Отправить</button>
          </div>
        </div>
      )}

      {showControls && (
        <div className="simple-controls">
          <h3>⚙️ Управление трансляцией</h3>
          <div className="controls-grid">
            <button className="control-btn">⏸️ Пауза</button>
            <button className="control-btn">🔇 Выключить звук</button>
            <button className="control-btn">📊 Статистика</button>
            <button className="control-btn">🔴 Завершить</button>
          </div>
        </div>
      )}

      {showStats && (
        <div className="simple-stats">
          <h3>📊 Статистика</h3>
          <div className="stats-grid">
            <div className="stat-item">
              <div className="stat-label">Зрители онлайн</div>
              <div className="stat-value">{broadcast?.viewerCount || 1}</div>
            </div>
            <div className="stat-item">
              <div className="stat-label">Длительность</div>
              <div className="stat-value">12:34</div>
            </div>
            <div className="stat-item">
              <div className="stat-label">Сообщений в чате</div>
              <div className="stat-value">24</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LiveBroadcastView;