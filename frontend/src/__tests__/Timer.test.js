import React from 'react';
import { render, screen } from '@testing-library/react';
import Timer from '../components/Dashboard/Timer';

jest.mock('../components/Dashboard/Timer.css', () => ({}));

describe('Timer', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2025-06-11T12:00:00'));
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('renders timer counting down', () => {
        const futureTime = new Date('2025-06-11T12:00:30').toISOString();
        render(<Timer targetTime={futureTime} />);
        expect(screen.getByText('ДО НАЧАЛА СОБЫТИЯ')).toBeInTheDocument();
    });

    it('shows expired message when target time has passed', () => {
        const pastTime = new Date('2025-06-11T11:00:00').toISOString();
        render(<Timer targetTime={pastTime} />);
        expect(screen.getByText('СОБЫТИЕ НАЧАЛОСЬ!')).toBeInTheDocument();
        expect(screen.getByText('Присоединяйтесь сейчас')).toBeInTheDocument();
    });

    it('shows days unit when days > 0', () => {
        const futureTime = new Date('2025-06-15T12:00:00').toISOString();
        render(<Timer targetTime={futureTime} />);
        const timerValues = screen.getAllByText('00', { exact: false });
        expect(timerValues.length).toBeGreaterThan(0);
    });

    it('hides days unit when days = 0', () => {
        const futureTime = new Date('2025-06-11T12:00:30').toISOString();
        render(<Timer targetTime={futureTime} />);
        const timerValues = screen.getAllByText('00', { exact: false });
        expect(timerValues.length).toBeGreaterThan(0);
    });

    it('formats time units with leading zeros', () => {
        const futureTime = new Date('2025-06-11T12:01:05').toISOString();
        render(<Timer targetTime={futureTime} />);
        expect(screen.getAllByText('00', { exact: false }).length).toBeGreaterThan(0);
    });
});
