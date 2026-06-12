import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Dashboard from '../components/Dashboard/Dashboard';
import { dashboardApi } from '../services/api';
import { usePermissions } from '../hooks/usePermissions';
import useAudioRecorder from '../hooks/useAudioRecorder';

jest.mock('../services/api', () => ({
    dashboardApi: {
        getMenuData: jest.fn(),
        saveMenuData: jest.fn(),
    },
}));

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

jest.mock('../hooks/useAudioRecorder', () => jest.fn());

jest.mock('../components/Dashboard/Dashboard.css', () => ({}));

jest.mock('../components/common/ConfirmModal', () => {
    return function MockConfirmModal({ isOpen, title, message, onConfirm, onCancel }) {
        if (!isOpen) return null;
        return (
            <div data-testid="confirm-modal">
                <span>{title}</span>
                <span>{message}</span>
                <button data-testid="confirm-yes" onClick={onConfirm}>Yes</button>
                <button data-testid="confirm-no" onClick={onCancel}>No</button>
            </div>
        );
    };
});

jest.mock('../components/common/AddContentModal', () => {
    return function MockAddContentModal({ isOpen, onClose, editingItem, handleSaveContent }) {
        if (!isOpen) return null;
        return (
            <div data-testid="add-modal">
                <button data-testid="save-content" onClick={handleSaveContent}>Save</button>
                <button data-testid="close-modal" onClick={onClose}>Close</button>
            </div>
        );
    };
});

describe('Dashboard', () => {
    const mockMenuData = {
        broadcasts: [
            { id: 1, title: 'Test Broadcast', description: 'Test desc' },
        ],
        exercises: [
            { id: 2, title: 'Test Exercise', description: 'Exercise desc' },
        ],
    };

    beforeEach(() => {
        jest.clearAllMocks();
        usePermissions.mockReturnValue({
            hasLevel: jest.fn().mockReturnValue(true),
        });
        useAudioRecorder.mockReturnValue({
            isRecording: false,
            recordingTime: 0,
            audioBlobUrl: null,
            startRecording: jest.fn(),
            stopRecording: jest.fn(),
            resetRecording: jest.fn(),
        });
    });

    it('renders all navigation menu cards', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText('Эфиры')).toBeInTheDocument();
        });
        expect(screen.getByText('Упражнения')).toBeInTheDocument();
        expect(screen.getByText('Рекомендации')).toBeInTheDocument();
        expect(screen.getByText('Закаливание')).toBeInTheDocument();
        expect(screen.getByText('О нас')).toBeInTheDocument();
    });

    it('opens dropdown on menu hover with items', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText('Эфиры')).toBeInTheDocument();
        });

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        await waitFor(() => {
            expect(screen.getByText('Test Broadcast')).toBeInTheDocument();
            expect(screen.getByText('Test desc')).toBeInTheDocument();
        });
    });

    it('shows add button in dropdown for admin users', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        await screen.findByText('Добавить');
        expect(screen.getByText('Добавить')).toBeInTheDocument();
    });

    it('opens add content modal when clicking add button', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const addBtn = await screen.findByText('Добавить');
        fireEvent.click(addBtn);

        expect(screen.getByTestId('add-modal')).toBeInTheDocument();
    });

    it('closes add modal on close', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const addBtn = await screen.findByText('Добавить');
        fireEvent.click(addBtn);

        expect(screen.getByTestId('add-modal')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('close-modal'));

        await waitFor(() => {
            expect(screen.queryByTestId('add-modal')).not.toBeInTheDocument();
        });
    });

    it('opens delete confirmation when delete button is clicked', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const deleteBtn = await screen.findByTitle('Удалить');
        fireEvent.click(deleteBtn);

        expect(screen.getByTestId('confirm-modal')).toBeInTheDocument();
    });
});
