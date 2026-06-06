import React from 'react';

/**
 * Универсальное модальное окно подтверждения.
 * Принимает:
 *   - isOpen: boolean
 *   - title: строка (заголовок)
 *   - message: строка (текст)
 *   - itemLabel: строка (название удаляемого элемента)
 *   - onConfirm: функция
 *   - onCancel: функция
 *   - confirmText: строка (по умолчанию "Да, удалить")
 *   - cancelText: строка (по умолчанию "Отмена")
 */
const ConfirmModal = ({
    isOpen,
    title = 'Подтверждение удаления',
    message = 'Вы действительно хотите удалить этот элемент?',
    itemLabel = '',
    onConfirm,
    onCancel,
    confirmText = '✅ Да, удалить',
    cancelText = '❌ Отмена'
}) => {
    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onCancel}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <span className="modal-icon">🗑️</span>
                    <h3>{title}</h3>
                </div>
                <div className="modal-body">
                    <p>{message}</p>
                    {itemLabel && (
                        <div className="item-preview">
                            <strong>{itemLabel}</strong>
                        </div>
                    )}
                    <p className="warning-text">Это действие невозможно отменить!</p>
                </div>
                <div className="modal-footer">
                    <button className="modal-btn cancel" onClick={onCancel}>
                        {cancelText}
                    </button>
                    <button className="modal-btn confirm" onClick={onConfirm}>
                        {confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmModal;
