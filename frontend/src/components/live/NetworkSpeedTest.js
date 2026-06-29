import React, { useState, useEffect } from 'react';
import './NetworkSpeedTest.css';

/**
 * NetworkSpeedTest — internet connection diagnostic tool for pre-broadcast checks.
 *
 * Measures download speed, upload speed, and ping latency, then determines
 * connection quality and recommends optimal stream settings (resolution, bitrate)
 * based on the measured bandwidth. Reports results via onTestComplete callback.
 *
 * @param {(result: { downloadSpeed: number, uploadSpeed: number, ping: number, quality: string, recommendedSettings: Object }) => void} onTestComplete - Callback with speed test results and recommendations
 * @param {() => void} onCancel - Callback to cancel the speed test
 * @returns {JSX.Element} The speed test interface with progress indicators and results
 */
const NetworkSpeedTest = ({ onTestComplete, onCancel }) => {
    const [testStatus, setTestStatus] = useState('ready'); // ready, testing, complete
    const [downloadSpeed, setDownloadSpeed] = useState(null);
    const [uploadSpeed, setUploadSpeed] = useState(null);
    const [ping, setPing] = useState(null);
    const [connectionQuality, setConnectionQuality] = useState(null);
    const [recommendedSettings, setRecommendedSettings] = useState(null);
    const [error, setError] = useState(null);

    // Функция для тестирования пинга
    const testPing = async () => {
        const startTime = Date.now();
        try {
            // Используем небольшой запрос для тестирования пинга
            await fetch('/api/ping', { method: 'HEAD', cache: 'no-store' });
            return Date.now() - startTime;
        } catch (error) {
            // Если нет эндпоинта /api/ping, используем альтернативный метод
            setError('Не удалось измерить пинг. Используется значение по умолчанию.');
            return 50; // Примерное значение по умолчанию
        }
    };

    // Функция для тестирования скорости загрузки
    const testDownloadSpeed = async () => {
        const testFileUrl = 'https://raw.githubusercontent.com/sivel/speedtest-cli/master/speedtest-cli';
        const fileSizeInBytes = 1024 * 1024; // 1MB тестовый файл
        const startTime = Date.now();

        try {
            const response = await fetch(testFileUrl, { cache: 'no-store' });
            const reader = response.body.getReader();
            let receivedLength = 0;

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                receivedLength += value.length;
                if (receivedLength >= fileSizeInBytes) break;
            }

            const duration = (Date.now() - startTime) / 1000; // в секундах
            const speedMbps = (receivedLength * 8) / (duration * 1000000); // Мбит/с
            return speedMbps;
        } catch (error) {
            setError('Не удалось измерить скорость загрузки. Используется значение по умолчанию.');
            // Используем API navigator.connection как запасной вариант
            if (navigator.connection && navigator.connection.downlink) {
                return navigator.connection.downlink;
            }
            return 10; // Значение по умолчанию
        }
    };

    // Функция для тестирования скорости отдачи
    const testUploadSpeed = async () => {
        const testData = new ArrayBuffer(1024 * 1024); // 1MB тестовых данных
        const startTime = Date.now();

        try {
            // Создаем тестовый эндпоинт или используем существующий
            await fetch('/api/test-upload', {
                method: 'POST',
                body: testData,
                headers: {
                    'Content-Type': 'application/octet-stream'
                }
            });

            const duration = (Date.now() - startTime) / 1000; // в секундах
            const speedMbps = (1024 * 1024 * 8) / (duration * 1000000); // Мбит/с
            return speedMbps;
        } catch (error) {
            setError('Не удалось измерить скорость отдачи. Используется значение по умолчанию.');
            // Используем API navigator.connection как запасной вариант
            if (navigator.connection && navigator.connection.effectiveType) {
                const type = navigator.connection.effectiveType;
                switch (type) {
                    case 'slow-2g': return 0.5;
                    case '2g': return 1;
                    case '3g': return 5;
                    case '4g': return 20;
                    default: return 10;
                }
            }
            return 5; // Значение по умолчанию
        }
    };

    const clearError = () => {
        setError(null);
    };

    // Определение качества соединения
    const determineConnectionQuality = (download, upload, pingValue) => {
        if (download >= 50 && upload >= 10 && pingValue <= 30) {
            return 'Отличное';
        } else if (download >= 20 && upload >= 5 && pingValue <= 60) {
            return 'Хорошее';
        } else if (download >= 10 && upload >= 2 && pingValue <= 100) {
            return 'Среднее';
        } else if (download >= 5 && upload >= 1 && pingValue <= 200) {
            return 'Слабое';
        } else {
            return 'Очень слабое';
        }
    };

    // Рекомендации настроек на основе качества соединения
    const getRecommendedSettings = (quality) => {
        switch (quality) {
            case 'Отличное':
                return {
                    quality: 'high',
                    videoBitrate: 5000,
                    audioBitrate: 192,
                    framerate: 60,
                    resolution: '1080p'
                };
            case 'Хорошее':
                return {
                    quality: 'medium',
                    videoBitrate: 2500,
                    audioBitrate: 128,
                    framerate: 30,
                    resolution: '720p'
                };
            case 'Среднее':
                return {
                    quality: 'low',
                    videoBitrate: 1000,
                    audioBitrate: 96,
                    framerate: 25,
                    resolution: '480p'
                };
            case 'Слабое':
                return {
                    quality: 'very-low',
                    videoBitrate: 500,
                    audioBitrate: 64,
                    framerate: 20,
                    resolution: '360p'
                };
            case 'Очень слабое':
                return {
                    quality: 'audio-only',
                    videoBitrate: 0,
                    audioBitrate: 48,
                    framerate: 0,
                    resolution: 'Аудио'
                };
            default:
                return {
                    quality: 'medium',
                    videoBitrate: 2500,
                    audioBitrate: 128,
                    framerate: 30,
                    resolution: '720p'
                };
        }
    };

    // Запуск теста
    const startTest = async () => {
        setTestStatus('testing');

        try {
            // Тестируем пинг
            setTestStatus('testing_ping');
            const pingValue = await testPing();
            setPing(pingValue);

            // Тестируем скорость загрузки
            setTestStatus('testing_download');
            const downloadValue = await testDownloadSpeed();
            setDownloadSpeed(downloadValue);

            // Тестируем скорость отдачи
            setTestStatus('testing_upload');
            const uploadValue = await testUploadSpeed();
            setUploadSpeed(uploadValue);

            // Определяем качество соединения
            const quality = determineConnectionQuality(downloadValue, uploadValue, pingValue);
            setConnectionQuality(quality);

            // Получаем рекомендуемые настройки
            const settings = getRecommendedSettings(quality);
            setRecommendedSettings(settings);

            setTestStatus('complete');
        } catch (error) {
            setTestStatus('error');
        }
    };

    // Применение рекомендуемых настроек
    const applySettings = () => {
        if (recommendedSettings && onTestComplete) {
            onTestComplete(recommendedSettings);
        }
    };

    // Запускаем тест при монтировании компонента
    useEffect(() => {
        startTest();
    }, []);

    // Отображение статуса тестирования
    const renderTestStatus = () => {
        switch (testStatus) {
            case 'ready':
                return <div className="test-status">Готов к тестированию...</div>;
            case 'testing_ping':
                return <div className="test-status">Тестируем пинг...</div>;
            case 'testing_download':
                return <div className="test-status">Тестируем скорость загрузки...</div>;
            case 'testing_upload':
                return <div className="test-status">Тестируем скорость отдачи...</div>;
            case 'complete':
                return <div className="test-status">Тестирование завершено!</div>;
            case 'error':
                return <div className="test-status error">Ошибка тестирования</div>;
            default:
                return <div className="test-status">Тестирование...</div>;
        }
    };

    return (
        <div className="network-speed-test">
            <div className="test-header">
                <h3>Тест скорости сети</h3>
                <p>Определение оптимальных настроек трансляции</p>
            </div>

            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="test-content">
                {renderTestStatus()}

                {testStatus === 'complete' && (
                    <div className="test-results">
                        <div className="result-item">
                            <span className="result-label">Пинг:</span>
                            <span className="result-value">{ping ? `${ping.toFixed(0)} мс` : '—'}</span>
                        </div>
                        <div className="result-item">
                            <span className="result-label">Скорость загрузки:</span>
                            <span className="result-value">{downloadSpeed ? `${downloadSpeed.toFixed(1)} Мбит/с` : '—'}</span>
                        </div>
                        <div className="result-item">
                            <span className="result-label">Скорость отдачи:</span>
                            <span className="result-value">{uploadSpeed ? `${uploadSpeed.toFixed(1)} Мбит/с` : '—'}</span>
                        </div>
                        <div className="result-item">
                            <span className="result-label">Качество соединения:</span>
                            <span className={`result-value quality-${connectionQuality?.toLowerCase().replace(' ', '-')}`}>
                                {connectionQuality || '—'}
                            </span>
                        </div>

                        {recommendedSettings && (
                            <div className="recommended-settings">
                                <h4>Рекомендуемые настройки:</h4>
                                <div className="settings-grid">
                                    <div className="setting-item">
                                        <span className="setting-label">Качество:</span>
                                        <span className="setting-value">{recommendedSettings.quality === 'high' ? 'Высокое' :
                                                                         recommendedSettings.quality === 'medium' ? 'Среднее' :
                                                                         recommendedSettings.quality === 'low' ? 'Низкое' :
                                                                         recommendedSettings.quality === 'very-low' ? 'Очень низкое' : 'Только аудио'}</span>
                                    </div>
                                    <div className="setting-item">
                                        <span className="setting-label">Разрешение:</span>
                                        <span className="setting-value">{recommendedSettings.resolution}</span>
                                    </div>
                                    <div className="setting-item">
                                        <span className="setting-label">Частота кадров:</span>
                                        <span className="setting-value">{recommendedSettings.framerate} FPS</span>
                                    </div>
                                    <div className="setting-item">
                                        <span className="setting-label">Битрейт видео:</span>
                                        <span className="setting-value">{recommendedSettings.videoBitrate} кбит/с</span>
                                    </div>
                                    <div className="setting-item">
                                        <span className="setting-label">Битрейт аудио:</span>
                                        <span className="setting-value">{recommendedSettings.audioBitrate} кбит/с</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {testStatus === 'error' && (
                    <div className="test-error">
                        <p>Не удалось выполнить тест скорости. Проверьте подключение к интернету.</p>
                        <button className="retry-btn" onClick={startTest}>
                            Повторить тест
                        </button>
                    </div>
                )}
            </div>

            <div className="test-actions">
                <button className="cancel-btn" onClick={onCancel}>
                    Отмена
                </button>
                {testStatus === 'complete' && (
                    <button className="apply-btn" onClick={applySettings}>
                        Применить автонастройки
                    </button>
                )}
            </div>
        </div>
    );
};

export default NetworkSpeedTest;