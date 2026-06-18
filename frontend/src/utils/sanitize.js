/**
 * Input sanitization utilities for XSS prevention.
 *
 * React auto-escapes content rendered in JSX ({expression}), but these
 * helpers provide defense-in-depth for edge cases where raw strings may
 * be used in HTML context, URL construction, or DOM manipulation.
 */

/**
 * Escapes HTML special characters in a string to prevent XSS when
 * the value is used in an HTML context (e.g., innerHTML, dangerouslySetInnerHTML).
 *
 * This mirrors the backend XssSanitizer.sanitize() logic for consistency.
 *
 * @param {string|null|undefined} input - Raw user input
 * @returns {string} HTML-escaped string safe for HTML insertion
 */
export function escapeHtml(input) {
  if (input == null) return '';
  return String(input)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

/**
 * Validates a URL string to ensure it uses a safe protocol (http/https).
 * Prevents javascript:, data:, and other dangerous protocol handlers.
 *
 * @param {string} url - URL to validate
 * @returns {boolean} true if the URL uses a safe protocol
 */
export function isSafeUrl(url) {
  if (!url || typeof url !== 'string') return false;
  try {
    const parsed = new URL(url);
    return ['http:', 'https:'].includes(parsed.protocol);
  } catch {
    // If URL constructor fails, try with https:// prefix
    try {
      const parsed = new URL('https://' + url);
      return ['http:', 'https:'].includes(parsed.protocol);
    } catch {
      return false;
    }
  }
}

/**
 * Validates a navigation path to ensure it's a relative path (not a full URL).
 * Prevents open redirect attacks via external URLs in navigation paths.
 *
 * @param {string} path - Path to validate
 * @returns {boolean} true if the path is a safe relative path
 */
export function isSafePath(path) {
  if (!path || typeof path !== 'string') return false;
  // Must start with / and not contain protocol
  return path.startsWith('/') && !path.includes('://');
}

/**
 * Strips potentially dangerous characters from a string for use as
 * a safe text content value. Useful when passing user input to
 * DOM APIs that don't auto-escape.
 *
 * @param {string|null|undefined} input - Raw user input
 * @returns {string} Sanitized string
 */
export function sanitizeText(input) {
  if (input == null) return '';
  return String(input)
    .replace(/[<>]/g, '') // Remove angle brackets
    .trim();
}

/**
 * Validates file type against an allowlist of MIME type prefixes.
 *
 * @param {string} fileType - MIME type of the file (e.g., 'image/png')
 * @param {string[]} allowedPrefixes - Allowed MIME type prefixes (e.g., ['image/', 'video/'])
 * @returns {boolean} true if the file type is allowed
 */
export function isAllowedFileType(fileType, allowedPrefixes = ['image/', 'video/', 'audio/']) {
  if (!fileType || typeof fileType !== 'string') return false;
  return allowedPrefixes.some(prefix => fileType.startsWith(prefix));
}
