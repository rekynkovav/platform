import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import { ThemeProvider } from './hooks/useTheme';
import { AudioPlayerProvider } from './context/AudioPlayerContext';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <ThemeProvider>
        <AudioPlayerProvider>
            <App />
        </AudioPlayerProvider>
    </ThemeProvider>
);