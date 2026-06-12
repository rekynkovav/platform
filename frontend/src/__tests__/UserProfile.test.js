import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UserProfile from '../components/UserProfile/UserProfile';
import { userApi, paymentApi } from '../services/api';
import { getUsername, getUserRoles } from '../keycloak';

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

jest.mock('../components/common/InfoModal', () => {
    return function MockInfoModal({ isOpen, title, message, onClose }) {
        if (!isOpen) return null;
        return (
            <div data-testid="info-modal">
                <span>{title}</span>
                <span>{message}</span>
                <button data-testid="info-close" onClick={onClose}>Close</button>
            </div>
        );
    };
});

jest.mock('../components/UserProfile/UserProfile.css', () => ({}));

describe('UserProfile', () => {
    const mockUser = {
        username: 'testuser',
        email: 'test@example.com',
        phone: '+123****7890',
        bio: 'Test bio',
        createdAt: '2025-01-01T00:00:00Z',
    };
    const mockPayments = [
        { id: 1, amount: 1500, currency: 'RUB', status: 'completed', subscriptionMonths: 1, createdAt: '2025-01-01T00:00:00Z' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        userApi.getMe.mockResolvedValue(mockUser);
        paymentApi.getUserPayments.mockResolvedValue(mockPayments);
    });

    it('renders loading state initially', () => {
        userApi.getMe.mockReturnValue(new Promise(() => {}));
        render(<UserProfile />);
        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('renders user profile after loading', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Profile: testuser')).toBeInTheDocument();
            expect(screen.getByText(/test@example.com/)).toBeInTheDocument();
            expect(screen.getByText(/Test bio/)).toBeInTheDocument();
        });
    });

    it('renders user roles', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText(/USER_0/)).toBeInTheDocument();
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
    });

    it('calls updateMe and shows info modal on save', async () => {
        userApi.updateMe.mockResolvedValue(mockUser);

        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Edit Profile')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Edit Profile'));

        const saveButton = screen.getByText('Save');
        fireEvent.click(saveButton);

        await waitFor(() => {
            expect(userApi.updateMe).toHaveBeenCalled();
            expect(screen.getByTestId('info-modal')).toBeInTheDocument();
        });
    });

    it('cancels editing', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Edit Profile')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Edit Profile'));
        fireEvent.click(screen.getByText('Cancel'));

        expect(screen.queryByDisplayValue('testuser')).not.toBeInTheDocument();
    });

    it('renders payment history', async () => {
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('Payment History')).toBeInTheDocument();
        });
    });

    it('shows "No payments yet" when empty', async () => {
        paymentApi.getUserPayments.mockResolvedValue([]);
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('No payments yet')).toBeInTheDocument();
        });
    });

    it('shows "User not found" when API load fails', async () => {
        userApi.getMe.mockRejectedValue(new Error('Network error'));
        render(<UserProfile />);

        await waitFor(() => {
            expect(screen.getByText('User not found')).toBeInTheDocument();
        });
    });
});
