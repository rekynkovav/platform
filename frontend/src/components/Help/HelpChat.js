import React from 'react';
import './HelpChat.css';

const HelpChat = () => {
    return (
        <div className="help-chat">
            <h2>💬 Чат поддержки</h2>

            <div className="help-chat-container">
                <div className="support-info">
                    <div className="support-icon">🛟</div>
                    <h3>Нужна помощь?</h3>
                    <p>Здесь вы можете задать любой вопрос администраторам, консультантам или разработчикам платформы.</p>
                </div>

                <div className="help-chat-placeholder">
                    <p className="placeholder-text">
                        Полноценный чат поддержки будет реализован в следующих порциях.
                    </p>
                    <p className="alternative">
                        Пока вы можете написать в <strong>Общий чат</strong> или воспользоваться разделом «Вопросы».
                    </p>
                </div>
            </div>
        </div>
    );
};

export default HelpChat;