import React from 'react';
import './HelpCenter.css';

const HelpCenter = () => {
    return (
        <div className="help-center">
            <h2>Центр помощи</h2>
            <p>Здесь вы можете найти ответы на часто задаваемые вопросы и связаться с поддержкой.</p>

            <div className="help-sections">
                <div className="help-card">
                    <h3>Часто задаваемые вопросы</h3>
                    <p>Скоро здесь появится FAQ</p>
                </div>
                <div className="help-card">
                    <h3>Техническая поддержка</h3>
                    <p>Для технических вопросов пишите в чат поддержки</p>
                </div>
            </div>
        </div>
    );
};

export default HelpCenter;