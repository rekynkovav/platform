import React, { useEffect } from 'react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import './Dashboard.css';

const Dashboard = () => {
    const {
        playTrack,
        currentTrack,
        isPlaying,
        stopTrack,
        volume,
        playbackRate,
        setVolume,
        setPlaybackRate,
        seek,
        currentTime,
        duration,
        formatTime
    } = useAudioPlayer();

    const broadcasts = [
        { id: 1, title: "Эфир - 01.01.2025", date: "1 января 2025", url: "/audio/efir-01-01-2025.mp3" },
        { id: 2, title: "Эфир - 13.01.2025", date: "13 января 2025", url: "/audio/efir-13-01-2025.mp3" },
        { id: 3, title: "Эфир - 22.01.2025", date: "22 января 2025", url: "/audio/efir-22-01-2025.mp3" },
        { id: 4, title: "Эфир - 31.01.2025", date: "31 января 2025", url: "/audio/efir-31-01-2025.mp3" }
    ];

    const importantMaterials = [
        { id: 1, title: "О стремлении человека не есть", description: "Понимание истинных потребностей тела и духа" },
        { id: 2, title: "Основа сила человека - дух", description: "Как развивать духовную силу" },
        { id: 3, title: "Значение движения", description: "Почему движение - это жизнь" },
        { id: 4, title: "Дыхание - основа жизни человека", description: "Практики правильного дыхания" },
        { id: 5, title: "О силе человека", description: "Источники внутренней силы" }
    ];

    const channels = [
        { name: "Путь Сердца", type: "Главный канал" },
        { name: "Путь Сердца - Чат", type: "Прикрепленная группа для общения" },
        { name: "Эволюция Питания (Путь Сердца)", type: "Канал по питанию" }
    ];

    const handlePlayAudio = (broadcast) => {
        if (currentTrack?.id === broadcast.id && isPlaying) {
            stopTrack();
        } else {
            playTrack({ id: broadcast.id, title: broadcast.title, url: broadcast.url });
        }
    };

    const handleSeek = (e, broadcastId) => {
        const newTime = parseFloat(e.target.value);
        if (currentTrack?.id === broadcastId) {
            seek(newTime);
        }
    };

    const handleVolumeChangeLocal = (e, broadcastId) => {
        const newVolume = parseFloat(e.target.value);
        if (currentTrack?.id === broadcastId) {
            setVolume(newVolume);
        }
    };

    const handleSpeedChangeLocal = (e, broadcastId) => {
        const newSpeed = parseFloat(e.target.value);
        if (currentTrack?.id === broadcastId) {
            setPlaybackRate(newSpeed);
        }
    };

    const getCurrentTimeForBroadcast = (broadcastId) => {
        if (currentTrack?.id === broadcastId) {
            return currentTime;
        }
        return 0;
    };

    const getDurationForBroadcast = (broadcastId) => {
        if (currentTrack?.id === broadcastId) {
            return duration;
        }
        return 0;
    };

    const isThisPlaying = (broadcastId) => {
        return currentTrack?.id === broadcastId && isPlaying;
    };

    return (
        <div className="dashboard">
            <div className="hero-section">
                <h1 className="hero-title">Глобальный Проект "Путь Сердца"</h1>
                <p className="hero-subtitle">Основатель - Вячеслав Тимошенко</p>
            </div>

            <div className="navigation-header">
                <h2>НАВИГАЦИЯ | ПРОЕКТ «Путь Сердца»</h2>
                <div className="meta-info">
                    <span>DB • 16 января 2025 в 0:15</span>
                </div>
            </div>

            <div className="about-section">
                <h3>О нас</h3>
                <div className="about-content">
                    <p><strong>О проекте "Путь Сердца"</strong></p>
                    <p>Мы есть ДУХ! - Мы есть СИЛА!</p>
                    <p>Наша Миссия "Путь Сердца" - Команда</p>
                    <p className="highlight">Мы создаем с вами НОВЫЙ МИР! Уже СЕЙЧАС!</p>
                </div>
            </div>

            <div className="newbie-section">
                <div className="newbie-badge">🔴 Инструкция для НОВИЧКОВ</div>
                <p>Добро пожаловать в проект! Начните с ознакомления с основными материалами ниже.</p>
            </div>

            <div className="channels-section">
                <h3>Какие каналы существуют?</h3>
                <div className="channels-list">
                    {channels.map((channel, idx) => (
                        <div key={idx} className="channel-item">
                            <span className="channel-name">{channel.name}</span>
                            <span className="channel-type">{channel.type}</span>
                        </div>
                    ))}
                </div>
            </div>

            <div className="materials-section">
                <h3>Важные материалы</h3>
                <div className="materials-grid">
                    {importantMaterials.map((material) => (
                        <div key={material.id} className="material-card">
                            <h4>{material.id}) {material.title}</h4>
                            <p>{material.description}</p>
                        </div>
                    ))}
                </div>
            </div>

            <div className="broadcasts-section">
                <h3>ЭФИРЫ</h3>
                <div className="broadcasts-list">
                    {broadcasts.map((broadcast) => (
                        <div key={broadcast.id} className="broadcast-item">
                            <div className="broadcast-info">
                                <span className="broadcast-title">{broadcast.title}</span>
                                <span className="broadcast-date">{broadcast.date}</span>
                            </div>
                            <div className="broadcast-controls">
                                {currentTrack?.id === broadcast.id ? (
                                    <div className="inline-audio-player">
                                        <div className="inline-audio-header">
                                            <span className="inline-audio-title">🎵 {broadcast.title}</span>
                                            <button className="inline-stop-btn" onClick={() => stopTrack()}>
                                                ✕
                                            </button>
                                        </div>
                                        <div className="inline-progress">
                                            <span>{formatTime(getCurrentTimeForBroadcast(broadcast.id))}</span>
                                            <input
                                                type="range"
                                                className="inline-seek"
                                                min="0"
                                                max={getDurationForBroadcast(broadcast.id) || 100}
                                                value={getCurrentTimeForBroadcast(broadcast.id)}
                                                onChange={(e) => handleSeek(e, broadcast.id)}
                                            />
                                            <span>{formatTime(getDurationForBroadcast(broadcast.id))}</span>
                                        </div>
                                        <div className="inline-controls">
                                            <button className="inline-play" onClick={() => handlePlayAudio(broadcast)}>
                                                {isThisPlaying(broadcast.id) ? '⏸️' : '▶️'}
                                            </button>
                                            <div className="inline-volume">
                                                <span>🔊</span>
                                                <input
                                                    type="range"
                                                    min="0"
                                                    max="1"
                                                    step="0.01"
                                                    value={volume}
                                                    onChange={(e) => handleVolumeChangeLocal(e, broadcast.id)}
                                                />
                                            </div>
                                            <div className="inline-speed">
                                                <span>⚡</span>
                                                <select value={playbackRate} onChange={(e) => handleSpeedChangeLocal(e, broadcast.id)}>
                                                    <option value="0.5">0.5x</option>
                                                    <option value="0.75">0.75x</option>
                                                    <option value="1.0">1.0x</option>
                                                    <option value="1.25">1.25x</option>
                                                    <option value="1.5">1.5x</option>
                                                    <option value="2.0">2.0x</option>
                                                </select>
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    <button className="play-button" onClick={() => handlePlayAudio(broadcast)}>
                                        ▶️ Слушать
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Dashboard;