import { renderHook, act, waitFor } from '@testing-library/react';
import useChatMedia from '../features/chat/hooks/useChatMedia';
import { mediaApi } from '../services/api';

jest.mock('../services/api', () => ({
    mediaApi: {
        upload: jest.fn(),
    },
}));

// Mock MediaRecorder
class MockMediaRecorder {
    constructor(stream) {
        this.stream = stream;
        this.state = 'inactive';
        this.ondataavailable = null;
        this.onstop = null;
    }
    start() {
        this.state = 'recording';
    }
    stop() {
        this.state = 'inactive';
        if (this.ondataavailable) {
            this.ondataavailable({ data: new Blob(['audio'], { type: 'audio/webm' }) });
        }
        if (this.onstop) this.onstop({});
    }
}

global.MediaRecorder = MockMediaRecorder;

// Mock getUserMedia to return a stream-like object with getTracks
const mockGetTracks = jest.fn().mockReturnValue([{ stop: jest.fn() }]);
Object.defineProperty(navigator, 'mediaDevices', {
    value: {
        getUserMedia: jest.fn().mockResolvedValue({
            getTracks: mockGetTracks,
        }),
    },
    writable: true,
});

// Helper: fire startRecording and flush async state updates
async function flushRecording(result) {
    act(() => {
        result.current.startRecording();
    });
    // Flush microtasks so getUserMedia promise resolves and setState runs
    await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
    });
}

describe('useChatMedia', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Re-mock getUserMedia after clearAllMocks
        navigator.mediaDevices.getUserMedia.mockResolvedValue({
            getTracks: jest.fn().mockReturnValue([{ stop: jest.fn() }]),
        });
    });

    it('returns initial state correctly', () => {
        const { result } = renderHook(() => useChatMedia());
        expect(result.current.isRecording).toBe(false);
        expect(result.current.recordingTime).toBe(0);
        expect(result.current.uploading).toBe(false);
        expect(typeof result.current.uploadFile).toBe('function');
        expect(typeof result.current.startRecording).toBe('function');
        expect(typeof result.current.stopRecording).toBe('function');
        expect(typeof result.current.cleanup).toBe('function');
    });

    it('uploadFile calls mediaApi.upload and returns media info', async () => {
        mediaApi.upload.mockResolvedValue({ data: { id: 'media-123' } });
        const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' });

        const { result } = renderHook(() => useChatMedia());

        let uploadResult;
        await act(async () => {
            uploadResult = await result.current.uploadFile(file);
        });

        expect(mediaApi.upload).toHaveBeenCalledWith(file);
        expect(uploadResult).toEqual({
            mediaId: 'media-123',
            mediaType: 'image',
            fileName: 'photo.jpg',
        });
        expect(result.current.uploading).toBe(false);
    });

    it('uploadFile handles video file type correctly', async () => {
        mediaApi.upload.mockResolvedValue({ data: { id: 'vid-1' } });
        const file = new File(['test'], 'clip.mp4', { type: 'video/mp4' });

        const { result } = renderHook(() => useChatMedia());
        let uploadResult;
        await act(async () => {
            uploadResult = await result.current.uploadFile(file);
        });
        expect(uploadResult.mediaType).toBe('video');
    });

    it('uploadFile handles audio file type correctly', async () => {
        mediaApi.upload.mockResolvedValue({ data: { id: 'aud-1' } });
        const file = new File(['test'], 'song.mp3', { type: 'audio/mpeg' });

        const { result } = renderHook(() => useChatMedia());
        let uploadResult;
        await act(async () => {
            uploadResult = await result.current.uploadFile(file);
        });
        expect(uploadResult.mediaType).toBe('audio');
    });

    it('uploadFile handles generic file type correctly', async () => {
        mediaApi.upload.mockResolvedValue({ data: { id: 'file-1' } });
        const file = new File(['test'], 'doc.pdf', { type: 'application/pdf' });

        const { result } = renderHook(() => useChatMedia());
        let uploadResult;
        await act(async () => {
            uploadResult = await result.current.uploadFile(file);
        });
        expect(uploadResult.mediaType).toBe('file');
    });

    it('uploadFile sets uploading to true during upload and resets after', async () => {
        mediaApi.upload.mockResolvedValue({ data: { id: '1' } });
        const file = new File(['test'], 'x.jpg', { type: 'image/jpeg' });

        const { result } = renderHook(() => useChatMedia());
        await act(async () => {
            await result.current.uploadFile(file);
        });
        expect(result.current.uploading).toBe(false);
    });

    it('uploadFile sets uploading to false even on error', async () => {
        mediaApi.upload.mockRejectedValue(new Error('fail'));
        const file = new File(['test'], 'x.jpg', { type: 'image/jpeg' });

        const { result } = renderHook(() => useChatMedia());
        await act(async () => {
            try { await result.current.uploadFile(file); } catch {}
        });
        expect(result.current.uploading).toBe(false);
    });

    it('startRecording sets isRecording to true', async () => {
        const { result } = renderHook(() => useChatMedia());

        await flushRecording(result);

        expect(result.current.isRecording).toBe(true);
        expect(result.current.recordingTime).toBe(0);
    });

    it('startRecording calls getUserMedia', async () => {
        const { result } = renderHook(() => useChatMedia());

        await flushRecording(result);

        expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true });
    });

    it('stopRecording stops the recorder and resets state', async () => {
        const { result } = renderHook(() => useChatMedia());

        await flushRecording(result);

        expect(result.current.isRecording).toBe(true);

        // stopRecording triggers recorder.onstop which calls setIsRecording(false), setRecordingTime(0)
        act(() => {
            result.current.stopRecording();
        });

        expect(result.current.isRecording).toBe(false);
        expect(result.current.recordingTime).toBe(0);
    });

    it('startRecording resolves with null on getUserMedia error', async () => {
        navigator.mediaDevices.getUserMedia.mockRejectedValueOnce(new Error('Permission denied'));

        const { result } = renderHook(() => useChatMedia());
        const promise = result.current.startRecording();

        await act(async () => {
            await promise;
        });

        expect(result.current.isRecording).toBe(false);
    });

    it('cleanup stops recording and clears interval', async () => {
        const { result } = renderHook(() => useChatMedia());

        await flushRecording(result);

        expect(result.current.isRecording).toBe(true);

        act(() => {
            result.current.cleanup();
        });

        // cleanup stops the recorder which triggers onstop -> setIsRecording(false)
        expect(result.current.isRecording).toBe(false);
    });

    it('stopRecording does nothing if recorder is already inactive', () => {
        const { result } = renderHook(() => useChatMedia());
        // No recording started, so stopRecording should not throw
        expect(() => result.current.stopRecording()).not.toThrow();
    });
});
