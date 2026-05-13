import React from 'react';
import './Dashboard.css';

const Dashboard = () => {
    return (
        <div className="dashboard">
            <h2>📊 Личный дашборд</h2>

            <div className="dashboard-content">
                <div className="welcome-section">
                    <h3>Добро пожаловать в «Путь Сердца»!</h3>
                    <p>Здесь собрано всё самое важное: предстоящие события, свежие публикации, статистика и быстрые действия.</p>
                </div>

                <div className="dashboard-grid">
                    {/* Блок ближайшего события */}
                    <div className="dashboard-card">
                        <h4>📅 Ближайшее событие</h4>
                        <p className="placeholder">Пока нет запланированных событий</p>
                    </div>

                    {/* Блок статистики */}
                    <div className="dashboard-card">
                        <h4>📈 Ваша активность</h4>
                        <p className="placeholder">Сообщений в чате: 0<br />Просмотрено событий: 0</p>
                    </div>

                    {/* Быстрые действия */}
                    <div className="dashboard-card">
                        <h4>⚡ Быстрые действия</h4>
                        <button className="quick-action-btn">Создать событие</button>
                        <button className="quick-action-btn">Написать в чат</button>
                        <button className="quick-action-btn">Открыть ленту</button>
                    </div>

                    {/* Последние посты */}
                    <div className="dashboard-card">
                        <h4>📰 Последние публикации</h4>
                        <p className="placeholder">Лента будет отображаться здесь</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;