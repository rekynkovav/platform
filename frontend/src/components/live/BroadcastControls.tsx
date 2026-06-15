// Компонент управления трансляцией
// Для администраторов и модераторов

import React, { useState, useCallback } from 'react';
import { LiveBroadcast, BroadcastStatus, StreamQuality } from '../../services/live/types';
import { useLiveBroadcast } from '../../hooks/live/useLiveBroadcast';
import { useLiveSession } from '../../context/live/LiveSessionContext';
import { mediaService } from '../../services/live';
import {
  formatDuration,
  getStatusColor,
  getStatusText,
  canControlBroadcast,
  getResolutionForQuality,
  formatBitrate
} from '../../utils/live/helpers';

export interface BroadcastControlsProps {
  broadcast: LiveBroadcast;
  onUpdate?: (broadcast: LiveBroadcast) => void;
  onStatusChange?: (status: BroadcastStatus) => void;
  showAdvanced?: boolean;
}

export const BroadcastControls: React.FC<BroadcastControlsProps> = ({
  broadcast,
  onUpdate,
  onStatusChange,
  showAdvanced = false
}) => {
  const {
    updateStatus,
    startBroadcast,
    stopBroadcast,
    updateBroadcast
  } = useLiveBroadcast(broadcast.id);

  const { state: liveSession } = useLiveSession();

  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState({
    title: broadcast.title,
    description: broadcast.description || '',
    isPublic: broadcast.settings.isPublic,
    isChatEnabled: broadcast.settings.isChatEnabled,
    isReactionsEnabled: broadcast.settings.isReactionsEnabled,
    isRecording: broadcast.settings.isRecording
  });

  const [selectedQuality, setSelectedQuality] = useState<StreamQuality>('720p');
  const [streamStats, setStreamStats] = useState<any>(null);
  const [showStats, setShowStats] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Проверка прав доступа
  const canControl = canControlBroadcast(broadcast, liveSession.isAdmin);
  const isAdmin = liveSession.isAdmin;
  const isModerator = liveSession.isModerator;

  // Обновление статуса
  const handleStatusChange = useCallback(async (newStatus: BroadcastStatus) => {
    if (!canControl) return;

    setIsLoading(true);
    try {
      await updateStatus(newStatus);
      if (onStatusChange) {
        onStatusChange(newStatus);
      }
    } catch (error) {
      console.error('[BroadcastControls] Ошибка изменения статуса:', error);
    } finally {
      setIsLoading(false);
    }
  }, [canControl, updateStatus, onStatusChange]);

  // Начало трансляции
  const handleStartBroadcast = useCallback(async () => {
    if (!canControl) return;

    setIsLoading(true);
    try {
      await startBroadcast(broadcast.title, broadcast.description);
      await updateStatus('live');
    } catch (error) {
      console.error('[BroadcastControls] Ошибка начала трансляции:', error);
    } finally {
      setIsLoading(false);
    }
  }, [canControl, broadcast.title, broadcast.description, startBroadcast, updateStatus]);

  // Остановка трансляции
  const handleStopBroadcast = useCallback(async () => {
    if (!canControl) return;

    if (!window.confirm('Вы уверены, что хотите остановить трансляцию?')) {
      return;
    }

    setIsLoading(true);
    try {
      await stopBroadcast();
    } catch (error) {
      console.error('[BroadcastControls] Ошибка остановки трансляции:', error);
    } finally {
      setIsLoading(false);
    }
  }, [canControl, stopBroadcast]);

  // Обновление информации о трансляции
  const handleUpdateBroadcast = useCallback(async () => {
    if (!canControl) return;

    setIsLoading(true);
    try {
      const updated = await updateBroadcast({
        title: editData.title,
        description: editData.description,
        settings: {
          ...broadcast.settings,
          isPublic: editData.isPublic,
          isChatEnabled: editData.isChatEnabled,
          isReactionsEnabled: editData.isReactionsEnabled,
          isRecording: editData.isRecording
        }
      });

      if (onUpdate) {
        onUpdate(updated);
      }

      setIsEditing(false);
    } catch (error) {
      console.error('[BroadcastControls] Ошибка обновления трансляции:', error);
    } finally {
      setIsLoading(false);
    }
  }, [canControl, updateBroadcast, editData, broadcast.settings, onUpdate]);

  // Переключение качества потока
  const handleQualityChange = useCallback(async (quality: StreamQuality) => {
    if (!canControl) return;

    try {
      const stream = mediaService.switchQuality(broadcast.id, quality);
      setSelectedQuality(quality);
      // console.log(`[BroadcastControls] Качество переключено на: ${quality}`, stream);
    } catch (error) {
      console.error('[BroadcastControls] Ошибка переключения качества:', error);
    }
  }, [canControl, broadcast.id]);

  // Получение статистики потока
  const handleGetStats = useCallback(() => {
    const stats = mediaService.getPlayerStats(broadcast.id);
    setStreamStats(stats);
    setShowStats(true);
  }, [broadcast.id]);

  // Имитация проблем с сетью (для тестирования)
  const handleSimulateNetworkIssue = useCallback((severity: 'low' | 'medium' | 'high') => {
    if (!canControl) return;

    mediaService.simulateNetworkIssues(broadcast.id, severity);
    setTimeout(() => {
      mediaService.restoreNetwork(broadcast.id);
    }, 10000); // Восстановление через 10 секунд
  }, [canControl, broadcast.id]);

  // Сброс всех состояний (для тестирования)
  const handleResetAll = useCallback(() => {
    if (!canControl) return;

    if (window.confirm('Вы уверены, что хотите сбросить все состояния? Это действие нельзя отменить.')) {
      mediaService.reset();
      window.location.reload();
    }
  }, [canControl]);

  // Рендер статуса
  const renderStatus = useCallback(() => {
    const statusColor = getStatusColor(broadcast.status);
    const statusText = getStatusText(broadcast.status);

    return (
      <div className="status-display">
        <div className="status-label">Статус:</div>
        <div className="status-value" style={{ color: statusColor }}>
          {statusText}
        </div>
      </div>
    );
  }, [broadcast.status]);

  // Рендер информации о трансляции
  const renderBroadcastInfo = useCallback(() => {
    return (
      <div className="broadcast-info">
        <div className="info-row">
          <span className="info-label">ID:</span>
          <span className="info-value">{broadcast.id}</span>
        </div>
        <div className="info-row">
          <span className="info-label">Тип:</span>
          <span className="info-value">{broadcast.streamType === 'video' ? 'Видео' : 'Аудио'}</span>
        </div>
        <div className="info-row">
          <span className="info-label">Зрители:</span>
          <span className="info-value">{broadcast.viewerCount} / {broadcast.maxViewers}</span>
        </div>
        {broadcast.startedAt && (
          <div className="info-row">
            <span className="info-label">Длительность:</span>
            <span className="info-value">
              {formatDuration((Date.now() - new Date(broadcast.startedAt).getTime()) / 1000)}
            </span>
          </div>
        )}
        <div className="info-row">
          <span className="info-label">Создана:</span>
          <span className="info-value">
            {new Date(broadcast.createdAt).toLocaleString('ru-RU')}
          </span>
        </div>
      </div>
    );
  }, [broadcast]);

  // Рендер управления статусом
  const renderStatusControls = useCallback(() => {
    if (!canControl) return null;

    const statusActions: Record<BroadcastStatus, { next: BroadcastStatus[]; label: string }> = {
      'idle': { next: ['preparing'], label: 'Подготовить' },
      'preparing': { next: ['live', 'idle'], label: 'Начать / Отменить' },
      'live': { next: ['paused', 'ended'], label: 'Пауза / Завершить' },
      'paused': { next: ['live', 'ended'], label: 'Продолжить / Завершить' },
      'ended': { next: [], label: 'Завершена' },
      'failed': { next: ['preparing'], label: 'Повторить' }
    };

    const actions = statusActions[broadcast.status];

    return (
      <div className="status-controls">
        <div className="controls-label">Управление статусом:</div>
        <div className="controls-buttons">
          {actions.next.map(status => (
            <button
              key={status}
              className={`status-button ${status}`}
              onClick={() => handleStatusChange(status)}
              disabled={isLoading}
            >
              {status === 'preparing' && 'Подготовить'}
              {status === 'live' && 'Начать трансляцию'}
              {status === 'paused' && 'Пауза'}
              {status === 'ended' && 'Завершить'}
              {status === 'idle' && 'Отменить'}
            </button>
          ))}
        </div>
      </div>
    );
  }, [broadcast.status, canControl, isLoading, handleStatusChange]);

  // Рендер редактирования
  const renderEditForm = useCallback(() => {
    if (!isEditing) return null;

    return (
      <div className="edit-form">
        <div className="form-group">
          <label>Название:</label>
          <input
            type="text"
            value={editData.title}
            onChange={(e) => setEditData({ ...editData, title: e.target.value })}
            maxLength={100}
          />
        </div>

        <div className="form-group">
          <label>Описание:</label>
          <textarea
            value={editData.description}
            onChange={(e) => setEditData({ ...editData, description: e.target.value })}
            maxLength={500}
            rows={3}
          />
        </div>

        <div className="form-group checkboxes">
          <label>
            <input
              type="checkbox"
              checked={editData.isPublic}
              onChange={(e) => setEditData({ ...editData, isPublic: e.target.checked })}
            />
            Публичная трансляция
          </label>

          <label>
            <input
              type="checkbox"
              checked={editData.isChatEnabled}
              onChange={(e) => setEditData({ ...editData, isChatEnabled: e.target.checked })}
            />
            Включить чат
          </label>

          <label>
            <input
              type="checkbox"
              checked={editData.isReactionsEnabled}
              onChange={(e) => setEditData({ ...editData, isReactionsEnabled: e.target.checked })}
            />
            Включить реакции
          </label>

          <label>
            <input
              type="checkbox"
              checked={editData.isRecording}
              onChange={(e) => setEditData({ ...editData, isRecording: e.target.checked })}
            />
            Записывать трансляцию
          </label>
        </div>

        <div className="form-actions">
          <button
            className="save-button"
            onClick={handleUpdateBroadcast}
            disabled={isLoading}
          >
            {isLoading ? 'Сохранение...' : 'Сохранить'}
          </button>
          <button
            className="cancel-button"
            onClick={() => setIsEditing(false)}
            disabled={isLoading}
          >
            Отмена
          </button>
        </div>
      </div>
    );
  }, [isEditing, editData, isLoading, handleUpdateBroadcast]);

  // Рендер управления качеством
  const renderQualityControls = useCallback(() => {
    if (!showAdvanced || !canControl) return null;

    const qualities: StreamQuality[] = ['1080p', '720p', '480p', '360p', 'auto'];
    const currentResolution = getResolutionForQuality(selectedQuality);

    return (
      <div className="quality-controls">
        <div className="controls-label">Управление качеством:</div>
        <div className="quality-buttons">
          {qualities.map(quality => (
            <button
              key={quality}
              className={`quality-button ${quality === selectedQuality ? 'active' : ''}`}
              onClick={() => handleQualityChange(quality)}
              disabled={isLoading}
            >
              {quality === 'auto' ? 'Авто' : quality}
            </button>
          ))}
        </div>
        {currentResolution && (
          <div className="quality-info">
            Разрешение: {currentResolution.width}×{currentResolution.height}
          </div>
        )}
      </div>
    );
  }, [showAdvanced, canControl, selectedQuality, isLoading, handleQualityChange]);

  // Рендер статистики
  const renderStats = useCallback(() => {
    if (!showStats || !streamStats) return null;

    return (
      <div className="stats-panel">
        <div className="stats-header">
          <h4>Статистика потока</h4>
          <button className="close-button" onClick={() => setShowStats(false)}>
            ✕
          </button>
        </div>
        <div className="stats-content">
          <div className="stat-row">
            <span className="stat-label">Битрейт:</span>
            <span className="stat-value">{formatBitrate(streamStats.bitrate)}</span>
          </div>
          <div className="stat-row">
            <span className="stat-label">Буфер:</span>
            <span className="stat-value">{streamStats.bufferLength.toFixed(1)}с</span>
          </div>
          <div className="stat-row">
            <span className="stat-label">Задержка:</span>
            <span className="stat-value">{streamStats.latency.toFixed(1)}с</span>
          </div>
          <div className="stat-row">
            <span className="stat-label">Потерянные кадры:</span>
            <span className="stat-value">{streamStats.droppedFrames}</span>
          </div>
          <div className="stat-row">
            <span className="stat-label">Сетевая активность:</span>
            <span className="stat-value">
              {streamStats.networkActivity ? '⚠️ Проблемы' : '✅ Норма'}
            </span>
          </div>
        </div>
      </div>
    );
  }, [showStats, streamStats]);

  // Ренде�� тестовых функций
  const renderTestFunctions = useCallback(() => {
    if (!showAdvanced || !canControl) return null;

    return (
      <div className="test-functions">
        <div className="controls-label">Тестовые функции:</div>
        <div className="test-buttons">
          <button
            className="test-button"
            onClick={handleGetStats}
          >
            📊 Получить статистику
          </button>
          <button
            className="test-button"
            onClick={() => handleSimulateNetworkIssue('low')}
          >
            🌐 Имитация проблем сети (низкие)
          </button>
          <button
            className="test-button"
            onClick={() => handleSimulateNetworkIssue('medium')}
          >
            🌐 Имитация проблем сети (средние)
          </button>
          <button
            className="test-button"
            onClick={() => handleSimulateNetworkIssue('high')}
          >
            🌐 Имитация проблем сети (высокие)
          </button>
          <button
            className="test-button danger"
            onClick={handleResetAll}
          >
            🔄 Сбросить все состояния
          </button>
        </div>
      </div>
    );
  }, [showAdvanced, canControl, handleGetStats, handleSimulateNetworkIssue, handleResetAll]);

  if (!canControl) {
    return (
      <div className="broadcast-controls no-access">
        <div className="access-denied">
          ⚠️ У вас нет прав для управления этой трансляцией
        </div>
        {renderStatus()}
        {renderBroadcastInfo()}
      </div>
    );
  }

  return (
    <div className="broadcast-controls">
      {/* Заголовок */}
      <div className="controls-header">
        <h3>Управление трансляцией</h3>
        <div className="header-actions">
          {!isEditing && (
            <button
              className="edit-button"
              onClick={() => setIsEditing(true)}
            >
              ✏️ Редактировать
            </button>
          )}
          <button
            className="refresh-button"
            onClick={() => window.location.reload()}
          >
            🔄 Обновить
          </button>
        </div>
      </div>

      {/* Основная информация */}
      <div className="controls-section">
        {renderStatus()}
        {renderBroadcastInfo()}
      </div>

      {/* Управление статусом */}
      <div className="controls-section">
        {renderStatusControls()}
      </div>

      {/* Редактирование */}
      <div className="controls-section">
        {renderEditForm()}
      </div>

      {/* Управление качеством */}
      {showAdvanced && (
        <div className="controls-section">
          {renderQualityControls()}
        </div>
      )}

      {/* Тестовые функции */}
      {showAdvanced && (
        <div className="controls-section">
          {renderTestFunctions()}
        </div>
      )}

      {/* Статистика */}
      {renderStats()}

      {/* Быстрые действия */}
      <div className="quick-actions">
        <button
          className="quick-action start"
          onClick={handleStartBroadcast}
          disabled={broadcast.status !== 'preparing' || isLoading}
        >
          ▶️ Начать трансляцию
        </button>
        <button
          className="quick-action stop"
          onClick={handleStopBroadcast}
          disabled={broadcast.status !== 'live' || isLoading}
        >
          ⏹️ Остановить трансляцию
        </button>
        <button
          className="quick-action stats"
          onClick={handleGetStats}
        >
          📊 Статистика
        </button>
      </div>
    </div>
  );
};

// Стили компонента
const styles = `
.broadcast-controls {
  background: #1a1a1a;
  border-radius: 8px;
  padding: 20px;
  color: white;
}

.broadcast-controls.no-access {
  opacity: 0.7;
}

.access-denied {
  background: #ff9800;
  color: white;
  padding: 12px;
  border-radius: 4px;
  text-align: center;
  margin-bottom: 20px;
  font-weight: bold;
}

.controls-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #333;
}

.controls-header h3 {
  margin: 0;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.edit-button, .refresh-button {
  background: #333;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.edit-button:hover, .refresh-button:hover {
  background: #444;
}

.controls-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #2a2a2a;
  border-radius: 6px;
}

.status-display {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.status-label {
  font-weight: bold;
  color: #888;
}

.status-value {
  font-weight: bold;
  font-size: 16px;
}

.broadcast-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.info-label {
  color: #888;
}

.info-value {
  color: white;
  font-weight: 500;
}

.status-controls, .quality-controls, .test-functions {
  margin-top: 15px;
}

.controls-label {
  font-weight: bold;
  color: #888;
  margin-bottom: 10px;
  font-size: 14px;
}

.controls-buttons, .quality-buttons, .test-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-button, .quality-button, .test-button {
  background: #333;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.status-button:hover:not(:disabled),
.quality-button:hover:not(:disabled),
.test-button:hover:not(:disabled) {
  background: #444;
}

.status-button:disabled,
.quality-button:disabled,
.test-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-button.live {
  background: #4CAF50;
}

.status-button.paused {
  background: #ff9800;
}

.status-button.ended {
  background: #f44336;
}

.status-button.preparing {
  background: #2196F3;
}

.quality-button.active {
  background: #4CAF50;
}

.test-button.danger {
  background: #f44336;
}

.test-button.danger:hover {
  background: #d32f2f;
}

.quality-info {
  margin-top: 10px;
  font-size: 12px;
  color: #888;
  text-align: center;
}

.edit-form {
  background: #333;
  padding: 15px;
  border-radius: 6px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #888;
  font-size: 14px;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  background: #1a1a1a;
  border: 1px solid #444;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.form-group input[type="text"]:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.form-group.checkboxes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-group.checkboxes label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
}

.form-group.checkboxes input[type="checkbox"] {
  width: 16px;
  height: 16px;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.save-button, .cancel-button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.save-button {
  background: #4CAF50;
  color: white;
}

.save-button:hover:not(:disabled) {
  background: #45a049;
}

.save-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-button {
  background: #666;
  color: white;
}

.cancel-button:hover:not(:disabled) {
  background: #777;
}

.stats-panel {
  background: #2a2a2a;
  border: 1px solid #333;
  border-radius: 6px;
  margin-top: 20px;
  overflow: hidden;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #333;
  border-bottom: 1px solid #444;
}

.stats-header h4 {
  margin: 0;
  font-size: 16px;
}

.close-button {
  background: none;
  border: none;
  color: #888;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: white;
}

.stats-content {
  padding: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}

.stat-label {
  color: #888;
}

.stat-value {
  color: white;
  font-weight: 500;
}

.quick-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #333;
}

.quick-action {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: bold;
}

.quick-action.start {
  background: #4CAF50;
  color: white;
}

.quick-action.start:hover:not(:disabled) {
  background: #45a049;
}

.quick-action.stop {
  background: #f44336;
  color: white;
}

.quick-action.stop:hover:not(:disabled) {
  background: #d32f2f;
}

.quick-action.stats {
  background: #2196F3;
  color: white;
}

.quick-action.stats:hover:not(:disabled) {
  background: #1976D2;
}

.quick-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
`;

// Добавляем стили в документ
if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = styles;
  document.head.appendChild(styleElement);
}

export default BroadcastControls;