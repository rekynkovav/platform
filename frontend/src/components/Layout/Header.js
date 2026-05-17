// components/Layout/Header.js
import React from 'react';
import ThemeToggle from './ThemeToggle';
import { getUsername, logout } from '../../keycloak';
import './Header.css';

const Header = () => {
    const username = getUsername();

    return (
        <header className="header">
            <div className="header-left">
                <h1 className="header-logo">PLATFORM</h1>
            </div>
            <div className="header-right">
                <span className="header-username">{username}</span>
                <ThemeToggle />
                <button className="header-logout" onClick={logout} title="Выйти">
                    🚪
                </button>
            </div>
        </header>
    );
};

export default Header;