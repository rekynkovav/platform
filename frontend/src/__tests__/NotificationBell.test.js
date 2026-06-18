import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationBell from '../components/Notification/NotificationBell';

jest.mock('../components/Notification/NotificationBell.css', () => ({}));

describe('NotificationBell', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.clear();
        sessionStorage.clear();
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('renders bell button', () => {
        render(<NotificationBell />);
        expect(screen.getByText('🔔')).toBeInTheDocument();
    });

    it('shows unread count when notifications exist', async () => {
        const notifications = [
            { id: '1', type: 'event', title: 'Event', message: 'msg', timestamp: new Date().toISOString(), read: false },
            { id: '2', type: 'admin-reply', title: 'Reply', message: 'msg', timestamp: new Date().toISOString(), read: true },
        ];
        localStorage.setItem('userNotifications', JSON.stringify(notifications));
        render(<NotificationBell />);
        await waitFor(() => {
            expect(screen.getByText('1')).toBeInTheDocument();
        });
    });

    it('opens dropdown on bell click', async () => {
        const notifications = [
            { id: '1', type: 'event', title: 'Event Title', message: 'msg', timestamp: new Date().toISOString(), read: false },
        ];
        localStorage.setItem('userNotifications', JSON.stringify(notifications));
        render(<NotificationBell />);
        await waitFor(() => {
            expect(screen.getByText('1')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('🔔'));
        expect(screen.getByText('Уведомления')).toBeInTheDocument();
        expect(screen.getByText('Event Title')).toBeInTheDocument();
    });

    it('shows no notifications message when list is empty', async () => {
        render(<NotificationBell />);
        fireEvent.click(screen.getByText('🔔'));
        await waitFor(() => {
            expect(screen.getByText('Нет уведомлений')).toBeInTheDocument();
        });
    });

    it('closes dropdown on click outside', async () => {
        const notifications = [
            { id: '1', type: 'event', title: 'Event', message: 'msg', timestamp: new Date().toISOString(), read: false },
        ];
        localStorage.setItem('userNotifications', JSON.stringify(notifications));
        render(<NotificationBell />);
        await waitFor(() => {
            expect(screen.getByText('1')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('🔔'));
        expect(screen.getByText('Уведомления')).toBeInTheDocument();
        fireEvent.mouseDown(document.body);
        await waitFor(() => {
            expect(screen.queryByText('Нет уведомлений')).not.toBeInTheDocument();
        });
    });

    it('shows clear all button when notifications exist', async () => {
        const notifications = [
            { id: '1', type: 'event', title: 'Event', message: 'msg', timestamp: new Date().toISOString(), read: false },
        ];
        localStorage.setItem('userNotifications', JSON.stringify(notifications));
        render(<NotificationBell />);
        await waitFor(() => {
            expect(screen.getByText('1')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('🔔'));
        expect(screen.getByText('🗑️')).toBeInTheDocument();
    });
});
