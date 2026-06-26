/**
 * Утилита для просмотра логов чата в консоли разработчика
 * Использование: в консоли браузера вызовите window.chatLogViewer.showStats()
 */

import chatLogger from './chatLogger';

class ChatLogViewer {
    constructor() {
        // Экспортируем методы в глобальную область видимости
        if (typeof window !== 'undefined') {
            window.chatLogViewer = this;
        }
    }

    /**
     * Показать статистику логов
     */
    showStats() {
        const stats = chatLogger.getStats();



        Object.entries(stats.byContentType).forEach(([type, count]) => {
        });

        Object.entries(stats.byChatType).forEach(([type, count]) => {
        });

        Object.entries(stats.byUserRole).forEach(([role, count]) => {
        });

    }

    /**
     * Показать последние N записей
     */
    showRecentLogs(count = 10) {
        const logs = chatLogger.getLogs();
        const recentLogs = logs.slice(0, count);


        if (recentLogs.length === 0) {
            return;
        }

        recentLogs.forEach((log, index) => {
        });

    }

    /**
     * Показать все логи
     */
    showAllLogs() {
        const logs = chatLogger.getLogs();


        if (logs.length === 0) {
            return;
        }

        logs.forEach((log, index) => {
        });

    }

    /**
     * Экспортировать логи в файл
     */
    exportLogs() {
        chatLogger.exportLogs();
    }

    /**
     * Очистить логи
     */
    clearLogs() {
        if (confirm('Очистить все логи? Это действие нельзя отменить.')) {
            chatLogger.clearLogs();
        }
    }

    /**
     * Включить/выключить логирование
     */
    toggleLogging() {
        const enabled = !chatLogger.isEnabled();
        chatLogger.setEnabled(enabled);
        //                   `color: ${enabled ? '#28a745' : '#dc3545'}; font-weight: bold;`);
    }

    /**
     * Форматирование записи лога
     */
    formatLogEntry(log) {
        const date = new Date(log.timestamp);
        const timeStr = date.toLocaleTimeString();
        const dateStr = date.toLocaleDateString();

        let details = '';

        switch (log.contentType) {
            case 'text':
                details = `Текст: "${log.details.textPreview}" (${log.details.textLength} симв.)`;
                break;
            case 'file':
                details = `Файл: ${log.details.fileName} (${this.formatFileSize(log.details.fileSize)})`;
                break;
            case 'voice':
                details = `Голосовое: ${log.details.durationFormatted}`;
                break;
            case 'edit':
                details = `Редактирование: ID ${log.details.messageId}`;
                break;
            case 'delete':
                details = `Удаление: ID ${log.details.messageId}`;
                break;
        }

        return `${this.getContentTypeIcon(log.contentType)} [${dateStr} ${timeStr}] ${log.chatType} → ${log.room} → ${log.userRole} → ${details}`;
    }

    /**
     * Форматирование размера файла
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Б';

        const k = 1024;
        const sizes = ['Б', 'КБ', 'МБ', 'ГБ'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    /**
     * Иконки для типов контента
     */
    getContentTypeIcon(type) {
        const icons = {
            'text': '📝',
            'file': '📎',
            'voice': '🎤',
            'edit': '✏️',
            'delete': '🗑️'
        };
        return icons[type] || '📄';
    }

    /**
     * Иконки для типов чата
     */
    getChatTypeIcon(type) {
        const icons = {
            'GeneralChat': '💬',
            'NutritionChat': '🥗',
            'UnknownChat': '❓'
        };
        return icons[type] || '💬';
    }

    /**
     * Иконки для ролей пользователей
     */
    getUserRoleIcon(role) {
        const icons = {
            'SUPER_ADMIN': '👑',
            'MODERATOR_ADMIN': '🛡️',
            'DEVELOPER': '👨‍💻',
            'VYACHESLAV': '🌟',
            'USER_0': '👤1',
            'USER_1': '👤2',
            'UNKNOWN': '❓'
        };
        return icons[role] || '👤';
    }

    /**
     * Показать справку по командам
     */
    showHelp() {
    }
}

// Создаем и экспортируем экземпляр
const chatLogViewer = new ChatLogViewer();
export default chatLogViewer;