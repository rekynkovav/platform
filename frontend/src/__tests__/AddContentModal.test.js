import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import AddContentModal from '../components/common/AddContentModal';

describe('AddContentModal', () => {
    const defaultProps = {
        isOpen: true,
        onClose: jest.fn(),
        editingItem: null,
        selectedMenuId: null,
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

    it('renders nothing when isOpen is false', () => {
        const { container } = render(<AddContentModal {...defaultProps} isOpen={false} />);
        expect(container.innerHTML).toBe('');
    });

    it('renders add content form when open', () => {
        render(<AddContentModal {...defaultProps} />);
        expect(screen.getByText('Добавить контент')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Как будет отображаться')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Сюда вставить ссылку')).toBeInTheDocument();
    });

    it('renders edit title when editingItem is provided', () => {
        render(<AddContentModal {...defaultProps} editingItem={{ id: 1 }} />);
        expect(screen.getByText('Редактировать контент')).toBeInTheDocument();
    });

    it('calls onClose when cancel button is clicked', () => {
        render(<AddContentModal {...defaultProps} />);
        fireEvent.click(screen.getByText('Отмена'));
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('calls handleSaveContent when save button is clicked', () => {
        render(<AddContentModal {...defaultProps} />);
        fireEvent.click(screen.getByText('Сохранить'));
        expect(defaultProps.handleSaveContent).toHaveBeenCalled();
    });

    it('updates title on input change', () => {
        render(<AddContentModal {...defaultProps} />);
        const titleInput = screen.getByPlaceholderText('Как будет отображаться');
        fireEvent.change(titleInput, { target: { value: 'New Title' } });
        expect(defaultProps.setNewItemTitle).toHaveBeenCalledWith('New Title');
    });

    it('updates description on textarea change', () => {
        render(<AddContentModal {...defaultProps} />);
        const descInput = screen.getByPlaceholderText('Сюда вставить ссылку');
        fireEvent.change(descInput, { target: { value: 'New description' } });
        expect(defaultProps.setNewItemDescription).toHaveBeenCalledWith('New description');
    });

    it('shows media buttons for broadcasts menu', () => {
        render(<AddContentModal {...defaultProps} selectedMenuId="broadcasts" />);
        expect(screen.getByText('Видео')).toBeInTheDocument();
        expect(screen.getByText('Аудио')).toBeInTheDocument();
        expect(screen.getByText('🎤 Записать голос')).toBeInTheDocument();
    });

    it('shows audio button for music menu', () => {
        render(<AddContentModal {...defaultProps} selectedMenuId="music" />);
        expect(screen.getByText('Аудио')).toBeInTheDocument();
        expect(screen.getByText('🎤 Записать голос')).toBeInTheDocument();
    });

    it('shows video button for movies menu', () => {
        render(<AddContentModal {...defaultProps} selectedMenuId="movies" />);
        expect(screen.getByText('Видео')).toBeInTheDocument();
        expect(screen.getByText('🎤 Записать голос')).not.toBeInTheDocument();
    });

    it('shows all media types for other menus', () => {
        render(<AddContentModal {...defaultProps} selectedMenuId="about" />);
        expect(screen.getByText('Фото')).toBeInTheDocument();
        expect(screen.getByText('Видео')).toBeInTheDocument();
        expect(screen.getByText('Аудио')).toBeInTheDocument();
    });

    it('shows recording indicator when isRecording is true', () => {
        render(<AddContentModal {...defaultProps} isRecording={true} recordingTime={65} />);
        expect(screen.getByText('⏹️ Остановить')).toBeInTheDocument();
        expect(screen.getByText('00:00')).toBeInTheDocument();
    });

    it('shows attached file info when mediaUrl exists', () => {
        render(<AddContentModal {...defaultProps} newItemMediaUrl="blob:test" newItemMediaType="image" />);
        expect(screen.getByText('📷 Фото прикреплено')).toBeInTheDocument();
    });

    it('shows video attached text for video type', () => {
        render(<AddContentModal {...defaultProps} newItemMediaUrl="blob:test" newItemMediaType="video" />);
        expect(screen.getByText('🎬 Видео прикреплено')).toBeInTheDocument();
    });

    it('shows audio attached text for audio type', () => {
        render(<AddContentModal {...defaultProps} newItemMediaUrl="blob:test" newItemMediaType="audio" />);
        expect(screen.getByText('🎵 Аудио прикреплено')).toBeInTheDocument();
    });
});
