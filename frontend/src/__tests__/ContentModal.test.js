import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ContentModal from '../components/Common/ContentModal';

jest.mock('../components/Common/ContentModal.css', () => ({}));

describe('ContentModal', () => {
    const defaultProps = {
        isOpen: true,
        onClose: jest.fn(),
        onSave: jest.fn(),
        initialTitle: '',
        initialDescription: '',
    };

    beforeEach(() => {
        jest.clearAllMocks();
        URL.createObjectURL = jest.fn(() => 'blob:test');
        URL.revokeObjectURL = jest.fn();
    });

    it('renders nothing when isOpen is false', () => {
        const { container } = render(<ContentModal {...defaultProps} isOpen={false} />);
        expect(container.innerHTML).toBe('');
    });

    it('renders modal content when open', () => {
        render(<ContentModal {...defaultProps} />);
        expect(screen.getByText('Добавить контент')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Название')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Описание')).toBeInTheDocument();
    });

    it('calls onClose when cancel button is clicked', () => {
        render(<ContentModal {...defaultProps} />);
        fireEvent.click(screen.getByText('Отмена'));
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('calls onClose when overlay is clicked', () => {
        render(<ContentModal {...defaultProps} />);
        const overlay = document.querySelector('.content-modal-overlay');
        fireEvent.click(overlay);
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('does not call onClose when modal content is clicked', () => {
        render(<ContentModal {...defaultProps} />);
        const modal = document.querySelector('.content-modal');
        fireEvent.click(modal);
        expect(defaultProps.onClose).not.toHaveBeenCalled();
    });

    it('does not save when title is empty', () => {
        window.alert = jest.fn();
        render(<ContentModal {...defaultProps} />);
        fireEvent.click(screen.getByText('Сохранить'));
        expect(defaultProps.onSave).not.toHaveBeenCalled();
        expect(window.alert).toHaveBeenCalledWith('Заполните заголовок');
    });

    it('calls onSave with title and description', () => {
        render(<ContentModal {...defaultProps} />);
        fireEvent.change(screen.getByPlaceholderText('Название'), {
            target: { value: 'My Title' },
        });
        fireEvent.change(screen.getByPlaceholderText('Описание'), {
            target: { value: 'My Description' },
        });
        fireEvent.click(screen.getByText('Сохранить'));
        expect(defaultProps.onSave).toHaveBeenCalledWith(
            expect.objectContaining({ title: 'My Title', description: 'My Description' })
        );
    });

    it('shows attached file info for image type', () => {
        const { rerender } = render(<ContentModal {...defaultProps} />);
        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' });
        Object.defineProperty(fileInput, 'files', { value: [file] });
        fireEvent.change(fileInput);
        expect(screen.getByText('📷 Фото прикреплено')).toBeInTheDocument();
    });
});
