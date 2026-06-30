// Tests for services/live/realMedia.ts
// KEY: jest.fn() mockImplementation does NOT work as constructor
// KEY: static class fields with jest.fn() break Babel transpilation — use method syntax

import '@testing-library/jest-dom';

// ── Mock hls.js ────────────────────────────────────────────────────────────────
// Track instances for assertions via outer array (not jest.fn mock)
const hlsInstances: any[] = [];

jest.mock('hls.js', () => {
  class MockHls {
    destroy = jest.fn();
    loadSource = jest.fn();
    attachMedia = jest.fn();
    on = jest.fn();
    startLoad = jest.fn();
    recoverMediaError = jest.fn();
    currentLevel = 0;
    url = 'http://test.m3u8';
    levels = [
      { height: 720, width: 1280, bitrate: 2500 },
      { height: 480, width: 854, bitrate: 1000 },
    ];
    constructor(_opts?: any) { hlsInstances.push(this); }
    // METHOD syntax — works with Babel class transpilation
    static isSupported() { return true; }
    static Events = {
      MANIFEST_PARSED: 'hlsManifestParsed',
      LEVEL_SWITCHED: 'hlsLevelSwitched',
      ERROR: 'hlsError',
    };
    static ErrorTypes = {
      NETWORK_ERROR: 'networkError',
      MEDIA_ERROR: 'mediaError',
    };
  }
  return { __esModule: true, default: MockHls };
});

// ── Mock realApi (for getStream) ───────────────────────────────────────────────

jest.mock('../services/live/realApi', () => ({
  realLiveApi: {
    getBroadcast: jest.fn().mockResolvedValue({
      id: 'bc-1',
      playbackUrl: 'http://test.m3u8',
    }),
  },
}));

// ── Import after mocks ─────────────────────────────────────────────────────────

import { realMediaService, RealMediaService } from '../services/live/realMedia';

// ── Tests ──────────────────────────────────────────────────────────────────────

describe('RealMediaService', () => {
  beforeEach(() => {
    hlsInstances.length = 0;
    realMediaService.reset();
  });

  it('is a singleton', () => {
    const instance1 = RealMediaService.getInstance();
    const instance2 = RealMediaService.getInstance();
    expect(instance1).toBe(instance2);
  });

  it('exported instance is the singleton', () => {
    expect(realMediaService).toBe(RealMediaService.getInstance());
  });

  // ── getStream ─────────────────────────────────────────────────────────────

  describe('getStream', () => {
    it('returns a media stream', async () => {
      const stream = await realMediaService.getStream('bc-1');
      expect(stream).toBeDefined();
      expect(stream.url).toBe('http://test.m3u8');
    });

    it('caches streams by key', async () => {
      const s1 = await realMediaService.getStream('bc-1');
      const s2 = await realMediaService.getStream('bc-1');
      expect(s1).toBe(s2);
    });

    it('returns stream with empty URL when no playback URL', async () => {
      const { realLiveApi } = require('../services/live/realApi');
      realLiveApi.getBroadcast.mockResolvedValue({ id: 'bc-2', playbackUrl: '' });
      const stream = await realMediaService.getStream('bc-2');
      expect(stream).toBeDefined();
      expect(stream.url).toBe('');
    });
  });

  // ── attachHls ─────────────────────────────────────────────────────────────

  describe('attachHls', () => {
    it('returns null when no playback URL', () => {
      const video = document.createElement('video');
      const onError = jest.fn();

      const result = realMediaService.attachHls('bc-1', video, '', undefined, onError);

      expect(result).toBeNull();
      expect(onError).toHaveBeenCalledWith('No playback URL provided');
    });

    it('creates HLS instance and attaches', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');
      const onManifestParsed = jest.fn();

      const result = realMediaService.attachHls('bc-1', video, 'http://test.m3u8', onManifestParsed);

      expect(result).not.toBeNull();
      expect(hlsInstances.length).toBe(1);
      expect(hlsInstances[0].loadSource).toHaveBeenCalledWith('http://test.m3u8');
      expect(hlsInstances[0].attachMedia).toHaveBeenCalledWith(video);
      expect(hlsInstances[0].on).toHaveBeenCalled();
    });

    it('uses native playback for Safari', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('probably');
      const onManifestParsed = jest.fn();

      const result = realMediaService.attachHls('bc-1', video, 'http://test.m3u8', onManifestParsed);

      expect(result).toBeNull();
      expect(video.src).toContain('test.m3u8');
      expect(onManifestParsed).toHaveBeenCalled();
    });
  });

  // ── detachHls ─────────────────────────────────────────────────────────────

  describe('detachHls', () => {
    it('destroys HLS instance', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');

      realMediaService.attachHls('bc-1', video, 'http://test.m3u8');
      expect(hlsInstances.length).toBe(1);

      realMediaService.detachHls('bc-1');

      expect(hlsInstances[0].destroy).toHaveBeenCalled();
    });

    it('does not throw when no instance exists', () => {
      expect(() => realMediaService.detachHls('nonexistent')).not.toThrow();
    });
  });

  // ── switchQuality ─────────────────────────────────────────────────────────

  describe('switchQuality', () => {
    it('throws when no HLS instance', () => {
      expect(() => {
        realMediaService.switchQuality('nonexistent', '720p');
      }).toThrow('No HLS instance for nonexistent');
    });

    it('switches to auto quality', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');

      realMediaService.attachHls('bc-1', video, 'http://test.m3u8');
      realMediaService.switchQuality('bc-1', 'auto');

      expect(hlsInstances[0].currentLevel).toBe(-1);
    });

    it('switches to specific quality', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');

      realMediaService.attachHls('bc-1', video, 'http://test.m3u8');
      realMediaService.switchQuality('bc-1', '720p');

      // 720p matches first level (height: 720)
      expect(hlsInstances[0].currentLevel).toBe(0);
    });
  });

  // ── getPlayerStats ────────────────────────────────────────────────────────

  describe('getPlayerStats', () => {
    it('returns default stats when no video element', () => {
      const stats = realMediaService.getPlayerStats('bc-1');
      expect(stats.bitrate).toBe(0);
      expect(stats.bufferLength).toBe(0);
      expect(stats.droppedFrames).toBe(0);
    });
  });

  // ── getAvailableQualities ─────────────────────────────────────────────────

  describe('getAvailableQualities', () => {
    it('returns video qualities', () => {
      const qualities = realMediaService.getAvailableQualities('video');
      expect(qualities).toContain('auto');
    });

    it('returns only auto for audio', () => {
      const qualities = realMediaService.getAvailableQualities('audio');
      expect(qualities).toEqual(['auto']);
    });
  });

  // ── stopAllStreams ─────────────────────────────────────────────────────────

  describe('stopAllStreams', () => {
    it('detaches HLS and clears streams', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');

      realMediaService.attachHls('bc-1', video, 'http://test.m3u8');
      realMediaService.stopAllStreams('bc-1');

      expect(hlsInstances[0].destroy).toHaveBeenCalled();
    });

    it('simulateNetworkIssues does not throw', () => {
      expect(() => realMediaService.simulateNetworkIssues('bc-1', 'low')).not.toThrow();
    });

    it('restoreNetwork does not throw', () => {
      expect(() => realMediaService.restoreNetwork('bc-1')).not.toThrow();
    });

    it('cleans up multiple broadcast instances', () => {
      const video = document.createElement('video');
      jest.spyOn(video, 'canPlayType').mockReturnValue('');

      realMediaService.attachHls('bc-1', video, 'http://test.m3u8');
      realMediaService.attachHls('bc-2', video, 'http://test2.m3u8');
      expect(hlsInstances.length).toBe(2);

      realMediaService.stopAllStreams('bc-1');
      expect(hlsInstances[0].destroy).toHaveBeenCalled();

      realMediaService.stopAllStreams('bc-2');
      expect(hlsInstances[1].destroy).toHaveBeenCalled();
    });
  });
});
