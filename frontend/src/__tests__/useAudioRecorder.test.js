import { renderHook, act } from '@testing-library/react';
import useAudioRecorder from '../hooks/useAudioRecorder';

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
global.MediaStream = class MediaStream {};

// Мок для getUserMedia
global.navigator.mediaDevices = {
    getUserMedia: jest.fn().mockResolvedValue(new MediaStream())
};

// Мок для URL.createObjectURL/revokeObjectURL (отсутствует в jsdom)
let blobCounter = 0;
beforeEach(() => {
    URL.createObjectURL = jest.fn(() => `blob:test-${++blobCounter}`);
    URL.revokeObjectURL = jest.fn();
});

describe('useAudioRecorder', () => {
    it('should start recording', async () => {
        const { result } = renderHook(() => useAudioRecorder());

        expect(result.current.isRecording).toBe(false);
        expect(result.current.recordingTime).toBe(0);
        expect(result.current.audioBlobUrl).toBeNull();

        await act(async () => {
            await result.current.startRecording();
        });

        expect(result.current.isRecording).toBe(true);
    });

    it('should stop recording and produce audio blob', async () => {
        const { result } = renderHook(() => useAudioRecorder());

        await act(async () => {
            await result.current.startRecording();
        });

        // Wait for the mock to fire ondataavailable
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 100));
            result.current.stopRecording();
        });

        expect(result.current.isRecording).toBe(false);
        // After stop, audioBlobUrl should be a blob URL (set by onstop handler)
        expect(result.current.audioBlobUrl).toMatch(/^blob:/);
    });

    it('should reset recording', async () => {
        const { result } = renderHook(() => useAudioRecorder());

        await act(async () => {
            await result.current.startRecording();
        });

        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 100));
            result.current.stopRecording();
        });

        await act(async () => {
            result.current.resetRecording();
        });

        expect(result.current.isRecording).toBe(false);
        expect(result.current.recordingTime).toBe(0);
        expect(result.current.audioBlobUrl).toBeNull();
    });
});
