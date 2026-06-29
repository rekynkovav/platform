import React from 'react';
import { ROOM_TITLES } from './config';

/**
 * ChatHeader — room title + WS connection status.
 */
const ChatHeader = ({ slug, isConnected }) => (
  <div className="chat-header">
    <h3 className="chat-title">{ROOM_TITLES[slug] || slug}</h3>
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginTop: '4px' }}>
      <span
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          background: isConnected ? '#10B981' : '#EF4444',
          display: 'inline-block',
        }}
      />
      <span>{isConnected ? 'Онлайн' : 'Отключён'}</span>
    </div>
  </div>
);

export default ChatHeader;
