import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ErrorBoundary from '../components/common/ErrorBoundary';

// Компонент, который всегда выбрасывает ошибку
const BuggyComponent = () => {
    throw new Error('Test error');
};

// Компонент, который работает нормально
const GoodComponent = () => <div data-testid="good-component">Normal content</div>;

describe('ErrorBoundary', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(console, 'error').mockImplementation(() => {}); // Suppress console.error
    });

    afterEach(() => {
        console.error.mockRestore();
    });

    it('renders children when there is no error', () => {
        render(
            <ErrorBoundary>
                <GoodComponent />
            </ErrorBoundary>
        );
        expect(screen.getByTestId('good-component')).toBeInTheDocument();
        expect(screen.getByText('Normal content')).toBeInTheDocument();
    });

    it('renders fallback UI when child throws', () => {
        render(
            <ErrorBoundary>
                <BuggyComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('Something went wrong')).toBeInTheDocument();
        expect(screen.getByText('An unexpected error occurred')).toBeInTheDocument();
        expect(screen.getByText('Retry')).toBeInTheDocument();
    });

    it('renders custom fallback when provided', () => {
        const CustomFallback = <div data-testid="custom-fallback">Custom Error</div>;
        render(
            <ErrorBoundary fallback={CustomFallback}>
                <BuggyComponent />
            </ErrorBoundary>
        );

        expect(screen.getByTestId('custom-fallback')).toBeInTheDocument();
        expect(screen.getByText('Custom Error')).toBeInTheDocument();
    });

    it('calls componentDidCatch when error occurs', () => {
        const spy = jest.spyOn(ErrorBoundary.prototype, 'componentDidCatch');
        render(
            <ErrorBoundary>
                <BuggyComponent />
            </ErrorBoundary>
        );
        expect(spy).toHaveBeenCalled();
        spy.mockRestore();
    });
});
