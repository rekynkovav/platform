import React from 'react';
import { ROOM_TITLES } from './config';
import './ChatHeader.css';

/**
 * ChatHeader — room title + WS connection status.
 */
const ChatHeader = ({ slug, isConnected }) => (
  <div className="chat-header">
    <h3 className="chat-title">{ROOM_TITLES[slug] || slug}</h3>
    <div className="chat-status">
      <span
        className="status-dot"
        style={{
          background: isConnected ? '#10B981' : '#EF4444',
        }}
      />
      <span>{isConnected ? 'Онлайн' : 'Отключён'}</span>
    </div>
  </div>
);

export default ChatHeader;
