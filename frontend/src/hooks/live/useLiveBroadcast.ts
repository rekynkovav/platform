// Хук для управления live трансляцией
// Предоставляет удобный интерфейс для работы с трансляцией

import { useState, useEffect, useCallback } from 'react';
import { LiveBroadcast, BroadcastStatus } from '../../services/live/types';
import { liveApi } from '../../services/live';
import { useLiveSession } from '../../context/live/LiveSessionContext';

export function useLiveBroadcast(broadcastId?: string) {
  const [broadcast, setBroadcast] = useState<LiveBroadcast | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    state: liveSession,
    connectToBroadcast,
    disconnectFromBroadcast,
    updateBroadcastStatus
  } = useLiveSession();

  // Загрузка трансляции
  const loadBroadcast = useCallback(async (id: string) => {
    if (!id) return;

    setLoading(true);
    setError(null);

    try {
      const data = await liveApi.getBroadcast(id);
      setBroadcast(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки трансляции');
    } finally {
      setLoading(false);
    }
  }, []);

  // Подключение к трансляции
  const connect = useCallback(async () => {
    if (!broadcastId) return;

    try {
      await connectToBroadcast(broadcastId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка подключения');
    }
  }, [broadcastId, connectToBroadcast]);

  // Отключение от трансляции
  const disconnect = useCallback(() => {
    disconnectFromBroadcast();
    setBroadcast(null);
  }, [disconnectFromBroadcast]);

  // Обновление статуса
  const updateStatus = useCallback(async (status: BroadcastStatus) => {
    if (!broadcastId) return;

    try {
      await updateBroadcastStatus(status);
      // Обновляем локальное состояние
      setBroadcast(prev => prev ? { ...prev, status } : null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления статуса');
    }
  }, [broadcastId, updateBroadcastStatus]);

  // Начало трансляции
  const startBroadcast = useCallback(async (title: string, description?: string) => {
    try {
      const response = await liveApi.startBroadcast({
        title,
        description,
        streamType: 'video',
        settings: {
          isChatEnabled: true,
          isReactionsEnabled: true,
          isRecording: true,
          isPublic: true
        }
      });

      setBroadcast(response.broadcast);
      return response.broadcast;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка начала трансляции');
      throw err;
    }
  }, []);

  // Остановка трансляции
  const stopBroadcast = useCallback(async () => {
    if (!broadcastId) return;

    try {
      await liveApi.stopBroadcast(broadcastId);
      await updateStatus('ended');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка остановки трансляции');
    }
  }, [broadcastId, updateStatus]);

  // Обновление информации о трансляции
  const updateBroadcast = useCallback(async (updates: Partial<LiveBroadcast>) => {
    if (!broadcastId) return;

    try {
      const updated = await liveApi.updateBroadcast(broadcastId, updates);
      setBroadcast(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления трансляции');
    }
  }, [broadcastId]);

  // Синхронизация с live сессией
  useEffect(() => {
    if (liveSession.broadcast && liveSession.broadcast.id === broadcastId) {
      setBroadcast(liveSession.broadcast);
    }
  }, [liveSession.broadcast, broadcastId]);

  // Автозагрузка при изменении broadcastId
  useEffect(() => {
    if (broadcastId) {
      loadBroadcast(broadcastId);
    } else {
      setBroadcast(null);
    }
  }, [broadcastId, loadBroadcast]);

  // Автоотключение при размонтировании
  useEffect(() => {
    return () => {
      if (broadcastId && liveSession.broadcast?.id === broadcastId) {
        disconnect();
      }
    };
  }, [broadcastId, liveSession.broadcast?.id, disconnect]);

  return {
    // Данные
    broadcast,
    loading,
    error,

    // Состояние из сессии
    isConnected: liveSession.wsConnected,
    isReconnecting: liveSession.wsReconnecting,
    isAdmin: liveSession.isAdmin,
    isViewer: liveSession.isViewer,
    isModerator: liveSession.isModerator,

    // Действия
    loadBroadcast,
    connect,
    disconnect,
    updateStatus,
    startBroadcast,
    stopBroadcast,
    updateBroadcast,

    // Утилиты
    isLive: broadcast?.status === 'live',
    isEnded: broadcast?.status === 'ended',
    isPreparing: broadcast?.status === 'preparing',
    canJoin: broadcast?.status === 'live' || broadcast?.status === 'preparing',
    canControl: liveSession.isAdmin || liveSession.isModerator,

    // Сброс ошибки
    clearError: () => setError(null)
  };
}

// Хук для списка трансляций
export function useLiveBroadcasts(filter?: {
  status?: BroadcastStatus | BroadcastStatus[];
  createdBy?: string;
  limit?: number;
}) {
  const [broadcasts, setBroadcasts] = useState<LiveBroadcast[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadBroadcasts = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const allBroadcasts = await liveApi.getAllBroadcasts();

      // Применяем фильтры
      let filtered = allBroadcasts;

      if (filter?.status) {
        const statuses = Array.isArray(filter.status) ? filter.status : [filter.status];
        filtered = filtered.filter(b => statuses.includes(b.status));
      }

      if (filter?.createdBy) {
        filtered = filtered.filter(b => b.createdBy === filter.createdBy);
      }

      if (filter?.limit) {
        filtered = filtered.slice(0, filter.limit);
      }

      setBroadcasts(filtered);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки трансляций');
    } finally {
      setLoading(false);
    }
  }, [filter]);

  // Загрузка активных трансляций
  const loadActiveBroadcasts = useCallback(async () => {
    try {
      const active = await liveApi.getActiveBroadcasts();
      setBroadcasts(active);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки активных трансляций');
    }
  }, []);

  // Обновление трансляции в списке
  const updateBroadcastInList = useCallback((updatedBroadcast: LiveBroadcast) => {
    setBroadcasts(prev => prev.map(b =>
      b.id === updatedBroadcast.id ? updatedBroadcast : b
    ));
  }, []);

  // Удаление трансляции из списка
  const removeBroadcastFromList = useCallback((broadcastId: string) => {
    setBroadcasts(prev => prev.filter(b => b.id !== broadcastId));
  }, []);

  // Автозагрузка при изменении фильтров
  useEffect(() => {
    loadBroadcasts();
  }, [loadBroadcasts]);

  return {
    broadcasts,
    loading,
    error,
    loadBroadcasts,
    loadActiveBroadcasts,
    updateBroadcastInList,
    removeBroadcastFromList,
    clearError: () => setError(null)
  };
}

// Хук для статистики трансляции
export function useBroadcastStats(broadcastId?: string) {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadStats = useCallback(async (id: string) => {
    if (!id) return;

    setLoading(true);
    setError(null);

    try {
      const data = await liveApi.getBroadcastStats(id);
      setStats(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки статистики');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadViewerStats = useCallback(async (id: string) => {
    if (!id) return;

    try {
      const data = await liveApi.getViewerStats(id);
      return data;
    } catch (err) {
      return [];
    }
  }, []);

  // Автозагрузка при изменении broadcastId
  useEffect(() => {
    if (broadcastId) {
      loadStats(broadcastId);
    } else {
      setStats(null);
    }
  }, [broadcastId, loadStats]);

  return {
    stats,
    loading,
    error,
    loadStats,
    loadViewerStats,
    clearError: () => setError(null)
  };
}

// Хук для проверки доступности трансляции
export function useBroadcastAvailability(broadcastId?: string) {
  const [available, setAvailable] = useState(false);
  const [checking, setChecking] = useState(false);

  const checkAvailability = useCallback(async (id: string) => {
    if (!id) return false;

    setChecking(true);

    try {
      // Проверяем существование трансляции
      await liveApi.getBroadcast(id);
      setAvailable(true);
      return true;
    } catch {
      setAvailable(false);
      return false;
    } finally {
      setChecking(false);
    }
  }, []);

  // Автопроверка при изменении broadcastId
  useEffect(() => {
    if (broadcastId) {
      checkAvailability(broadcastId);
    } else {
      setAvailable(false);
    }
  }, [broadcastId, checkAvailability]);

  return {
    available,
    checking,
    checkAvailability
  };
}