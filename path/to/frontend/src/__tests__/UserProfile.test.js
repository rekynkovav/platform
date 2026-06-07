import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UserProfile from '../components/UserProfile/UserProfile';
import { userApi, paymentApi } from '../services/api';
import { getUsername, getUserRoles } from '../keycloak';

// Моки
jest.mock('../services/api', () => ({
    userApi: {
        getMe: jest.fn(),
        updateMe: jest.fn(),
    },
    paymentApi: {
        getUserPayments: jest.fn(),
        create: jest.fn(),
        getPaymentQR: jest.fn(),
    },
}));

jest.mock('../keycloak', () => ({
    getUsername: jest.fn(),
    getUserRoles: jest.fn(),
}));

jest.mock('../components/UserProfile/UserProfile.css', () => ({}));

// Мок для InfoModal
jest.mock('../components/common/InfoModal', () => {
    return function MockInfoModal({ isOpen, title, message, onClose }) {
        if (!isOpen) return null;
        return (
            <div data-testid="info-modal">
                <span>{title}</span>
                <span>{message}</span>
                <button data-testid="info-close" onClick={onClose}>Закрыть</button>
            </div>
        );
    };
});

describe('UserProfile', () => {
    const mockUser = {
        username: 'testuser',
        email: 'test@example.com',
        phone: '+1234567890',
        bio: 'Test bio',
        createdAt: '2025-01-01T12:00:00Z',
    };

    const mockPayments = [
        { id: '1', amount: 1500, currency: 'RUB', status: 'COMPLETED', subscriptionMonths: 1, createdAt: '2025-01-15T12:00:00Z' },
        { id: '2', amount: 4000, currency: 'RUB', status: 'PENDING', subscriptionMonths: 3, createdAt: '2025-02-01T12:00:00Z' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        userApi.getMe.mockResolvedValue(mockUser);
        paymentApi.getUserPayments.mockResolvedValue(mockPayments);
    });

    it('renders loading state and then user data', async () => {
        render(<UserProfile />);

        expect(screen.getByText('Loading...')).toBeInTheDocument();

        await waitFor(() => {
            expect(screen.getByText('Profile: testuser')).toBeInTheDocument();
            expect(screen.getByText('test@example.com')).toBeInTheDocument();
            expect(screen.getByText('+1234567890')).toBeInTheDocument();
            expect(screen.getByText('Test bio')).toBeInTheDocument();
        });
    });

    it('displays user roles', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Roles: USER_0')).toBeInTheDocument();
        });
    });

    it('shows edit form when Edit Profile is clicked', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Edit Profile')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Edit Profile'));

        expect(screen.getByDisplayValue('testuser')).toBeInTheDocument();
        expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
        expect(screen.getByDisplayValue('+1234567890')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Test bio')).toBeInTheDocument();
    });

    it('saves edited profile', async () => {
        userApi.updateMe.mockResolvedValue({ ...mockUser, username: 'updateduser' });
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Edit Profile')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Edit Profile'));

        const usernameInput = screen.getByDisplayValue('testuser');
        fireEvent.change(usernameInput, { target: { value: 'updateduser' } });

        fireEvent.click(screen.getByText('Save'));

        await waitFor(() => {
            expect(userApi.updateMe).toHaveBeenCalledWith(expect.objectContaining({ username: 'updateduser' }));
            expect(screen.getByText('Profile: updateduser')).toBeInTheDocument();
        });
    });

    it('displays payment history', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('1500 RUB')).toBeInTheDocument();
            expect(screen.getByText('4000 RUB')).toBeInTheDocument();
            expect(screen.getByText('COMPLETED')).toBeInTheDocument();
            expect(screen.getByText('PENDING')).toBeInTheDocument();
        });
    });

    it('creates a payment and shows QR code', async () => {
        paymentApi.create.mockResolvedValue({ id: '3', amount: 1500, expiresAt: '2025-03-01T12:00:00Z' });
        paymentApi.getPaymentQR.mockResolvedValue({ qrCodeBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==' });

        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('1 month - 1500 RUB')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('1 month - 1500 RUB'));

        await waitFor(() => {
            expect(paymentApi.create).toHaveBeenCalledWith({ subscriptionMonths: 1 });
            expect(screen.getByText('Оплата подписки')).toBeInTheDocument();
            expect(screen.getByText('Сумма: 1500 RUB')).toBeInTheDocument();
        });
    });

    it('shows error when API fails', async () => {
        userApi.getMe.mockRejectedValue(new Error('Network error'));
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить данные пользователя')).toBeInTheDocument();
        });
    });

    it('shows error when payment creation fails', async () => {
        paymentApi.create.mockRejectedValue(new Error('Payment error'));
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('1 month - 1500 RUB')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('1 month - 1500 RUB'));

        await waitFor(() => {
            expect(screen.getByText('Не удалось создать платеж')).toBeInTheDocument();
        });
    });
});
