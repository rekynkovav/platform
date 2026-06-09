import React from 'react';

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
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '400px', padding: '20px', textAlign: 'center' }}>
                    <h2>Something went wrong</h2>
                    <p style={{ color: '#666', marginBottom: '20px' }}>An unexpected error occurred</p>
                    <button
                        onClick={() => {
                            this.setState({ hasError: false, error: null });
                            window.location.reload();
                        }}
                        style={{ padding: '10px 24px', cursor: 'pointer' }}
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
