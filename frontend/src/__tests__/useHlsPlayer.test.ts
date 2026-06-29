// useHlsPlayer tests — pure logic + mocked hook behavior

import { StreamQuality } from '../services/live/types';

// ── Mock services (needed for hook import) ───────────────────────────────────
jest.mock('../services/live', () => ({
  mediaService: {
    getStream: jest.fn().mockResolvedValue({ url: 'http://test.m3u8', quality: 'auto', bitrate: 0, codec: 'h264' }),
    getAvailableQualities: jest.fn().mockReturnValue(['1080p', '720p', '480p', '360p', 'auto']),
    getPlayerStats: jest.fn().mockReturnValue({ bitrate: 0, bufferLength: 0, droppedFrames: 0, latency: 0, networkActivity: false }),
  },
  liveWebSocket: {},
  liveApi: {},
}));

jest.mock('../utils/live/helpers', () => ({
  formatDuration: jest.fn((s: number) => `${Math.floor(s)}s`),
  formatBitrate: jest.fn((b: number) => `${b} kbps`),
  getResolutionForQuality: jest.fn(() => ({ width: 1280, height: 720 })),
  getRecommendedQuality: jest.fn(() => '720p'),
}));

// ── Test hook contract ───────────────────────────────────────────────────────
describe('useHlsPlayer hook contract', () => {
  it('экспортирует правильный интерфейс', () => {
    const hookModule = require('../hooks/live/useHlsPlayer');
    expect(typeof hookModule.useHlsPlayer).toBe('function');
  });

  it('hook экспортирует правильную сигнатуру', () => {
    const hookModule = require('../hooks/live/useHlsPlayer');
    expect(typeof hookModule.useHlsPlayer).toBe('function');
    // Verify the hook accepts expected params by checking function length
    expect(hookModule.useHlsPlayer.length).toBe(1); // single options object
  });
});
