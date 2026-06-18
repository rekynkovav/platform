import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import GlobalEventNotification from '../components/Notification/GlobalEventNotification';

jest.mock('../components/Notification/GlobalEventNotification.css', () => ({}));

describe('GlobalEventNotification', () => {
    const originalEnv = process.env.NODE_ENV;

    beforeEach(() => {
        jest.clearAllMocks();
        jest.useFakeTimers();
        sessionStorage.clear();
    });

    afterEach(() => {
        jest.useRealTimers();
        Object.defineProperty(process.env, 'NODE_ENV', { value: originalEnv, writable: true });
    });

    it('returns null when no event in sessionStorage', () => {
        Object.defineProperty(process.env, 'NODE_ENV', { value: 'development', writable: true });
        const { container } = render(<GlobalEventNotification onEventClick={jest.fn()} />);
        expect(container.innerHTML).toBe('');
    });

    it('renders when a future event exists within 7 days', () => {
        Object.defineProperty(process.env, 'NODE_ENV', { value: 'development', writable: true });
        const futureDate = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000);
        const dateStr = futureDate.toISOString().split('T')[0];
        const timeStr = futureDate.toTimeString().slice(0, 5);
        const events = [{ id: 1, title: 'Test Event', date: dateStr, time: timeStr }];
        sessionStorage.setItem('scheduledEvents', JSON.stringify(events));
        render(<GlobalEventNotification onEventClick={jest.fn()} />);
        expect(screen.getByText('Test Event')).toBeInTheDocument();
        expect(screen.getByText('До Ближайшего События')).toBeInTheDocument();
    });

    it('calls onEventClick when notification is clicked', () => {
        Object.defineProperty(process.env, 'NODE_ENV', { value: 'development', writable: true });
        const onEventClick = jest.fn();
        const futureDate = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000);
        const dateStr = futureDate.toISOString().split('T')[0];
        const timeStr = futureDate.toTimeString().slice(0, 5);
        const events = [{ id: 1, title: 'Test Event', date: dateStr, time: timeStr }];
        sessionStorage.setItem('scheduledEvents', JSON.stringify(events));
        render(<GlobalEventNotification onEventClick={onEventClick} />);
        fireEvent.click(screen.getByText('Test Event').closest('.global-event-notification'));
        expect(onEventClick).toHaveBeenCalled();
    });

    it('does not render when event is more than 7 days away', () => {
        Object.defineProperty(process.env, 'NODE_ENV', { value: 'development', writable: true });
        const farDate = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000);
        const dateStr = farDate.toISOString().split('T')[0];
        const timeStr = farDate.toTimeString().slice(0, 5);
        const events = [{ id: 1, title: 'Far Event', date: dateStr, time: timeStr }];
        sessionStorage.setItem('scheduledEvents', JSON.stringify(events));
        const { container } = render(<GlobalEventNotification onEventClick={jest.fn()} />);
        expect(container.innerHTML).toBe('');
    });

    it('does not render when event is in the past', () => {
        Object.defineProperty(process.env, 'NODE_ENV', { value: 'development', writable: true });
        const pastDate = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);
        const dateStr = pastDate.toISOString().split('T')[0];
        const timeStr = pastDate.toTimeString().slice(0, 5);
        const events = [{ id: 1, title: 'Past Event', date: dateStr, time: timeStr }];
        sessionStorage.setItem('scheduledEvents', JSON.stringify(events));
        const { container } = render(<GlobalEventNotification onEventClick={jest.fn()} />);
        expect(container.innerHTML).toBe('');
    });
});
