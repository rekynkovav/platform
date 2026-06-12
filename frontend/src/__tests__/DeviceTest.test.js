import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DeviceTest from '../components/live/DeviceTest';

// Моки
const mockGetUserMedia = jest.fn();
const mockEnumerateDevices = jest.fn();

global.navigator.mediaDevices = {
    getUserMedia: mockGetUserMedia,
    enumerateDevices: mockEnumerateDevices,
};

// Мок для MediaRecorder
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
        if (this.onstop) this.onstop({});
    }
}
global.MediaRecorder = MockMediaRecorder;

// Мок для AudioContext
global.AudioContext = class {
    constructor() { this.state = 'running'; }
    close() { this.state = 'closed'; }
    createAnalyser() {
        return {
            fftSize: 256,
            frequencyBinCount: 128,
            getByteFrequencyData: jest.fn(),
        };
    }
    createMediaStreamSource() {
        return { connect: jest.fn() };
    }
};

jest.mock('../components/live/DeviceTest.css', () => ({}));

describe('DeviceTest', () => {
    const mockOnTestComplete = jest.fn();
    const mockOnCancel = jest.fn();

    const mockDevices = [
        { deviceId: 'audio1', kind: 'audioinput', label: 'Microphone 1' },
        { deviceId: 'audio2', kind: 'audioinput', label: 'Microphone 2' },
        { deviceId: 'video1', kind: 'videoinput', label: 'Camera 1' },
        { deviceId: 'audio3', kind: 'audiooutput', label: 'Speakers 1' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        mockGetUserMedia.mockResolvedValue(new MediaStream());
        mockEnumerateDevices.mockResolvedValue(mockDevices);
    });

    it('renders device test header', async () => {
        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Тест аудио и видео устройств')).toBeInTheDocument();
        });
    });

    it('renders device selection sections', async () => {
        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Микрофон')).toBeInTheDocument();
            expect(screen.getByText('Камера')).toBeInTheDocument();
            expect(screen.getByText('Динамики')).toBeInTheDocument();
        });
    });

    it('renders cancel and apply buttons', async () => {
        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Отмена')).toBeInTheDocument();
            expect(screen.getByText('Применить настройки')).toBeInTheDocument();
        });
    });

    it('calls onCancel when cancel is clicked', async () => {
        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Отмена')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Отмена'));
        expect(mockOnCancel).toHaveBeenCalled();
    });

    it('shows error when getUserMedia fails', async () => {
        mockGetUserMedia.mockRejectedValue(new Error('Permission denied'));

        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText(/Не удалось получить доступ к устройствам/)).toBeInTheDocument();
        });
    });

    it('renders test recording section', async () => {
        render(<DeviceTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Тестовая запись')).toBeInTheDocument();
            expect(screen.getByText(/Запишите короткий фрагмент/)).toBeInTheDocument();
        });
    });
});
