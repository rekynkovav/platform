import React, { Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import App from './App';
import { ThemeProvider } from './hooks/useTheme';
import ErrorBoundary from './components/common/ErrorBoundary';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <ThemeProvider>
        <ErrorBoundary>
            <Suspense fallback={null}>
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </Suspense>
        </ErrorBoundary>
    </ThemeProvider>
);
