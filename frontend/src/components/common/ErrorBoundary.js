import React from 'react';
import './ErrorBoundary.css';

/**
 * ErrorBoundary — React error boundary component that catches JavaScript errors
 * in its child component tree and displays a fallback UI instead of crashing.
 *
 * Implements the standard React error boundary lifecycle methods:
 * - getDerivedStateFromError: updates state to trigger fallback rendering
 * - componentDidCatch: logs error details to the console
 *
 * @param {React.ReactNode} props.children - Child components to wrap
 * @param {React.ReactNode} [props.fallback] - Custom fallback UI to render on error (optional)
 * @returns {React.ReactNode} Either the children or the fallback UI
 */
class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error('ErrorBoundary caught:', error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            const fallback = this.props.fallback || (
                <div className="error-fallback">
                    <h2>Something went wrong</h2>
                    <p className="error-fallback-message">An unexpected error occurred</p>
                    <button
                        onClick={() => {
                            this.setState({ hasError: false, error: null });
                            window.location.reload();
                        }}
                        className="error-fallback-retry"
                    >
                        Retry
                    </button>
                </div>
            );
            return fallback;
        }
        return this.props.children;
    }
}

export default ErrorBoundary;
