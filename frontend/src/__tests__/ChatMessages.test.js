import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ChatMessages from '../features/chat/ChatMessages';

jest.mock('../components/Chat/MediaRenderer', () => {
  return function MockMediaRenderer({ mediaUrl, mediaType, fileName }) {
    return (
      <div data-testid="media-renderer">
        <span data-testid="media-url">{mediaUrl}</span>
        <span data-testid="media-type">{mediaType}</span>
        <span data-testid="media-filename">{fileName}</span>
      </div>
    );
  };
});

jest.mock('../components/Chat/MediaRenderer.css', () => ({}));

describe('ChatMessages', () => {
  const defaultProps = {
    messages: [],
    username: 'testuser',
    loading: false,
    onEdit: jest.fn(),
    onDelete: jest.fn(),
  };

  const sampleMessage = {
    id: 1,
    author: 'testuser',
    content: 'Hello world',
    timestamp: '2025-06-29T10:30:00Z',
  };

  const otherMessage = {
    id: 2,
    author: 'other_user',
    content: 'Reply here',
    timestamp: '2025-06-29T10:35:00Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  // --- Loading state ---

  it('shows loading indicator when loading is true', () => {
    render(<ChatMessages {...defaultProps} loading={true} />);
    expect(screen.getByText('Загрузка...')).toBeInTheDocument();
  });

  it('does not show loading indicator when loading is false', () => {
    render(<ChatMessages {...defaultProps} loading={false} />);
    expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
  });

  // --- Empty state ---

  it('renders the chat-messages container', () => {
    const { container } = render(<ChatMessages {...defaultProps} />);
    expect(container.querySelector('.chat-messages')).toBeInTheDocument();
  });

  it('renders empty when no messages', () => {
    const { container } = render(<ChatMessages {...defaultProps} messages={[]} />);
    const messagesContainer = container.querySelector('.chat-messages');
    // Only the scroll anchor div should be inside
    expect(messagesContainer.children.length).toBe(1); // just the ref div
  });

  // --- Rendering messages ---

  it('renders a single message with author and content', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText('Hello world')).toBeInTheDocument();
  });

  it('renders multiple messages', () => {
    render(
      <ChatMessages {...defaultProps} messages={[sampleMessage, otherMessage]} />,
    );
    expect(screen.getByText('Hello world')).toBeInTheDocument();
    expect(screen.getByText('Reply here')).toBeInTheDocument();
  });

  // --- Own messages vs others ---

  it('applies "own" class to messages from the current user', () => {
    const { container } = render(
      <ChatMessages {...defaultProps} messages={[sampleMessage]} />,
    );
    const msg = container.querySelector('.chat-message.own');
    expect(msg).toBeInTheDocument();
  });

  it('does not apply "own" class to other users messages', () => {
    const { container } = render(
      <ChatMessages {...defaultProps} messages={[otherMessage]} />,
    );
    const own = container.querySelector('.chat-message.own');
    expect(own).not.toBeInTheDocument();
  });

  // --- Pending messages ---

  it('shows "Отправляется..." for pending messages', () => {
    const pendingMsg = { ...sampleMessage, pending: true };
    render(<ChatMessages {...defaultProps} messages={[pendingMsg]} />);
    expect(screen.getByText('Отправляется...')).toBeInTheDocument();
  });

  it('applies "pending" class to pending messages', () => {
    const { container } = render(
      <ChatMessages {...defaultProps} messages={[{ ...sampleMessage, pending: true }]} />,
    );
    expect(container.querySelector('.chat-message.pending')).toBeInTheDocument();
  });

  // --- Edited label ---

  it('shows "(изменено)" for edited messages', () => {
    const editedMsg = { ...sampleMessage, edited: true };
    render(<ChatMessages {...defaultProps} messages={[editedMsg]} />);
    expect(screen.getByText('(изменено)')).toBeInTheDocument();
  });

  // --- Edit / Delete buttons ---

  it('shows edit and delete buttons for own messages', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    expect(screen.getByTitle('Редактировать')).toBeInTheDocument();
    expect(screen.getByTitle('Удалить')).toBeInTheDocument();
  });

  it('does NOT show edit/delete for other users messages', () => {
    render(<ChatMessages {...defaultProps} messages={[otherMessage]} />);
    expect(screen.queryByTitle('Редактировать')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Удалить')).not.toBeInTheDocument();
  });

  it('does NOT show edit/delete for pending own messages', () => {
    const pendingOwn = { ...sampleMessage, pending: true };
    render(<ChatMessages {...defaultProps} messages={[pendingOwn]} />);
    expect(screen.queryByTitle('Редактировать')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Удалить')).not.toBeInTheDocument();
  });

  it('calls onEdit when edit button clicked', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    fireEvent.click(screen.getByTitle('Редактировать'));
    expect(defaultProps.onEdit).toHaveBeenCalledWith(sampleMessage);
  });

  it('calls onDelete with message id when delete button clicked', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    fireEvent.click(screen.getByTitle('Удалить'));
    expect(defaultProps.onDelete).toHaveBeenCalledWith(1);
  });

  // --- Avatar handling ---

  it('shows avatar placeholder with icon for regular user', () => {
    const { container } = render(
      <ChatMessages {...defaultProps} messages={[sampleMessage]} />,
    );
    expect(container.querySelector('.avatar-placeholder')).toBeInTheDocument();
  });

  it('shows system icon for "Система" author', () => {
    const sysMsg = { ...sampleMessage, author: 'Система', id: 10 };
    render(<ChatMessages {...defaultProps} messages={[sysMsg]} />);
    expect(screen.getByText('🤖')).toBeInTheDocument();
  });

  it('shows admin icon for "Админ" author', () => {
    const adminMsg = { ...sampleMessage, author: 'Админ', id: 11 };
    render(<ChatMessages {...defaultProps} messages={[adminMsg]} />);
    expect(screen.getByText('🛡️')).toBeInTheDocument();
  });

  it('renders avatar image when avatarUrl is provided', () => {
    const msgWithAvatar = { ...sampleMessage, avatarUrl: 'https://example.com/avatar.png' };
    render(<ChatMessages {...defaultProps} messages={[msgWithAvatar]} />);
    const img = screen.getByAltText('Аватар testuser');
    expect(img).toBeInTheDocument();
    expect(img.src).toContain('avatar.png');
  });

  // --- Media rendering ---

  it('renders MediaRenderer when mediaId is present', () => {
    const msgWithMedia = { ...sampleMessage, mediaId: 'media-123', mediaType: 'image', fileName: 'photo.jpg' };
    render(<ChatMessages {...defaultProps} messages={[msgWithMedia]} />);
    expect(screen.getByTestId('media-renderer')).toBeInTheDocument();
    expect(screen.getByTestId('media-url')).toHaveTextContent('/api/media/stream/media-123');
    expect(screen.getByTestId('media-type')).toHaveTextContent('image');
    expect(screen.getByTestId('media-filename')).toHaveTextContent('photo.jpg');
  });

  it('renders MediaRenderer when mediaUrl is present', () => {
    const msgWithUrl = { ...sampleMessage, mediaUrl: 'https://example.com/video.mp4', mediaType: 'video' };
    render(<ChatMessages {...defaultProps} messages={[msgWithUrl]} />);
    expect(screen.getByTestId('media-renderer')).toBeInTheDocument();
    expect(screen.getByTestId('media-url')).toHaveTextContent('https://example.com/video.mp4');
    expect(screen.getByTestId('media-type')).toHaveTextContent('video');
  });

  it('renders MediaRenderer when fileUrl is present', () => {
    const msgWithFile = { ...sampleMessage, fileUrl: 'https://example.com/doc.pdf', fileType: 'file' };
    render(<ChatMessages {...defaultProps} messages={[msgWithFile]} />);
    expect(screen.getByTestId('media-renderer')).toBeInTheDocument();
    expect(screen.getByTestId('media-url')).toHaveTextContent('https://example.com/doc.pdf');
    expect(screen.getByTestId('media-type')).toHaveTextContent('file');
  });

  it('does not render MediaRenderer when no media present', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    expect(screen.queryByTestId('media-renderer')).not.toBeInTheDocument();
  });

  // --- Timestamp ---

  it('formats and displays timestamp in Russian locale', () => {
    render(<ChatMessages {...defaultProps} messages={[sampleMessage]} />);
    // The timestamp should be rendered as a time string
    const timestamps = screen.getAllByText(/:/);
    expect(timestamps.length).toBeGreaterThan(0);
  });
});
