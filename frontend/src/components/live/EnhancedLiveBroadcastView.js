// Улучшенный интерфейс Live трансляции с чатом и настройками
import React, { useState, useEffect } from 'react';
import './EnhancedLiveBroadcastView.css';

const EnhancedLiveBroadcastView = ({
  broadcast,
  autoPlay = true,
  showChat = true,
  showControls = true,
  showStats = false,
  layout = 'player-first',
  demoMode = true,
  isOrganizer = false,
  onEndBroadcast = null
}) => {
  const [chatMessages, setChatMessages] = useState([
    { id: 1, user: 'User1', text: 'Отличная трансляция!', time: '19:10' },
    { id: 2, user: 'Admin', text: 'Спасибо за просмотр!', time: '19:11' },
    { id: 3, user: 'User2', text: 'Когда следующая трансляция?', time: '19:12' },
    { id: 4, user: 'User3', text: 'Очень интересная тема', time: '19:13' },
    { id: 5, user: 'User4', text: 'Можно повторить последний слайд?', time: '19:14' }
  ]);

  const [newMessage, setNewMessage] = useState('');
  const [viewerCount, setViewerCount] = useState(broadcast?.viewerCount || 1);
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);
  const [streamQuality, setStreamQuality] = useState('medium');
  const [audioSettings, setAudioSettings] = useState({
    bitrate: '128',
    sampleRate: '44100',
    volume: 80
  });
  const [videoSettings, setVideoSettings] = useState({
    bitrate: '2000',
    frameRate: '30',
    resolution: '720p'
  });
  const [streamDuration, setStreamDuration] = useState('00:00:00');
  const [error, setError] = useState(null);

  // Имитация увеличения количества зрителей
  useEffect(() => {
    const interval = setInterval(() => {
      setViewerCount(prev => prev + Math.floor(Math.random() * 3));
    }, 30000); // Каждые 30 секунд

    return () => clearInterval(interval);
  }, []);

  // Имитация времени трансляции
  useEffect(() => {
    const startTime = new Date();
    const interval = setInterval(() => {
      const now = new Date();
      const diffMs = now - startTime;
      const hours = Math.floor(diffMs / (1000 * 60 * 60));
      const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);
      setStreamDuration(`${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`);
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  const handleSendMessage = () => {
    if (!newMessage.trim()) return;

    const newMsg = {
      id: chatMessages.length + 1,
      user: isOrganizer ? 'Организатор' : 'Вы',
      text: newMessage,
      time: new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
    };

    setChatMessages([...chatMessages, newMsg]);
    setNewMessage('');
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSendMessage();
    }
  };

  const handleQualityChange = (quality) => {
    setStreamQuality(quality);
    alert(`Качество стрима изменено на: ${quality === 'low' ? 'Низкое (360p)' : quality === 'medium' ? 'Среднее (720p)' : 'Высокое (1080p)'}`);
  };

  const handleAudioSettingChange = (setting, value) => {
    setAudioSettings(prev => ({ ...prev, [setting]: value }));
  };

  const handleVideoSettingChange = (setting, value) => {
    setVideoSettings(prev => ({ ...prev, [setting]: value }));
  };

  const clearError = () => {
    setError(null);
  };

  const handleEndBroadcast = () => {
    if (window.confirm('Вы уверены, что хотите завершить трансляцию?')) {
      if (onEndBroadcast) {
        onEndBroadcast();
      } else {
        alert('Трансляция завершена!');
      }
    }
  };

  const handlePauseBroadcast = () => {
    alert('Трансляция приостановлена');
  };

  const handleMuteAudio = () => {
    alert('Звук выключен');
  };

  return (
    <div className="enhanced-live-broadcast">
      {/* Отображение ошибок */}
      {error && (
        <div className="error-message">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
          <button className="error-close" onClick={clearError}>×</button>
        </div>
      )}

      <div className="broadcast-header">
        <div className="header-left">
          <h2 className="broadcast-title">
            <span className="live-badge">🔴 LIVE</span>
            {broadcast?.title || 'Демо трансляция'}
          </h2>
          <div className="broadcast-info">
            <span className="info-item">👁️ {viewerCount} зрителей</span>
            <span className="info-item">⏱️ {streamDuration}</span>
            <span className="info-item">
              {broadcast?.type === 'audio' ? '🎙️ Аудио' : '🎥 Видео'}
            </span>
          </div>
        </div>
        <div className="header-right">
          {isOrganizer && (
            <button
              className="end-broadcast-btn"
              onClick={handleEndBroadcast}
              title="Завершить трансляцию"
            >
              🔴 Завершить
            </button>
          )}
        </div>
      </div>

      <div className="broadcast-content">
        {/* Основной контент - видео/аудио плеер */}
        <div className="main-player-section">
          <div className="player-container">
            <div className="player-placeholder">
              <div className="placeholder-content">
                {broadcast?.type === 'audio' ? (
                  <>
                    <div className="audio-visualizer">
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                      <div className="visualizer-bar"></div>
                    </div>
                    <div className="audio-info">
                      <div className="audio-icon">🎙️</div>
                      <div className="audio-text">Аудио трансляция в эфире</div>
                    </div>
                  </>
                ) : (
                  <>
                    <div className="video-preview">
                      <div className="preview-text">Видео трансляция в эфире</div>
                      <div className="preview-placeholder">
                        <div className="camera-icon">📹</div>
                        <div className="streamer-name">Организатор</div>
                      </div>
                    </div>
                  </>
                )}
                <div className="demo-notice">🎮 Демо режим: Live streaming система</div>
              </div>
            </div>

            {/* Панель управления плеером */}
            <div className="player-controls">
              <button className="player-control-btn" onClick={handlePauseBroadcast}>
                ⏸️ Пауза
              </button>
              <button className="player-control-btn" onClick={handleMuteAudio}>
                🔇 Выкл. звук
              </button>
              <div className="volume-control">
                <span>🔊</span>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={audioSettings.volume}
                  onChange={(e) => handleAudioSettingChange('volume', e.target.value)}
                />
                <span>{audioSettings.volume}%</span>
              </div>
            </div>
          </div>

          {/* Меню настроек (внизу слева) */}
          <div className="settings-menu-container">
            <button
              className="settings-toggle-btn"
              onClick={() => setShowSettingsMenu(!showSettingsMenu)}
              title="Настройки трансляции"
            >
              ⚙️ Настройки
            </button>

            {showSettingsMenu && (
              <div className="settings-menu">
                <div className="settings-header">
                  <h4>Настройки трансляции</h4>
                  <button
                    className="close-settings-btn"
                    onClick={() => setShowSettingsMenu(false)}
                  >
                    ✕
                  </button>
                </div>

                <div className="settings-section">
                  <h5>Качество стрима</h5>
                  <div className="quality-buttons">
                    <button
                      className={`quality-btn ${streamQuality === 'low' ? 'active' : ''}`}
                      onClick={() => handleQualityChange('low')}
                    >
                      Низкое
                    </button>
                    <button
                      className={`quality-btn ${streamQuality === 'medium' ? 'active' : ''}`}
                      onClick={() => handleQualityChange('medium')}
                    >
                      Среднее
                    </button>
                    <button
                      className={`quality-btn ${streamQuality === 'high' ? 'active' : ''}`}
                      onClick={() => handleQualityChange('high')}
                    >
                      Высокое
                    </button>
                  </div>
                  <div className="current-quality">
                    Текущее: {streamQuality === 'low' ? '360p' : streamQuality === 'medium' ? '720p' : '1080p'}
                  </div>
                </div>

                <div className="settings-section">
                  <h5>Аудио настройки</h5>
                  <div className="audio-controls">
                    <label>
                      Битрейт:
                      <select
                        value={audioSettings.bitrate}
                        onChange={(e) => handleAudioSettingChange('bitrate', e.target.value)}
                      >
                        <option value="64">64 kbps</option>
                        <option value="128">128 kbps</option>
                        <option value="192">192 kbps</option>
                        <option value="256">256 kbps</option>
                      </select>
                    </label>
                    <label>
                      Частота:
                      <select
                        value={audioSettings.sampleRate}
                        onChange={(e) => handleAudioSettingChange('sampleRate', e.target.value)}
                      >
                        <option value="22050">22.05 kHz</option>
                        <option value="44100">44.1 kHz</option>
                        <option value="48000">48 kHz</option>
                      </select>
                    </label>
                  </div>
                </div>

                {broadcast?.type === 'video' && (
                  <div className="settings-section">
                    <h5>Видео настройки</h5>
                    <div className="video-controls">
                      <label>
                        Разрешение:
                        <select
                          value={videoSettings.resolution}
                          onChange={(e) => handleVideoSettingChange('resolution', e.target.value)}
                        >
                          <option value="360p">360p</option>
                          <option value="480p">480p</option>
                          <option value="720p">720p</option>
                          <option value="1080p">1080p</option>
                        </select>
                      </label>
                      <label>
                        Кадры:
                        <select
                          value={videoSettings.frameRate}
                          onChange={(e) => handleVideoSettingChange('frameRate', e.target.value)}
                        >
                          <option value="24">24 fps</option>
                          <option value="30">30 fps</option>
                          <option value="60">60 fps</option>
                        </select>
                      </label>
                    </div>
                  </div>
                )}

                <div className="settings-section">
                  <h5>Дополнительно</h5>
                  <div className="additional-settings">
                    <label className="checkbox-label">
                      <input type="checkbox" defaultChecked />
                      <span>Запись трансляции</span>
                    </label>
                    <label className="checkbox-label">
                      <input type="checkbox" defaultChecked />
                      <span>Сохранять чат</span>
                    </label>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Чат (справа) */}
        {showChat && (
          <div className="chat-section">
            <div className="chat-header">
              <h3>💬 Чат трансляции</h3>
              <span className="chat-count">{chatMessages.length} сообщений</span>
            </div>

            <div className="chat-messages">
              {chatMessages.map(message => (
                <div key={message.id} className="chat-message">
                  <div className="message-header">
                    <span className="message-user">{message.user}</span>
                    <span className="message-time">{message.time}</span>
                  </div>
                  <div className="message-text">{message.text}</div>
                </div>
              ))}
            </div>

            <div className="chat-input">
              <input
                type="text"
                placeholder="Написать сообщение..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyPress={handleKeyPress}
              />
              <button onClick={handleSendMessage}>Отправить</button>
            </div>
          </div>
        )}
      </div>

      {/* Статистика (если включена) */}
      {showStats && (
        <div className="stats-section">
          <h3>📊 Статистика трансляции</h3>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon">👁️</div>
              <div className="stat-value">{viewerCount}</div>
              <div className="stat-label">Зрителей онлайн</div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">⏱️</div>
              <div className="stat-value">{streamDuration}</div>
              <div className="stat-label">Длительность</div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">💬</div>
              <div className="stat-value">{chatMessages.length}</div>
              <div className="stat-label">Сообщений</div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">📡</div>
              <div className="stat-value">{streamQuality === 'low' ? '360p' : streamQuality === 'medium' ? '720p' : '1080p'}</div>
              <div className="stat-label">Качество</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EnhancedLiveBroadcastView;