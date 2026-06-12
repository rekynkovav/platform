import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ConfirmModal from '../components/common/ConfirmModal';

describe('ConfirmModal', () => {
    const defaultProps = {
        isOpen: true,
        title: 'Подтверждение удаления',
        message: 'Вы действительно хотите удалить этот элемент?',
        onConfirm: jest.fn(),
        onCancel: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders nothing when isOpen is false', () => {
        const { container } = render(<ConfirmModal {...defaultProps} isOpen={false} />);
        expect(container.innerHTML).toBe('');
    });

    it('renders title and message when open', () => {
        render(<ConfirmModal {...defaultProps} />);
        expect(screen.getByText('Подтверждение удаления')).toBeInTheDocument();
        expect(screen.getByText('Вы действительно хотите удалить этот элемент?')).toBeInTheDocument();
    });

    it('renders default button texts', () => {
        render(<ConfirmModal {...defaultProps} />);
        expect(screen.getByText('✅ Да, удалить')).toBeInTheDocument();
        expect(screen.getByText('❌ Отмена')).toBeInTheDocument();
    });

    it('renders custom button texts', () => {
        render(
            <ConfirmModal
                {...defaultProps}
                confirmText="Да"
                cancelText="Нет"
            />
        );
        expect(screen.getByText('Да')).toBeInTheDocument();
        expect(screen.getByText('Нет')).toBeInTheDocument();
    });

    it('renders itemLabel when provided', () => {
        render(<ConfirmModal {...defaultProps} itemLabel="Test Item" />);
        expect(screen.getByText('Test Item')).toBeInTheDocument();
    });

    it('calls onConfirm when confirm button is clicked', () => {
        render(<ConfirmModal {...defaultProps} />);
        screen.getByText('✅ Да, удалить').click();
        expect(defaultProps.onConfirm).toHaveBeenCalled();
    });

    it('calls onCancel when cancel button is clicked', () => {
        render(<ConfirmModal {...defaultProps} />);
        screen.getByText('❌ Отмена').click();
        expect(defaultProps.onCancel).toHaveBeenCalled();
    });

    it('calls onCancel when overlay is clicked', () => {
        render(<ConfirmModal {...defaultProps} />);

        const overlay = document.querySelector('.modal-overlay');
        fireEvent.click(overlay);
        expect(defaultProps.onCancel).toHaveBeenCalled();
    });

    it('does not call onCancel when modal content is clicked', () => {
        render(<ConfirmModal {...defaultProps} />);

        const content = document.querySelector('.modal-content');
        fireEvent.click(content);
        expect(defaultProps.onCancel).not.toHaveBeenCalled();
    });

    it('shows warning text', () => {
        render(<ConfirmModal {...defaultProps} />);
        expect(screen.getByText('Это действие невозможно отменить!')).toBeInTheDocument();
    });
});
