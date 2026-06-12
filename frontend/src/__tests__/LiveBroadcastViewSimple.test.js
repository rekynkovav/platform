import React from 'react';
import { render, screen } from '@testing-library/react';
import LiveBroadcastView from '../components/live/LiveBroadcastViewSimple';

jest.mock('../components/live/LiveBroadcastView.css', () => ({}));

describe('LiveBroadcastViewSimple', () => {
    const mockBroadcast = {
        title: 'Test Broadcast',
        viewerCount: 5,
        type: 'video',
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders with default demo title when no broadcast', () => {
        render(<LiveBroadcastView />);
        expect(screen.getByText(/Демо трансляция/)).toBeInTheDocument();
    });

    it('renders broadcast title', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText(/Test Broadcast/)).toBeInTheDocument();
    });

    it('renders viewer count', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText(/5 зрителей/)).toBeInTheDocument();
    });

    it('shows demo mode notice', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText(/Демо режим/)).toBeInTheDocument();
    });

    it('shows video type text for video broadcasts', () => {
        render(<LiveBroadcastView broadcast={{ ...mockBroadcast, type: 'video' }} />);
        expect(screen.getByText('Видео трансляция')).toBeInTheDocument();
    });

    it('shows audio type text for audio broadcasts', () => {
        render(<LiveBroadcastView broadcast={{ ...mockBroadcast, type: 'audio' }} />);
        expect(screen.getByText('Аудио трансляция')).toBeInTheDocument();
    });

    it('renders chat section by default', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText('Чат трансляции')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Написать сообщение...')).toBeInTheDocument();
    });

    it('hides chat when showChat is false', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} showChat={false} />);
        expect(screen.queryByText('Чат трансляции')).not.toBeInTheDocument();
    });

    it('renders controls section by default', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText('Управление трансляцией')).toBeInTheDocument();
    });

    it('hides controls when showControls is false', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} showControls={false} />);
        expect(screen.queryByText('Управление трансляцией')).not.toBeInTheDocument();
    });

    it('renders stats section when showStats is true', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} showStats={true} />);
        expect(screen.getByText('Статистика')).toBeInTheDocument();
        expect(screen.getByText('Зрители онлайн')).toBeInTheDocument();
        expect(screen.getByText('Длительность')).toBeInTheDocument();
        expect(screen.getByText('Сообщений в чате')).toBeInTheDocument();
    });

    it('hides stats section by default', () => {
        render(<LiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.queryByText('Статистика')).not.toBeInTheDocument();
    });
});
