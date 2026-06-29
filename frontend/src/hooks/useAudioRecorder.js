import { useState, useRef, useCallback, useEffect } from 'react';

/**
 * Хук для записи аудио с микрофона.
 * Возвращает состояние записи, управляющие функции и blob URL полученной записи.
 */
const useAudioRecorder = () => {
    const [isRecording, setIsRecording] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [audioBlobUrl, setAudioBlobUrl] = useState(null);
    const mediaRecorderRef = useRef(null);
    const timerRef = useRef(null);
    const chunksRef = useRef([]);
    const streamRef = useRef(null);

    const clearTimer = useCallback(() => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
    }, []);

    const stopTracks = useCallback(() => {
        if (streamRef.current) {
            streamRef.current.getTracks().forEach(track => track.stop());
            streamRef.current = null;
        }
    }, []);

    /** Начать запись */
    const startRecording = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            streamRef.current = stream;
            const recorder = new MediaRecorder(stream);
            chunksRef.current = [];

            recorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunksRef.current.push(e.data);
                }
            };

            recorder.onstop = () => {
                const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
                const url = URL.createObjectURL(blob);
                setAudioBlobUrl(url);
                stopTracks();
                clearTimer();
                setIsRecording(false);
                setRecordingTime(0);
            };

            recorder.start();
            mediaRecorderRef.current = recorder;
            setIsRecording(true);
            setRecordingTime(0);
            setAudioBlobUrl(null);

            timerRef.current = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
        } catch (err) {
            throw err; // пробрасываем, чтобы компонент мог обработать
        }
    }, [clearTimer, stopTracks]);

    /** Остановить запись */
    const stopRecording = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
        } else {
            // Если записи не было (например, ошибка) — просто сбросить
            clearTimer();
            stopTracks();
            setIsRecording(false);
            setRecordingTime(0);
        }
    }, [clearTimer, stopTracks]);

    /** Сбросить результат записи (удалить blob URL) */
    const resetRecording = useCallback(() => {
        if (audioBlobUrl) {
            URL.revokeObjectURL(audioBlobUrl);
        }
        setAudioBlobUrl(null);
        setIsRecording(false);
        setRecordingTime(0);
    }, [audioBlobUrl]);

    // Очистка при размонтировании
    useEffect(() => {
        return () => {
            if (audioBlobUrl) {
                URL.revokeObjectURL(audioBlobUrl);
            }
            clearTimer();
            stopTracks();
        };
    }, [audioBlobUrl, clearTimer, stopTracks]);

    return {
        isRecording,
        recordingTime,
        audioBlobUrl,
        startRecording,
        stopRecording,
        resetRecording,
    };
};

export default useAudioRecorder;
