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

        // console.log('%c📊 СТАТИСТИКА ДОБАВЛЕНИЯ КОНТЕНТА В ЧАТ', 'font-size: 16px; font-weight: bold; color: #9333ea;');
        // console.log('='.repeat(50));

        // console.log(`📈 Всего записей: ${stats.total}`);
        // console.log(`⏰ За последние 24 часа: ${stats.last24Hours}`);

        // console.log('\n📋 По типу контента:');
        Object.entries(stats.byContentType).forEach(([type, count]) => {
            // console.log(`  ${this.getContentTypeIcon(type)} ${type}: ${count}`);
        });

        // console.log('\n💬 По типу чата:');
        Object.entries(stats.byChatType).forEach(([type, count]) => {
            // console.log(`  ${this.getChatTypeIcon(type)} ${type}: ${count}`);
        });

        // console.log('\n👤 По роли пользователя:');
        Object.entries(stats.byUserRole).forEach(([role, count]) => {
            // console.log(`  ${this.getUserRoleIcon(role)} ${role}: ${count}`);
        });

        // console.log('='.repeat(50));
    }

    /**
     * Показать последние N записей
     */
    showRecentLogs(count = 10) {
        const logs = chatLogger.getLogs();
        const recentLogs = logs.slice(0, count);

        // console.log('%c📝 ПОСЛЕДНИЕ ЗАПИСИ ЛОГА', 'font-size: 16px; font-weight: bold; color: #9333ea;');
        // console.log('='.repeat(50));

        if (recentLogs.length === 0) {
            // console.log('📭 Логи отсутствуют');
            return;
        }

        recentLogs.forEach((log, index) => {
            // console.log(`\n${index + 1}. ${this.formatLogEntry(log)}`);
        });

        // console.log('='.repeat(50));
    }

    /**
     * Показать все логи
     */
    showAllLogs() {
        const logs = chatLogger.getLogs();

        // console.log('%c📚 ВСЕ ЗАПИСИ ЛОГА', 'font-size: 16px; font-weight: bold; color: #9333ea;');
        // console.log(`Всего записей: ${logs.length}`);
        // console.log('='.repeat(50));

        if (logs.length === 0) {
            // console.log('📭 Логи отсутствуют');
            return;
        }

        logs.forEach((log, index) => {
            // console.log(`\n${index + 1}. ${this.formatLogEntry(log)}`);
        });

        // console.log('='.repeat(50));
    }

    /**
     * Экспортировать логи в файл
     */
    exportLogs() {
        chatLogger.exportLogs();
        // console.log('%c💾 Логи экспортированы в файл', 'color: #28a745; font-weight: bold;');
    }

    /**
     * Очистить логи
     */
    clearLogs() {
        if (confirm('Очистить все логи? Это действие нельзя отменить.')) {
            chatLogger.clearLogs();
            // console.log('%c🗑️ Логи очищены', 'color: #dc3545; font-weight: bold;');
        }
    }

    /**
     * Включить/выключить логирование
     */
    toggleLogging() {
        const enabled = !chatLogger.isEnabled();
        chatLogger.setEnabled(enabled);
        // console.log(`%c${enabled ? '✅' : '❌'} Логирование ${enabled ? 'включено' : 'выключено'}`,
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
        // console.log('%c🆘 СПРАВКА ПО КОМАНДАМ ЛОГГЕРА', 'font-size: 16px; font-weight: bold; color: #9333ea;');
        // console.log('='.repeat(50));
        // console.log('📊 chatLogViewer.showStats()      - Показать статистику');
        // console.log('📝 chatLogViewer.showRecentLogs() - Показать последние 10 записей');
        // console.log('📚 chatLogViewer.showAllLogs()    - Показать все записи');
        // console.log('💾 chatLogViewer.exportLogs()     - Экспортировать логи в файл');
        // console.log('🗑️ chatLogViewer.clearLogs()      - Очистить все логи');
        // console.log('🔧 chatLogViewer.toggleLogging()  - Включить/выключить логирование');
        // console.log('🆘 chatLogViewer.showHelp()       - Показать эту справку');
        // console.log('='.repeat(50));
    }
}

// Создаем и экспортируем экземпляр
const chatLogViewer = new ChatLogViewer();
export default chatLogViewer;