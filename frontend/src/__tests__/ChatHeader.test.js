import React from 'react';
import { render, screen } from '@testing-library/react';
import ChatHeader from '../features/chat/ChatHeader';
import { ROOM_TITLES } from '../features/chat/config';

describe('ChatHeader', () => {
  it('renders room title from ROOM_TITLES when slug matches', () => {
    render(<ChatHeader slug="user1" isConnected={true} />);
    expect(screen.getByText(ROOM_TITLES['user1'])).toBeInTheDocument();
  });

  it('renders slug as fallback when title is not in ROOM_TITLES', () => {
    render(<ChatHeader slug="unknown_room" isConnected={true} />);
    expect(screen.getByText('unknown_room')).toBeInTheDocument();
  });

  it('shows "Онлайн" when connected', () => {
    render(<ChatHeader slug="user1" isConnected={true} />);
    expect(screen.getByText('Онлайн')).toBeInTheDocument();
  });

  it('shows "Отключён" when disconnected', () => {
    render(<ChatHeader slug="user1" isConnected={false} />);
    expect(screen.getByText('Отключён')).toBeInTheDocument();
  });

  it('renders the chat-header container', () => {
    const { container } = render(<ChatHeader slug="user1" isConnected={true} />);
    expect(container.querySelector('.chat-header')).toBeInTheDocument();
  });

  it('renders the chat-title element with correct text', () => {
    render(<ChatHeader slug="nutrition" isConnected={false} />);
    const title = screen.getByText(ROOM_TITLES['nutrition']);
    expect(title).toBeInTheDocument();
    expect(title.className).toBe('chat-title');
  });
});
