import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Dashboard from '../components/Dashboard/Dashboard';
import { dashboardApi } from '../services/api';
import { usePermissions } from '../hooks/usePermissions';
import useAudioRecorder from '../hooks/useAudioRecorder';

// Моки
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

// Мок для ConfirmModal
jest.mock('../components/common/ConfirmModal', () => {
    return function MockConfirmModal({ isOpen, title, message, onConfirm, onCancel }) {
        if (!isOpen) return null;
        return (
            <div data-testid="confirm-modal">
                <span>{title}</span>
                <span>{message}</span>
                <button data-testid="confirm-yes" onClick={onConfirm}>Да</button>
                <button data-testid="confirm-no" onClick={onCancel}>Нет</button>
            </div>
        );
    };
});

// Мок для AddContentModal
jest.mock('../components/common/AddContentModal', () => {
    return function MockAddContentModal({ isOpen, onClose, editingItem, handleSaveContent }) {
        if (!isOpen) return null;
        return (
            <div data-testid="add-modal">
                <button data-testid="save-content" onClick={handleSaveContent}>Сохранить</button>
                <button data-testid="close-modal" onClick={onClose}>Закрыть</button>
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

    it('renders loading state and then menus', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        expect(screen.getByText('Эфиры')).toBeInTheDocument();
        expect(screen.getByText('Упражнения')).toBeInTheDocument();

        await waitFor(() => {
            // Проверяем, что карточки меню отображаются (все 18 разделов)
            expect(screen.getByText('Эфиры')).toBeInTheDocument();
            expect(screen.getByText('Упражнения')).toBeInTheDocument();
            expect(screen.getByText('Рекомендации')).toBeInTheDocument();
            expect(screen.getByText('Закаливание')).toBeInTheDocument();
            expect(screen.getByText('О нас')).toBeInTheDocument();
        });
    });

    it('opens dropdown on menu hover', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        await waitFor(() => {
            expect(screen.getByText('Test Broadcast')).toBeInTheDocument();
            expect(screen.getByText('Test desc')).toBeInTheDocument();
        });
    });

    it('shows add button for admin', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        await waitFor(() => {
            expect(screen.getByText('Добавить')).toBeInTheDocument();
        });
    });

    it('opens add modal when clicking add button', async () => {
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

    it('saves content via modal', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        dashboardApi.saveMenuData.mockResolvedValue({});
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const addBtn = await screen.findByText('Добавить');
        fireEvent.click(addBtn);

        fireEvent.click(screen.getByTestId('save-content'));

        await waitFor(() => {
            expect(dashboardApi.saveMenuData).toHaveBeenCalled();
            expect(screen.queryByTestId('add-modal')).not.toBeInTheDocument();
        });
    });

    it('opens delete confirmation and executes delete', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        dashboardApi.saveMenuData.mockResolvedValue({});
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const deleteBtn = await screen.findByTitle('Удалить');
        fireEvent.click(deleteBtn);

        expect(screen.getByTestId('confirm-modal')).toBeInTheDocument();
        expect(screen.getByText('Подтверждение удаления')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('confirm-yes'));

        await waitFor(() => {
            expect(dashboardApi.saveMenuData).toHaveBeenCalled();
            expect(screen.queryByTestId('confirm-modal')).not.toBeInTheDocument();
        });
    });

    it('cancels delete', async () => {
        dashboardApi.getMenuData.mockResolvedValue(mockMenuData);
        render(<Dashboard />);

        const menuCard = screen.getByText('Эфиры').closest('.navigation-menu-card');
        fireEvent.mouseEnter(menuCard);

        const deleteBtn = await screen.findByTitle('Удалить');
        fireEvent.click(deleteBtn);

        expect(screen.getByTestId('confirm-modal')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('confirm-no'));

        await waitFor(() => {
            expect(screen.queryByTestId('confirm-modal')).not.toBeInTheDocument();
        });
    });

    it('shows error when API fails', async () => {
        dashboardApi.getMenuData.mockRejectedValue(new Error('Network error'));
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить данные дашборда')).toBeInTheDocument();
        });
    });
});
