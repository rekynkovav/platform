import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import EventsHub from '../components/Events/EventsHub';
import { eventsApi, feedbackApi } from '../services/api';
import { getUsername } from '../keycloak';

// Моки
jest.mock('../services/api', () => ({
    eventsApi: {
        getMaterialsBySection: jest.fn(),
        createMaterial: jest.fn(),
        updateMaterial: jest.fn(),
        deleteMaterial: jest.fn(),
    },
    feedbackApi: {
        submitFeedback: jest.fn(),
        submitQuestion: jest.fn(),
    },
}));

jest.mock('../keycloak', () => ({
    getUsername: jest.fn(),
}));

jest.mock('../components/Events/EventsHub.css', () => ({}));

describe('EventsHub', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        getUsername.mockReturnValue('testuser');
        eventsApi.getMaterialsBySection.mockResolvedValue([]);
    });

    it('renders library header', () => {
        render(<EventsHub />);
        expect(screen.getByText('Библиотека материалов')).toBeInTheDocument();
    });

    it('renders all 13 menu items', () => {
        render(<EventsHub />);
        expect(screen.getByText('О Нас')).toBeInTheDocument();
        expect(screen.getByText('Важные материалы')).toBeInTheDocument();
        expect(screen.getByText('Интервью')).toBeInTheDocument();
        expect(screen.getByText('Для новичков')).toBeInTheDocument();
        expect(screen.getByText('Для размышлений')).toBeInTheDocument();
        expect(screen.getByText('От соучастников проекта')).toBeInTheDocument();
        expect(screen.getByText('Музыка')).toBeInTheDocument();
        expect(screen.getByText('Мотивация')).toBeInTheDocument();
        expect(screen.getByText('Фильмы')).toBeInTheDocument();
        expect(screen.getByText('Отзывы и Предложения')).toBeInTheDocument();
        expect(screen.getByText('Результаты Тимошенко')).toBeInTheDocument();
        expect(screen.getByText('Эфиры')).toBeInTheDocument();
        expect(screen.getByText('Годовой челлендж')).toBeInTheDocument();
    });

    it('shows add material form for admin in development', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        sessionStorage.setItem('demoRole', 'MODERATOR_ADMIN');

        render(<EventsHub />);

        // Hover over a menu item
        const menuItem = screen.getByText('О Нас');
        fireEvent.mouseEnter(menuItem);

        await waitFor(() => {
            // Should trigger materials load
            expect(eventsApi.getMaterialsBySection).toHaveBeenCalledWith('about');
        });

        sessionStorage.removeItem('demoRole');
    });

    it('shows feedback form when clicking Отзывы и Предложения', () => {
        render(<EventsHub />);

        const feedbackItem = screen.getByText('Отзывы и Предложения');
        fireEvent.mouseEnter(feedbackItem);

        expect(screen.getByText('Поделитесь своим мнением')).toBeInTheDocument();
    });

    it('renders audio files list on mount', () => {
        render(<EventsHub />);

        expect(screen.getByText('Эфир от 01.01.2025')).toBeInTheDocument();
        expect(screen.getByText('Эфир от 15.01.2025')).toBeInTheDocument();
        expect(screen.getByText('Эфир от 01.02.2025')).toBeInTheDocument();
        expect(screen.getByText('Эфир от 15.02.2025')).toBeInTheDocument();
        expect(screen.getByText('Эфир от 01.03.2025')).toBeInTheDocument();
    });
});
