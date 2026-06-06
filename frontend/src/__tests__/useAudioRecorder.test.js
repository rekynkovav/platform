import { renderHook, act } from '@testing-library/react';
import useAudioRecorder from '../../hooks/useAudioRecorder';

// Мок для MediaRecorder
class MockMediaRecorder {
    constructor(stream) {
        this.stream = stream;
        this.state = 'inactive';
        this.ondataavailable = null;
        this.onstop = null;
        this.onerror = null;
    }

    start() {
        this.state = 'recording';
        setTimeout(() => {
            if (this.ondataavailable) {
                this.ondataavailable({ data: new Blob(['test audio data'], { type: 'audio/webm' }) });
            }
        }, 50);
    }

    stop() {
        this.state = 'inactive';
        if (this.onstop) this.onstop({});
    }
}

global.MediaRecorder = MockMediaRecorder;

// Мок для getUserMedia
global.navigator.mediaDevices = {
    getUser