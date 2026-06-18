import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Notifications from '../components/Common/Notifications';

jest.mock('../components/Common/Notifications.css', () => ({}));
jest.mock('../services/api', () => ({
    chatApi: {},
    eventsApi: {},
    postsApi: {},
}));

describe('Notifications', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders notifications bell button', async () => {
        render(<Notifications />);
        expect(screen.getByLabelText('Уведомления')).toBeInTheDocument();
    });

    it('shows unread badge after loading', async () => {
        render(<Notifications />);
        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
    });

    it('opens dropdown on bell click', async () => {
        render(<Notifications />);
        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByLabelText('Уведомления'));
        expect(screen.getByText('Уведомления')).toBeInTheDocument();
        expect(screen.getByText('Новое сообщение')).toBeInTheDocument();
    });

    it('shows mark all read button when there are unread notifications', async () => {
        render(<Notifications />);
        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByLabelText('Уведомления'));
        expect(screen.getByText('Прочитать все')).toBeInTheDocument();
    });

    it('marks all as read when clicking mark all read', async () => {
        render(<Notifications />);
        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByLabelText('Уведомления'));
        await waitFor(() => {
            const markAllBtn = screen.getByText('Прочитать все');
            fireEvent.click(markAllBtn);
        });
        await waitFor(() => {
            expect(screen.queryByText('Прочитать все')).not.toBeInTheDocument();
        });
    });

    it('closes dropdown when close button is clicked', async () => {
        render(<Notifications />);
        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByLabelText('Уведомления'));
        const closeBtn = document.querySelector('.close-dropdown');
        fireEvent.click(closeBtn);
        await waitFor(() => {
            expect(screen.queryByText('Посмотреть все')).not.toBeInTheDocument();
        });
    });
});
