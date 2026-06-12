import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import InfoModal from '../components/common/InfoModal';

describe('InfoModal', () => {
    const defaultProps = {
        isOpen: true,
        title: 'Информация',
        message: 'Тестовое сообщение',
        onClose: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders nothing when isOpen is false', () => {
        const { container } = render(<InfoModal {...defaultProps} isOpen={false} />);
        expect(container.innerHTML).toBe('');
    });

    it('renders title and message when open', () => {
        render(<InfoModal {...defaultProps} />);
        expect(screen.getByText('Информация')).toBeInTheDocument();
        expect(screen.getByText('Тестовое сообщение')).toBeInTheDocument();
    });

    it('renders close button', () => {
        render(<InfoModal {...defaultProps} />);
        expect(screen.getByText('Закрыть')).toBeInTheDocument();
    });

    it('calls onClose when close button is clicked', () => {
        render(<InfoModal {...defaultProps} />);
        screen.getByText('Закрыть').click();
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('calls onClose when overlay is clicked', () => {
        render(<InfoModal {...defaultProps} />);
        const overlay = document.querySelector('.modal-overlay');
        fireEvent.click(overlay);
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('does not call onClose when modal content is clicked', () => {
        render(<InfoModal {...defaultProps} />);
        const content = document.querySelector('.modal-content');
        fireEvent.click(content);
        expect(defaultProps.onClose).not.toHaveBeenCalled();
    });

    it('uses default title when not provided', () => {
        render(<InfoModal isOpen={true} message="Test" onClose={jest.fn()} />);
        expect(screen.getByText('Информация')).toBeInTheDocument();
    });
});
