export interface ChatRoom {
  id: string;
  slug: string;
  title: string;
  description?: string;
  type: 'general' | 'admin' | 'private' | 'questions' | 'nutrition' | 'workout';
  ownerId?: string;
  createdAt: string;
  updatedAt?: string;
  isActive: boolean;
  participantCount?: number;
  lastMessageAt?: string;
  permissions?: {
    read: boolean;
    write: boolean;
    admin: boolean;
  };
}

export interface ChatMessage {
  id: string;
  content: string;
  userId: string;
  userName: string;
  chatType: string;
  recipientId?: string;
  isRead: boolean;
  createdAt: string;
  mediaId?: string;
  mediaType?: string;
  fileName?: string;
  roomId?: string;
  roomSlug?: string;
  pending?: boolean;
  edited?: boolean;
  avatarUrl?: string;
  author?: string;
  timestamp?: string;
  fileUrl?: string;
  fileType?: string;
  type?: string;
  isSystem?: boolean;
  isOwn?: boolean;
}

export interface ChatRoomPermission {
  roomId: string;
  userId: string;
  role: 'owner' | 'admin' | 'member' | 'readonly';
  grantedAt: string;
  grantedBy?: string;
}

export interface ChatRoomMember {
  userId: string;
  userName: string;
  role: string;
  joinedAt: string;
  lastActiveAt?: string;
}

export interface ChatRoomStats {
  roomId: string;
  totalMessages: number;
  unreadMessages: number;
  activeUsers: number;
  lastActivity: string;
}

export interface ChatError {
  code: string;
  message: string;
  timestamp: string;
  details?: Record<string, any>;
}

export interface ChatApiResponse<T> {
  data: T;
  error?: ChatError;
  success: boolean;
  timestamp: string;
}

export interface ChatPagination {
  page: number;
  limit: number;
  total: number;
  hasMore: boolean;
}