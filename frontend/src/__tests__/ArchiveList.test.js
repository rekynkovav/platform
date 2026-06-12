import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ArchiveList from '../components/Archive/ArchiveList';
import { mediaApi, broadcastApi, demoApi } from '../services/api';

// Моки
jest.mock('../services/api', () => ({
    mediaApi: {
        download: jest.fn(),
        getStreamUrl: jest.fn(),
    },
    broadcastApi: {
        getActive: jest.fn(),
        start: jest.fn(),
        end: jest.fn(),
    },
    demoApi: {
        getDemoRole: jest.fn(),
        getWillAttendCounts: jest.fn(),
        saveWillAttendCounts: jest.fn(),
        hasUserAttended: jest.fn(),
        markUserAttended: jest.fn(),
    },
}));

jest.mock('../components/Archive/ArchiveList.css', () => ({}));

// Мок для вложенных компонентов
jest.mock('../components/live/LiveBroadcastViewSimple', () => () => <div data-testid="live-view-simple">LiveBroadcastViewSimple</div>);
jest.mock('../components/live/EnhancedLiveBroadcastView', () => () => <div data-testid="enhanced-live-view">EnhancedLiveBroadcastView</div>);
jest.mock('../components/live/NetworkSpeedTest', () => ({ onTestComplete, onCancel }) => (
    <div data-testid="network-speed-test">
        NetworkSpeedTest
        <button onClick={() => onTestComplete({ quality: 'high' })}>Complete</button>
        <button onClick={onCancel}>Cancel</button>
    </div>
));
jest.mock('../components/live/DeviceTest', () => ({ onTestComplete, onCancel }) => (
    <div data-testid="device-test">
        DeviceTest
        <button onClick={() => onTestComplete({ audioInput: 'mic1' })}>Complete</button>
        <button onClick={onCancel}>Cancel</button>
    </div>
));

describe('ArchiveList', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        process.env.NODE_ENV = 'test';
        // Mock sessionStorage
        const store = {};
        jest.spyOn(Storage.prototype, 'getItem').mockImplementation(key => store[key] || null);
        jest.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => { store[key] = value; });
        jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(key => { delete store[key]; });

        demoApi.getDemoRole.mockResolvedValue('MODERATOR_ADMIN');
        demoApi.getWillAttendCounts.mockResolvedValue({});
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('renders archive list header', async () => {
        render(<ArchiveList />);

        await waitFor(() => {
            expect(screen.getByText(/Прямой эфир - Основы питания/)).toBeInTheDocument();
        });
    });

    it('loads broadcasts from sessionStorage in development', async () => {
        process.env.NODE_ENV = 'development';
        const mockBroadcasts = JSON.stringify([
            {
                id: 99,
                title: 'Dev Broadcast',
                description: 'Dev desc',
                date: '2026-12-31',
                time: '18:00',
                type: 'video',
                status: 'scheduled',
                fileId: 'dev-file',
                mediaUrl: '/api/media/stream/dev-file',
                ownerId: 'SUPER_ADMIN'
            }
        ]);
        sessionStorage.getItem.mockImplementation((key) => {
            if (key === 'broadcasts') return mockBroadcasts;
            return null;
        });

        render(<ArchiveList />);

        await waitFor(() => {
            expect(screen.getByText('Dev Broadcast')).toBeInTheDocument();
        });
    });

    it('shows create form for admin user', async () => {
        render(<ArchiveList />);

        await waitFor(() => {
            expect(screen.getByText('Создать трансляцию')).toBeInTheDocument();
        });
    });

    it('renders will attend button', async () => {
        render(<ArchiveList />);

        await waitFor(() => {
            expect(screen.getByText('Буду!')).toBeInTheDocument();
        });
    });
});
