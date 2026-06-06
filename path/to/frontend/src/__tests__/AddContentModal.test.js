import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import AddContentModal from '../components/common/AddContentModal';

describe('AddContentModal', () => {
    const defaultProps = {
        isOpen: true,
        onClose: jest.fn(),
        editingItem: null,
        selectedMenuId: 'test',
        newItemTitle: '',
        setNewItemTitle: jest.fn(),
        newItemDescription: '',
        setNewItemDescription: jest.fn(),
        newItemMediaUrl: '',
        setNewItemMediaUrl: jest.fn(),
        newItemMediaType: '',
        setNewItemMediaType: jest.fn(),
        isRecording: false,
        recordingTime: 0,
        handleStartRecording: jest.fn(),
        handleStopRecording: jest.fn(),
        handleFileUpload: jest.fn(),
        handleSaveContent: jest.fn(),
        formatRecordingTime: jest.fn(() => '00:00'),
        resetRecording: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders nothing when not open', () => {
        render(<AddContentModal {...defaultProps} isOpen={false} />);
        expect(screen.queryByText('Добавить контент')).not.toBeInTheDocument();
    });

    it('renders form fields and save button', () => {
        render(<AddContentModal {...defaultProps} />);
        expect(screen.getByPlaceholderText('Название')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Описание')).toBeInTheDocument();
        expect(screen.getByText('Сохранить')).toBeInTheDocument();
    });

    it('calls handleSaveContent when save clicked', () => {
        render(<AddContentModal {...defaultProps} />);
        const saveBtn = screen.getByText('Сохранить');
        fireEvent.click(saveBtn);
        expect(defaultProps.handleSaveContent).toHaveBeenCalled();
    });

    it('calls onClose when close button clicked', () => {
        render(<AddContentModal {...defaultProps} />);
        const closeBtn = screen.getByText('Закрыть');
        fireEvent.click(closeBtn);
        expect(defaultProps.onClose).toHaveBeenCalled();
    });
});
