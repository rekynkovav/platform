import { useState, useEffect, useCallback, useRef } from 'react';
import { chatApi } from '../../../services/api';
import { getUsername } from '../../../keycloak';
import useChatWs from './useChatWs';

/**
 * useChatRoom — single-room chat state manager.
 *
 * Manages messages, loading, error, and send/edit/delete actions.
 * WebSocket handles incoming real-time messages.
 *
 * @param {string} slug — backend room slug (e.g. 'user1', 'nutrition')
 * @returns {Object} chat state + actions
 */
const useChatRoom = (slug) => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const messageIdsRef = useRef(new Set());
  const username = getUsername();

  const handleWsMessage = useCallback((data) => {
    const msg = data;
    if (msg.id && !messageIdsRef.current.has(msg.id)) {
      messageIdsRef.current.add(msg.id);
      setMessages((prev) => [...prev, msg]);
    } else if (!msg.id) {
      setMessages((prev) => [...prev, msg]);
    }
  }, []);

  const { isConnected, send } = useChatWs(slug, handleWsMessage);

  const loadMessages = useCallback(async () => {
    try {
      setLoading(true);
      const response = await chatApi.getMessages(slug, 0, 50);
      const data = response.data || [];
      messageIdsRef.current = new Set(data.map((m) => m.id).filter(Boolean));
      setMessages(data);
    } catch (err) {
      setError('Не удалось загрузить сообщения');
    } finally {
      setLoading(false);
    }
  }, [slug]);

  useEffect(() => {
    loadMessages();
  }, [loadMessages]);

  const sendMessage = useCallback(
    async (content, mediaId = null) => {
      const text = typeof content === 'string' ? content : '';
      if (!text.trim() && !mediaId) return;

      if (!isConnected) {
        setError('Отключён от сервера. Обновите страницу.');
        return;
      }

      const tempId = `temp_${Date.now()}`;
      const tempMsg = {
        id: tempId,
        author: username,
        content: text,
        mediaId,
        createdAt: new Date().toISOString(),
        pending: true,
      };
      setMessages((prev) => [...prev, tempMsg]);

      try {
        const response = await chatApi.sendMessage(slug, {
          content: text,
          mediaId,
        });
        const realId = response.id || response.data?.id;
        if (realId) messageIdsRef.current.add(realId);

        setMessages((prev) =>
          prev.map((m) =>
            m.id === tempId
              ? { ...response, ...response.data, id: realId || m.id, pending: false }
              : m,
          ),
        );

        // Broadcast via WS for other clients
        send({
          id: realId,
          content: text,
          author: username,
          createdAt: new Date().toISOString(),
        });
      } catch (err) {
        setMessages((prev) => prev.filter((m) => m.id !== tempId));
        setError('Не удалось отправить сообщение');
      }
    },
    [slug, isConnected, username, send],
  );

  const editMessage = useCallback(
    async (msgId, newContent) => {
      if (!newContent?.trim()) return;
      try {
        await chatApi.editMessage(slug, msgId, { content: newContent.trim() });
        setMessages((prev) =>
          prev.map((m) =>
            m.id === msgId ? { ...m, content: newContent.trim(), edited: true } : m,
          ),
        );
      } catch (err) {
        setError('Не удалось отредактировать сообщение');
      }
    },
    [slug],
  );

  const deleteMessage = useCallback(
    async (msgId) => {
      if (!window.confirm('Удалить это сообщение?')) return false;
      try {
        await chatApi.deleteMessage(slug, msgId);
        setMessages((prev) => prev.filter((m) => m.id !== msgId));
        return true;
      } catch (err) {
        setError('Не удалось удалить сообщение');
        return false;
      }
    },
    [slug],
  );

  const clearError = useCallback(() => setError(null), []);

  return {
    messages,
    loading,
    error,
    currentSlug: slug,
    isConnected,
    username,
    sendMessage,
    editMessage,
    deleteMessage,
    clearError,
  };
};

export default useChatRoom;
