import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import InfoModal from '../components/common/InfoModal';

describe('InfoModal', () => {
    it('renders nothing when not open', () => {
        render(<InfoModal isOpen={false} title="Info" message="Test" onClose={jest.fn()} />);
        expect(screen.queryByText('Info')).not.toBeInTheDocument();
    });

    it('renders title and message', () => {
        render(<InfoModal isOpen={true} title="Info Title" message="Message text" onClose={jest.fn()} />);
        expect(screen.getByText('Info Title')).toBeInTheDocument();
        expect(screen.getByText('Message text')).toBeInTheDocument();
    });

    it('calls onClose when close button clicked', () => {
        const onClose = jest.fn();
        render(<InfoModal isOpen={true} title="Info" message="Test" onClose={onClose} />);
        const closeBtn = screen.getByText('Закрыть');
        fireEvent.click(closeBtn);
        expect(onClose).toHaveBeenCalled();
    });
});
