import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ArchiveList from '../components/Archive/ArchiveList';
import { mediaApi, broadcastApi, demoApi } from '../services/api';
import { usePermissions } from '../hooks/usePermissions';

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

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
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

// Helper: create a permissions object for a given role level
const createPermissions = (roleName, level) => ({
    roles: [roleName],
    level,
    hasRole: (r) => [roleName].includes(r),
    hasAnyRole: (rs) => rs.some(r => [roleName].includes(r)),
    hasLevel: (n) => level >= n,
    canViewAdmin: () => level >= 6,
    canUploadMedia: () => level >= 4,
    canStartBroadcast: () => level >= 6,
    canModerate: () => level >= 6,
    isConsultant: () => false,
});

describe('ArchiveList', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        process.env.NODE_ENV = 'test';
        usePermissions.mockReturnValue(createPermissions('SUPER_ADMIN', 8));
        // Mock sessionStorage for broadcasts feature
        jest.spyOn(Storage.prototype, 'getItem').mockReturnValue(null);
        jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {});
        jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {});

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
        process.env.REACT_APP_ENVIRONMENT = 'development';
        render(<ArchiveList />);

        await waitFor(() => {
            expect(screen.getByText(/\+ Создать трансляцию/)).toBeInTheDocument();
        });
    });

    it('renders will attend button', async () => {
        render(<ArchiveList />);
        await waitFor(() => {
            expect(screen.getAllByText(/Буду!/).length).toBeGreaterThan(0);
        });
    });
});
