import React from 'react';

/**
 * Простое модальное окно с сообщением и кнопкой закрытия.
 * Принимает:
 *   - isOpen: boolean
 *   - title: строка (заголовок)
 *   - message: строка (текст)
 *   - onClose: функция
 */
const InfoModal = ({ isOpen, title = 'Информация', message = '', onClose }) => {
    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <span className="modal-icon">ℹ️</span>
                    <h3>{title}</h3>
                </div>
                <div className="modal-body">
                    <p>{message}</p>
                </div>
                <div className="modal-footer">
                    <button className="modal-btn confirm" onClick={onClose}>
                        Закрыть
                    </button>
                </div>
            </div>
        </div>
    );
};

export default InfoModal;
