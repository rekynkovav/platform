import '@testing-library/jest-dom';

// Global mocks for jsdom environment
Element.prototype.scrollIntoView = jest.fn();
global.MediaStream = class MediaStream {};
