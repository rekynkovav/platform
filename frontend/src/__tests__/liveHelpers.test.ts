// Live helpers tests — pure functions, no mocks needed
import {
  formatDuration,
  formatBitrate,
  getResolutionForQuality,
  getRecommendedQuality,
  getRoleColor,
  getRoleText,
  isValidStreamUrl,
  isValidBroadcastTitle,
  isValidChatMessage,
  pluralize,
} from '../utils/live/helpers';

describe('live helpers', () => {
  describe('getResolutionForQuality', () => {
    it('1080p → 1920x1080', () => {
      expect(getResolutionForQuality('1080p')).toEqual({ width: 1920, height: 1080 });
    });

    it('720p → 1280x720', () => {
      expect(getResolutionForQuality('720p')).toEqual({ width: 1280, height: 720 });
    });

    it('480p → 854x480', () => {
      expect(getResolutionForQuality('480p')).toEqual({ width: 854, height: 480 });
    });

    it('360p → 640x360', () => {
      expect(getResolutionForQuality('360p')).toEqual({ width: 640, height: 360 });
    });

    it('auto → 1280x720 (дефолт)', () => {
      expect(getResolutionForQuality('auto')).toEqual({ width: 1280, height: 720 });
    });
  });

  describe('getRecommendedQuality', () => {
    it('≥1920 → 1080p', () => {
      expect(getRecommendedQuality(1920)).toBe('1080p');
      expect(getRecommendedQuality(2560)).toBe('1080p');
    });

    it('≥1280 → 720p', () => {
      expect(getRecommendedQuality(1280)).toBe('720p');
      expect(getRecommendedQuality(1500)).toBe('720p');
    });

    it('≥854 → 480p', () => {
      expect(getRecommendedQuality(854)).toBe('480p');
      expect(getRecommendedQuality(1000)).toBe('480p');
    });

    it('<854 → 360p', () => {
      expect(getRecommendedQuality(640)).toBe('360p');
      expect(getRecommendedQuality(400)).toBe('360p');
    });
  });

  describe('formatDuration', () => {
    it('секунды', () => expect(formatDuration(65)).toBe('01:05'));
    it('часы', () => expect(formatDuration(3661)).toBe('01:01:01'));
    it('ноль', () => expect(formatDuration(0)).toBe('00:00'));
    it('отрицательное', () => expect(formatDuration(-5)).toBe('00:00'));
  });

  describe('formatBitrate', () => {
    it('кбит', () => expect(formatBitrate(500)).toBe('500 Кбит/с'));
    it('мбит', () => expect(formatBitrate(2500)).toBe('2.5 Мбит/с'));
  });

  describe('getRoleColor / getRoleText', () => {
    it('USER_1 → blue / Пользователь', () => {
      expect(getRoleColor('USER_1')).toBe('blue');
      expect(getRoleText('USER_1')).toBe('Пользователь');
    });

    it('SUPER_ADMIN → red / Администратор', () => {
      expect(getRoleColor('SUPER_ADMIN')).toBe('red');
      expect(getRoleText('SUPER_ADMIN')).toBe('Администратор');
    });

    it('unknown role → gray fallback', () => {
      expect(getRoleColor('UNKNOWN')).toBe('gray');
      expect(getRoleText('UNKNOWN')).toBe('Пользователь');
    });
  });

  describe('validation', () => {
    it('isValidStreamUrl — valid http', () => {
      expect(isValidStreamUrl('http://example.com/stream')).toBe(true);
      expect(isValidStreamUrl('https://example.com/stream')).toBe(true);
    });

    it('isValidStreamUrl — invalid', () => {
      expect(isValidStreamUrl('')).toBe(false);
      expect(isValidStreamUrl('ftp://example.com')).toBe(false);
    });

    it('isValidBroadcastTitle', () => {
      expect(isValidBroadcastTitle('Hello')).toBe(true);
      expect(isValidBroadcastTitle('ab')).toBe(false); // < 3 chars
      expect(isValidBroadcastTitle('')).toBe(false);
    });

    it('isValidChatMessage', () => {
      expect(isValidChatMessage('Hi')).toBe(true);
      expect(isValidChatMessage('')).toBe(false);
    });
  });

  describe('pluralize', () => {
    it('1 минуту', () => expect(pluralize(1, ['минуту', 'минуты', 'минут'])).toBe('минуту'));
    it('2 минуты', () => expect(pluralize(2, ['минуту', 'минуты', 'минут'])).toBe('минуты'));
    it('5 минут', () => expect(pluralize(5, ['минуту', 'минуты', 'минут'])).toBe('минут'));
  });
});
