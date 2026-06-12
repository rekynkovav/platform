import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import EnhancedLiveBroadcastView from '../components/live/EnhancedLiveBroadcastView';

jest.mock('../components/live/EnhancedLiveBroadcastView.css', () => ({}));

describe('EnhancedLiveBroadcastView', () => {
    const mockBroadcast = {
        title: 'Enhanced Test Broadcast',
        viewerCount: 10,
        type: 'video',
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders broadcast header with title', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText('Enhanced Test Broadcast')).toBeInTheDocument();
        expect(screen.getByText('LIVE')).toBeInTheDocument();
    });

    it('renders default demo title when no broadcast', () => {
        render(<EnhancedLiveBroadcastView />);
        expect(screen.getByText('Демо трансляция')).toBeInTheDocument();
    });

    it('renders viewer count and duration', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText(/10 зрителей/)).toBeInTheDocument();
    });

    it('shows video type for video broadcast', () => {
        render(<EnhancedLiveBroadcastView broadcast={{ ...mockBroadcast, type: 'video' }} />);
        expect(screen.getByText('🎥 Видео')).toBeInTheDocument();
    });

    it('shows audio type for audio broadcast', () => {
        render(<EnhancedLiveBroadcastView broadcast={{ ...mockBroadcast, type: 'audio' }} />);
        expect(screen.getByText('🎙️ Аудио')).toBeInTheDocument();
    });

    it('shows end broadcast button for organizer', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} isOrganizer={true} />);
        expect(screen.getByText('🔴 Завершить')).toBeInTheDocument();
    });

    it('hides end broadcast button for non-organizer', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} isOrganizer={false} />);
        expect(screen.queryByText('🔴 Завершить')).not.toBeInTheDocument();
    });

    it('renders chat section by default', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText('Чат трансляции')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Написать сообщение...')).toBeInTheDocument();
    });

    it('hides chat when showChat is false', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} showChat={false} />);
        expect(screen.queryByText('Чат трансляции')).not.toBeInTheDocument();
    });

    it('allows sending messages in chat', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        const input = screen.getByPlaceholderText('Написать сообщение...');
        fireEvent.change(input, { target: { value: 'Hello!' } });
        fireEvent.click(screen.getByText('Отправить'));

        expect(screen.getByText('Hello!')).toBeInTheDocument();
    });

    it('sends message on Enter key press', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        const input = screen.getByPlaceholderText('Написать сообщение...');
        fireEvent.change(input, { target: { value: 'Enter message' } });
        fireEvent.keyPress(input, { key: 'Enter', code: 'Enter', charCode: 13 });

        expect(screen.getByText('Enter message')).toBeInTheDocument();
    });

    it('shows stats section when showStats is true', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} showStats={true} />);
        expect(screen.getByText('Статистика трансляции')).toBeInTheDocument();
    });

    it('shows settings menu when settings button is clicked', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        fireEvent.click(screen.getByTitle('Настройки трансляции'));

        expect(screen.getByText('Настройки трансляции')).toBeInTheDocument();
        expect(screen.getByText('Качество стрима')).toBeInTheDocument();
        expect(screen.getByText('Аудио настройки')).toBeInTheDocument();
    });

    it('changes stream quality when quality button is clicked', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        fireEvent.click(screen.getByTitle('Настройки трансляции'));

        fireEvent.click(screen.getByText('Высокое'));
        // After changing quality, the current quality display should update
        expect(screen.getByText('1080p')).toBeInTheDocument();
    });

    it('shows video settings for video broadcasts', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        fireEvent.click(screen.getByTitle('Настройки трансляции'));

        expect(screen.getByText('Видео настройки')).toBeInTheDocument();
    });

    it('hides video settings for audio broadcasts', () => {
        render(<EnhancedLiveBroadcastView broadcast={{ ...mockBroadcast, type: 'audio' }} />);
        fireEvent.click(screen.getByTitle('Настройки трансляции'));

        expect(screen.queryByText('Видео настройки')).not.toBeInTheDocument();
    });

    it('shows demo mode notice', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText(/Демо режим/)).toBeInTheDocument();
    });

    it('renders player controls', () => {
        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} />);
        expect(screen.getByText('⏸️ Пауза')).toBeInTheDocument();
        expect(screen.getByText('🔇 Выкл. звук')).toBeInTheDocument();
    });

    it('handles end broadcast via organizer button', () => {
        const mockOnEnd = jest.fn();
        const mockConfirm = jest.fn();
        global.confirm = jest.fn(() => true);

        render(<EnhancedLiveBroadcastView broadcast={mockBroadcast} isOrganizer={true} onEndBroadcast={mockOnEnd} />);
        fireEvent.click(screen.getByText('🔴 Завершить'));

        expect(mockOnEnd).toHaveBeenCalled();
    });
});
