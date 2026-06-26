import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NavTabs from '../components/Navigation/NavTabs';

jest.mock('../components/Navigation/NavTabs.css', () => ({}));

describe('NavTabs', () => {
    it('renders all base tabs', () => {
        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );
        expect(screen.getByText('Лента Новостей')).toBeInTheDocument();
        expect(screen.getByText('Трансляции')).toBeInTheDocument();
        expect(screen.getByText('События')).toBeInTheDocument();
        expect(screen.getByText('Навигация')).toBeInTheDocument();
        expect(screen.getByText('Чат')).toBeInTheDocument();
        expect(screen.getByText('Помощь')).toBeInTheDocument();
    });

    it('highlights active tab based on URL', () => {
        render(
            <MemoryRouter initialEntries={['/chat']}>
                <NavTabs />
            </MemoryRouter>
        );
        const chatTab = screen.getByText('Чат');
        expect(chatTab.classList.contains('active')).toBe(true);
    });

    it('navigates when tab is clicked', () => {
        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );
        fireEvent.click(screen.getByText('Помощь'));
        // After click, URL should change — check that the help tab is now active
        expect(screen.getByText('Помощь').classList.contains('active')).toBe(true);
    });

    it('shows active class on the correct tab', () => {
        render(
            <MemoryRouter initialEntries={['/archive']}>
                <NavTabs />
            </MemoryRouter>
        );
        const archiveTab = screen.getByText('Трансляции');
        expect(archiveTab.classList.contains('active')).toBe(true);
        expect(screen.getByText('Лента Новостей').classList.contains('active')).toBe(false);
    });

    it('renders split tabs for feed on hover', () => {
        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );
        const feedTab = screen.getByText('Лента Новостей');
        const wrapper = feedTab.closest('.nav-tab-wrapper');
        fireEvent.mouseEnter(wrapper);
        expect(screen.getByText('Питание')).toBeInTheDocument();
        expect(screen.getByText('Новости')).toBeInTheDocument();
    });

    it('renders split tabs for chat on hover', () => {
        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );
        const chatTab = screen.getByText('Чат');
        const wrapper = chatTab.closest('.nav-tab-wrapper');
        fireEvent.mouseEnter(wrapper);
        expect(screen.getByText('Основной')).toBeInTheDocument();
    });

    it('handles nutrition sub-tab as active', () => {
        render(
            <MemoryRouter initialEntries={['/nutrition']}>
                <NavTabs />
            </MemoryRouter>
        );
        expect(screen.getByText('Лента Новостей').closest('.nav-tab-wrapper').classList.contains('active')).toBe(true);
    });

    it('handles chat-nutrition sub-tab as active', () => {
        render(
            <MemoryRouter initialEntries={['/chat/nutrition']}>
                <NavTabs />
            </MemoryRouter>
        );
        expect(screen.getByText('Чат').closest('.nav-tab-wrapper').classList.contains('active')).toBe(true);
    });
});
