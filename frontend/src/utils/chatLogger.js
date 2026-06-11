/**
 * Логгер для отслеживания добавления контента в чат
 * Временное решение для сбора данных перед интеграцией с бэкендом
 */

class ChatLogger {
    constructor() {
        this.logKey = 'chat_content_logs';
        this.maxLogs = 1000; // Максимальное количество записей
        this.enabled = true; // Можно отключить через sessionStorage
    }

    /**
     * Логирование добавления контента в чат
     * @param {Object} data - Данные для логирования
     */
    logContentAddition(data) {
        if (!this.enabled) return;

        try {
            const logEntry = {
                timestamp: new Date().toISOString(),
                ...data
            };

            // Получаем текущие логи
            const existingLogs = this.getLogs();

            // Добавляем новую запись в начало
            const updatedLogs = [logEntry, ...existingLogs];

            // Ограничиваем количество записей
            if (updatedLogs.length > this.maxLogs) {
                updatedLogs.length = this.maxLogs;
            }

            // Сохраняем в sessionStorage (только в development)
            if (process.env.NODE_ENV === 'development') {
                sessionStorage.setItem(this.logKey, JSON.stringify(updatedLogs));
            }

            // Также пишем в консоль для отладки
            // console.log('[ChatLogger] Content added:', logEntry);

        } catch (error) {
            console.error('[ChatLogger] Error logging content:', error);
        }
    }

    /**
     * Логирование текстового сообщения
     */
    logTextMessage(chatType, room, userRole, text) {
        this.logContentAddition({
            chatType,
            room,
            contentType: 'text',
            userRole,
            details: {
                textLength: text.length,
                textPreview: text.length > 50 ? text.substring(0, 50) + '...' : text
            }
        });
    }

    /**
     * Логирование файла
     */
    logFileUpload(chatType, room, userRole, file) {
        this.logContentAddition({
            chatType,
            room,
            contentType: 'file',
            userRole,
            details: {
                fileName: file.name,
                fileType: file.type,
                fileSize: file.size,
                mimeType: file.type
            }
        });
    }

    /**
     * Логирование голосового сообщения
     */
    logVoiceMessage(chatType, room, userRole, duration) {
        this.logContentAddition({
            chatType,
            room,
            contentType: 'voice',
            userRole,
            details: {
                durationSeconds: duration,
                durationFormatted: `${Math.floor(duration / 60)}:${String(duration % 60).padStart(2, '0')}`
            }
        });
    }

    /**
     * Логирование редактирования сообщения
     */
    logMessageEdit(chatType, room, userRole, messageId, oldText, newText) {
        this.logContentAddition({
            chatType,
            room,
            contentType: 'edit',
            userRole,
            details: {
                messageId,
                oldTextLength: oldText?.length || 0,
                newTextLength: newText.length,
                textChanged: oldText !== newText
            }
        });
    }

    /**
     * Логирование удаления сообщения
     */
    logMessageDelete(chatType, room, userRole, messageId) {
        this.logContentAddition({
            chatType,
            room,
            contentType: 'delete',
            userRole,
            details: {
                messageId
            }
        });
    }

    /**
     * Получение всех логов
     */
    getLogs() {
        try {
            const logs = process.env.NODE_ENV === 'development'
                ? sessionStorage.getItem(this.logKey)
                : null;
            return logs ? JSON.parse(logs) : [];
        } catch (error) {
            console.error('[ChatLogger] Error reading logs:', error);
            return [];
        }
    }

    /**
     * Очистка логов
     */
    clearLogs() {
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.removeItem(this.logKey);
        }
        // console.log('[ChatLogger] Logs cleared');
    }

    /**
     * Экспорт логов в файл
     */
    exportLogs() {
        const logs = this.getLogs();
        const dataStr = JSON.stringify(logs, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });

        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `chat_logs_${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    /**
     * Включение/выключение логирования
     */
    setEnabled(enabled) {
        this.enabled = enabled;
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('chat_logger_enabled', enabled.toString());
        }
        // console.log(`[ChatLogger] Logging ${enabled ? 'enabled' : 'disabled'}`);
    }

    /**
     * Проверка статуса логирования
     */
    isEnabled() {
        const saved = process.env.NODE_ENV === 'development'
            ? sessionStorage.getItem('chat_logger_enabled')
            : null;
        return saved ? saved === 'true' : this.enabled;
    }

    /**
     * Получение статистики
     */
    getStats() {
        const logs = this.getLogs();
        const stats = {
            total: logs.length,
            byContentType: {},
            byChatType: {},
            byUserRole: {},
            last24Hours: 0
        };

        const now = new Date();
        const twentyFourHoursAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

        logs.forEach(log => {
            // По типу контента
            stats.byContentType[log.contentType] = (stats.byContentType[log.contentType] || 0) + 1;

            // По типу чата
            stats.byChatType[log.chatType] = (stats.byChatType[log.chatType] || 0) + 1;

            // По роли пользователя
            stats.byUserRole[log.userRole] = (stats.byUserRole[log.userRole] || 0) + 1;

            // За последние 24 часа
            const logDate = new Date(log.timestamp);
            if (logDate > twentyFourHoursAgo) {
                stats.last24Hours++;
            }
        });

        return stats;
    }
}

// Создаем глобальный экземпляр логгера
const chatLogger = new ChatLogger();

// Экспортируем для использования
export default chatLogger;