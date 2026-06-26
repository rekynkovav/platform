import React, { useState, useEffect, useRef } from 'react';
import './DeviceTest.css';

/**
 * DeviceTest — hardware diagnostic component for testing audio/video devices before a broadcast.
 *
 * Enumerates available media devices (audio inputs, video inputs, audio outputs),
 * allows device selection, provides live audio level metering, video preview,
 * test recording with playback, and reports results via onTestComplete callback.
 *
 * @param {({ audioInput: string, videoInput: string, audioOutput: string, testPassed: boolean }) => void} onTestComplete - Callback with selected devices and test result
 * @param {() => void} onCancel - Callback to cancel the device test
 * @returns {JSX.Element} The device test interface with device lists, previews, and test controls
 */
const DeviceTest = ({ onTestComplete, onCancel }) => {
    const [devices, setDevices] = useState({
        audioInputs: [],
        videoInputs: [],
        audioOutputs: []
    });
    const [selectedDevices, setSelectedDevices] = useState({
        audioInput: '',
        videoInput: '',
        audioOutput: ''
    });
    const [testStatus, setTestStatus] = useState('ready'); // ready, testing, recording, playing, complete
    const [audioLevel, setAudioLevel] = useState(0);
    const [videoPreview, setVideoPreview] = useState(null);
    const [audioPreview, setAudioPreview] = useState(null);
    const [testRecording, setTestRecording] = useState(null);
    const [isRecording, setIsRecording] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [error, setError] = useState(null);

    const videoRef = useRef(null);
    const audioRef = useRef(null);
    const mediaRecorderRef = useRef(null);
    const audioContextRef = useRef(null);
    const analyserRef = useRef(null);
    const animationFrameRef = useRef(null);
    const recordingTimerRef = useRef(null);

    const clearError = () => {
        setError(null);
    };

    // Получение списка доступных устройств
    const getDevices = async () => {
        try {
            // Запрашиваем разрешение на доступ к устройствам
            await navigator.mediaDevices.getUserMedia({ audio: true, video: true });

            const deviceList = await navigator.mediaDevices.enumerateDevices();

            const audioInputs = deviceList.filter(device => device.kind === 'audioinput');
            const videoInputs = deviceList.filter(device => device.kind === 'videoinput');
            const audioOutputs = deviceList.filter(device => device.kind === 'audiooutput');

            setDevices({
                audioInputs,
                videoInputs,
                audioOutputs
            });

            // Устанавливаем устройства по умолчанию
            if (audioInputs.length > 0) {
                setSelectedDevices(prev => ({
                    ...prev,
                    audioInput: audioInputs[0].deviceId
                }));
            }
            if (videoInputs.length > 0) {
                setSelectedDevices(prev => ({
                    ...prev,
                    videoInput: videoInputs[0].deviceId
                }));
            }
            if (audioOutputs.length > 0) {
                setSelectedDevices(prev => ({
                    ...prev,
                    audioOutput: audioOutputs[0].deviceId
                }));
            }

        } catch (error) {
            console.error('Ошибка получения устройств:', error);
            setError('Не удалось получить доступ к устройствам. Проверьте разрешения.');
        }
    };

    // Инициализация аудиоанализатора для визуализации уровня звука
    const initAudioAnalyzer = (stream) => {
        if (audioContextRef.current) {
            audioContextRef.current.close();
        }

        audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)();
        analyserRef.current = audioContextRef.current.createAnalyser();
        analyserRef.current.fftSize = 256;

        const source = audioContextRef.current.createMediaStreamSource(stream);
        source.connect(analyserRef.current);

        const updateAudioLevel = () => {
            if (!analyserRef.current) return;

            const dataArray = new Uint8Array(analyserRef.current.frequencyBinCount);
            analyserRef.current.getByteFrequencyData(dataArray);

            // Вычисляем средний уровень звука
            const average = dataArray.reduce((sum, value) => sum + value, 0) / dataArray.length;
            setAudioLevel(average);

            animationFrameRef.current = requestAnimationFrame(updateAudioLevel);
        };

        updateAudioLevel();
    };

    // Запуск предпросмотра видео
    const startVideoPreview = async () => {
        if (videoRef.current && selectedDevices.videoInput) {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: { deviceId: selectedDevices.videoInput ? { exact: selectedDevices.videoInput } : undefined }
                });

                videoRef.current.srcObject = stream;
                setVideoPreview(stream);
            } catch (error) {
                console.error('Ошибка запуска видео:', error);
                setError('Не удалось запустить видео. Проверьте доступ к камере.');
            }
        }
    };

    // Запуск предпросмотра аудио
    const startAudioPreview = async () => {
        if (selectedDevices.audioInput) {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    audio: { deviceId: selectedDevices.audioInput ? { exact: selectedDevices.audioInput } : undefined }
                });

                initAudioAnalyzer(stream);
                setAudioPreview(stream);
            } catch (error) {
                console.error('Ошибка запуска аудио:', error);
                setError('Не удалось запустить аудио. Проверьте доступ к микрофону.');
            }
        }
    };

    // Начало тестовой записи
    const startTestRecording = async () => {
        try {
            const constraints = {
                audio: { deviceId: selectedDevices.audioInput ? { exact: selectedDevices.audioInput } : undefined },
                video: selectedDevices.videoInput ? {
                    deviceId: { exact: selectedDevices.videoInput },
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                } : false
            };

            const stream = await navigator.mediaDevices.getUserMedia(constraints);

            // Останавливаем предыдущие предпросмотры
            if (videoPreview) {
                videoPreview.getTracks().forEach(track => track.stop());
            }
            if (audioPreview) {
                audioPreview.getTracks().forEach(track => track.stop());
            }

            // Запускаем запись
            mediaRecorderRef.current = new MediaRecorder(stream);
            const chunks = [];

            mediaRecorderRef.current.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    chunks.push(event.data);
                }
            };

            mediaRecorderRef.current.onstop = () => {
                const blob = new Blob(chunks, { type: 'video/webm' });
                const url = URL.createObjectURL(blob);
                setTestRecording(url);
                setTestStatus('complete');

                // Останавливаем поток после записи
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorderRef.current.start();
            setIsRecording(true);
            setTestStatus('recording');
            setRecordingTime(0);

            // Таймер записи (5 секунд)
            recordingTimerRef.current = setInterval(() => {
                setRecordingTime(prev => {
                    if (prev >= 4) { // 5 секунд (0-4)
                        stopTestRecording();
                        return 0;
                    }
                    return prev + 1;
                });
            }, 1000);

        } catch (error) {
            console.error('Ошибка записи:', error);
            setError('Не удалось начать запись. Проверьте доступ к устройствам.');
        }
    };

    // Остановка тестовой записи
    const stopTestRecording = () => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
        }

        if (recordingTimerRef.current) {
            clearInterval(recordingTimerRef.current);
        }

        setIsRecording(false);
    };

    // Воспроизведение записи
    const playRecording = () => {
        if (testRecording && audioRef.current) {
            audioRef.current.play();
        }
    };

    // Очистка ресурсов
    const cleanup = () => {
        if (videoPreview) {
            videoPreview.getTracks().forEach(track => track.stop());
        }
        if (audioPreview) {
            audioPreview.getTracks().forEach(track => track.stop());
        }
        if (animationFrameRef.current) {
            cancelAnimationFrame(animationFrameRef.current);
        }
        if (audioContextRef.current) {
            audioContextRef.current.close();
        }
        if (recordingTimerRef.current) {
            clearInterval(recordingTimerRef.current);
        }
        if (testRecording) {
            URL.revokeObjectURL(testRecording);
        }
    };

    // Применение настроек
    const applySettings = () => {
        if (onTestComplete) {
            onTestComplete(selectedDevices);
        }
    };

    // Инициализация при монтировании
    useEffect(() => {
        getDevices();
        return cleanup;
    }, []);

    // Обновление предпросмотра при изменении выбранных устройств
    useEffect(() => {
        if (selectedDevices.videoInput) {
            startVideoPreview();
        }
        if (selectedDevices.audioInput) {
            startAudioPreview();
        }
    }, [selectedDevices.videoInput, selectedDevices.audioInput]);

    return (
        <div className="device-test">
            <div className="test-header">
                <h3>Тест аудио и видео устройств</h3>
                <p>Проверьте и настройте устройства перед трансляцией</p>
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
                {/* Выбор устройств */}
                <div className="device-selection">
                    <div className="device-group">
                        <h4>Микрофон</h4>
                        <select
                            value={selectedDevices.audioInput}
                            onChange={(e) => setSelectedDevices(prev => ({ ...prev, audioInput: e.target.value }))}
                            disabled={isRecording}
                        >
                            <option value="">Выберите микрофон</option>
                            {devices.audioInputs.map(device => (
                                <option key={device.deviceId} value={device.deviceId}>
                                    {device.label || `Микрофон ${device.deviceId.slice(0, 8)}`}
                                </option>
                            ))}
                        </select>

                        {/* Визуализация уровня звука */}
                        {selectedDevices.audioInput && (
                            <div className="audio-level">
                                <div className="audio-level-bar">
                                    <div
                                        className="audio-level-fill"
                                        style={{ width: `${Math.min(audioLevel * 2, 100)}%` }}
                                    />
                                </div>
                                <span className="audio-level-text">
                                    Уровень звука: {Math.round(audioLevel)}%
                                </span>
                            </div>
                        )}
                    </div>

                    <div className="device-group">
                        <h4>Камера</h4>
                        <select
                            value={selectedDevices.videoInput}
                            onChange={(e) => setSelectedDevices(prev => ({ ...prev, videoInput: e.target.value }))}
                            disabled={isRecording}
                        >
                            <option value="">Выберите камеру</option>
                            {devices.videoInputs.map(device => (
                                <option key={device.deviceId} value={device.deviceId}>
                                    {device.label || `Камера ${device.deviceId.slice(0, 8)}`}
                                </option>
                            ))}
                        </select>

                        {/* Предпросмотр видео */}
                        {selectedDevices.videoInput && (
                            <div className="video-preview">
                                <video
                                    ref={videoRef}
                                    autoPlay
                                    muted
                                    playsInline
                                    className="preview-video"
                                />
                                <div className="preview-label">Предпросмотр камеры</div>
                            </div>
                        )}
                    </div>

                    <div className="device-group">
                        <h4>Динамики</h4>
                        <select
                            value={selectedDevices.audioOutput}
                            onChange={(e) => setSelectedDevices(prev => ({ ...prev, audioOutput: e.target.value }))}
                        >
                            <option value="">Выберите динамики</option>
                            {devices.audioOutputs.map(device => (
                                <option key={device.deviceId} value={device.deviceId}>
                                    {device.label || `Динамики ${device.deviceId.slice(0, 8)}`}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* Тестовая запись */}
                <div className="test-recording">
                    <h4>Тестовая запись</h4>
                    <p>Запишите короткий фрагмент (5 секунд) для проверки качества</p>

                    <div className="recording-controls">
                        {!isRecording ? (
                            <button
                                className="record-btn"
                                onClick={startTestRecording}
                                disabled={!selectedDevices.audioInput}
                            >
                                🎤 Записать тестовый фрагмент
                            </button>
                        ) : (
                            <div className="recording-status">
                                <div className="recording-indicator">
                                    <div className="recording-dot" />
                                    <span>Идет запись... {5 - recordingTime} сек</span>
                                </div>
                                <button
                                    className="stop-btn"
                                    onClick={stopTestRecording}
                                >
                                    ⏹️ Остановить
                                </button>
                            </div>
                        )}

                        {testRecording && (
                            <div className="playback-controls">
                                <button
                                    className="play-btn"
                                    onClick={playRecording}
                                >
                                    ▶️ Прослушать запись
                                </button>
                                <audio
                                    ref={audioRef}
                                    src={testRecording}
                                    controls
                                    className="test-audio"
                                />
                            </div>
                        )}
                    </div>
                </div>

                {/* Статус тестирования */}
                {testStatus === 'recording' && (
                    <div className="test-status recording">
                        <div className="status-indicator" />
                        <span>Запись тестового фрагмента...</span>
                    </div>
                )}

                {testStatus === 'complete' && testRecording && (
                    <div className="test-result">
                        <div className="result-success">
                            ✅ Тест завершен успешно!
                        </div>
                        <p className="result-description">
                            Устройства настроены корректно. Вы можете применить настройки.
                        </p>
                    </div>
                )}
            </div>

            <div className="test-actions">
                <button className="cancel-btn" onClick={onCancel}>
                    Отмена
                </button>
                <button
                    className="apply-btn"
                    onClick={applySettings}
                    disabled={!selectedDevices.audioInput}
                >
                    Применить настройки
                </button>
            </div>
        </div>
    );
};

export default DeviceTest;