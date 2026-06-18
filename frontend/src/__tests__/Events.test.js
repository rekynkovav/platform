import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Events from '../components/Common/Events';

jest.mock('../components/Common/Events.css', () => ({}));
jest.mock('../services/api', () => ({ eventsApi: {} }));
jest.mock('react-window', () => ({
    FixedSizeList: ({ children, itemCount, ...props }) => (
        <div data-testid="virtual-list">
            {Array.from({ length: Math.min(itemCount, 5) }, (_, i) => (
                <div key={i}>{children({ index: i, style: {} })}</div>
            ))}
        </div>
    ),
}));
jest.mock('react-virtualized-auto-sizer', () => ({
    AutoSizer: ({ children }) => children({ height: 500, width: 800 }),
}));

describe('Events', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders without crashing', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByText('📅 События')).toBeInTheDocument();
        });
    });

    it('shows stats section after loading', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByText('Всего событий')).toBeInTheDocument();
            expect(screen.getByText('Предстоящих')).toBeInTheDocument();
            expect(screen.getByText('Мои регистрации')).toBeInTheDocument();
        });
    });

    it('toggles from list to calendar view', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByText('📅 События')).toBeInTheDocument();
        });
        const calendarBtn = screen.getAllByTitle('Календарь')[0];
        fireEvent.click(calendarBtn);
        expect(screen.getByText(/Календарь событий/)).toBeInTheDocument();
    });

    it('toggles back to list view from calendar', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByText('📅 События')).toBeInTheDocument();
        });
        fireEvent.click(screen.getAllByTitle('Календарь')[0]);
        expect(screen.getByText(/Календарь событий/)).toBeInTheDocument();
        fireEvent.click(screen.getAllByTitle('Список')[0]);
        expect(screen.queryByText(/Календарь событий/)).not.toBeInTheDocument();
    });

    it('renders filter buttons', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByText('Предстоящие')).toBeInTheDocument();
            expect(screen.getByText('Сейчас')).toBeInTheDocument();
            expect(screen.getByText('Прошедшие')).toBeInTheDocument();
            expect(screen.getByText('Мои')).toBeInTheDocument();
        });
    });

    it('shows virtual list in list view', async () => {
        render(<Events />);
        await waitFor(() => {
            expect(screen.getByTestId('virtual-list')).toBeInTheDocument();
        });
    });
});
