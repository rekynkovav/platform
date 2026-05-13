import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { AudioPlayerProvider } from './contexts/AudioPlayerContext';
import './index.css';

const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
    <React.StrictMode>
        <AudioPlayerProvider>
            <App />
        </AudioPlayerProvider>
    </React.StrictMode>
);