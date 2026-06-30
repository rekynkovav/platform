import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { useTheme, ThemeProvider } from '../hooks/useTheme';

// Test component that uses the hook
const TestComponent = () => {
    const { theme } = useTheme();
    return <div data-testid="theme-value">{theme}</div>;
};

describe('useTheme', () => {
    it('throws error when used outside ThemeProvider', () => {
        const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
        
        // Suppress React error boundary console output
        expect(() => {
            render(<TestComponent />);
        }).toThrow('useTheme must be used within ThemeProvider');
        
        spy.mockRestore();
    });

    it('provides dark theme via ThemeProvider', () => {
        render(
            <ThemeProvider>
                <TestComponent />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme-value')).toHaveTextContent('dark');
    });

    it('sets data-theme attribute on document element', () => {
        render(
            <ThemeProvider>
                <TestComponent />
            </ThemeProvider>
        );

        expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('always provides dark theme', () => {
        render(
            <ThemeProvider>
                <TestComponent />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme-value')).toHaveTextContent('dark');
        // Theme is always dark, there is no toggle
    });

    it('renders children correctly', () => {
        render(
            <ThemeProvider>
                <div data-testid="child">Hello</div>
            </ThemeProvider>
        );

        expect(screen.getByTestId('child')).toHaveTextContent('Hello');
    });

    it('nested components can access theme', () => {
        const NestedComponent = () => {
            const { theme } = useTheme();
            return <span data-testid="nested-theme">{theme}</span>;
        };

        render(
            <ThemeProvider>
                <div>
                    <TestComponent />
                    <NestedComponent />
                </div>
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme-value')).toHaveTextContent('dark');
        expect(screen.getByTestId('nested-theme')).toHaveTextContent('dark');
    });
});
