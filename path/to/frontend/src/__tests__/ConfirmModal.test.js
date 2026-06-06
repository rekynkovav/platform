import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ConfirmModal from '../components/common/ConfirmModal';

describe('ConfirmModal', () => {
    const defaultProps = {
        isOpen: true,
        title: 'Delete item',
        message: 'Are you sure?',
        itemLabel: 'Test Item',
        onConfirm: jest.fn(),
        onCancel: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders nothing when not open', () => {
        render(<ConfirmModal {...defaultProps} isOpen={false} />);
        expect(screen.queryByText('Delete item')).not.toBeInTheDocument();
    });

    it('renders title and message', () => {
        render(<ConfirmModal {...defaultProps} />);
        expect(screen.getByText('Delete item')).toBeInTheDocument();
        expect(screen.getByText('Are you sure?')).toBeInTheDocument();
        expect(screen.getByText(/Test Item/)).toBeInTheDocument();
    });

    it('calls onConfirm when confirm button clicked', () => {
        render(<ConfirmModal {...defaultProps} />);
        const confirmBtn = screen.getByText('Да');
        fireEvent.click(confirmBtn);
        expect(defaultProps.onConfirm).toHaveBeenCalled();
    });

    it('calls onCancel when cancel button clicked', () => {
        render(<ConfirmModal {...defaultProps} />);
        const cancelBtn = screen.getByText('Нет');
        fireEvent.click(cancelBtn);
        expect(defaultProps.onCancel).toHaveBeenCalled();
    });
});
