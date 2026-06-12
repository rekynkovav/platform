import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NetworkSpeedTest from '../components/live/NetworkSpeedTest';

// Моки для fetch
global.fetch = jest.fn();

jest.mock('../components/live/NetworkSpeedTest.css', () => ({}));

describe('NetworkSpeedTest', () => {
    const mockOnTestComplete = jest.fn();
    const mockOnCancel = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch.mockImplementation((url) => {
            if (url === '/api/ping') {
                return Promise.resolve(new Response());
            }
            if (url === 'https://raw.githubusercontent.com/sivel/speedtest-cli/master/speedtest-cli') {
                return Promise.resolve({
                    body: {
                        getReader: () => {
                            let called = false;
                            return {
                                read: () => {
                                    if (!called) {
                                        called = true;
                                        return Promise.resolve({ done: false, value: new Uint8Array(1024 * 1024) });
                                    }
                                    return Promise.resolve({ done: true, value: null });
                                }
                            };
                        }
                    }
                });
            }
            if (url === '/api/test-upload') {
                return Promise.resolve(new Response());
            }
            return Promise.reject(new Error('Unknown URL'));
        });
    });

    it('renders test header', () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);
        expect(screen.getByText('Тест скорости сети')).toBeInTheDocument();
        expect(screen.getByText('Определение оптимальных настроек трансляции')).toBeInTheDocument();
    });

    it('renders cancel button', () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);
        expect(screen.getByText('Отмена')).toBeInTheDocument();
    });

    it('runs speed test on mount and displays results', async () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            // Should show results after test completes
            expect(screen.getByText(/Пинг/)).toBeInTheDocument();
            expect(screen.getByText(/Скорость загрузки/)).toBeInTheDocument();
            expect(screen.getByText(/Скорость отдачи/)).toBeInTheDocument();
            expect(screen.getByText(/Качество соединения/)).toBeInTheDocument();
        });
    });

    it('shows recommended settings after test completes', async () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Рекомендуемые настройки:')).toBeInTheDocument();
            expect(screen.getByText('Применить автонастройки')).toBeInTheDocument();
        });
    });

    it('applies settings when apply button is clicked', async () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Применить автонастройки')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Применить автонастройки'));

        expect(mockOnTestComplete).toHaveBeenCalledWith(expect.objectContaining({
            quality: expect.any(String),
            resolution: expect.any(String),
            videoBitrate: expect.any(Number),
            audioBitrate: expect.any(Number),
        }));
    });

    it('calls onCancel when cancel is clicked', () => {
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);
        fireEvent.click(screen.getByText('Отмена'));
        expect(mockOnCancel).toHaveBeenCalled();
    });

    it('shows re-test button on error', async () => {
        global.fetch.mockRejectedValue(new Error('Network error'));
        render(<NetworkSpeedTest onTestComplete={mockOnTestComplete} onCancel={mockOnCancel} />);

        await waitFor(() => {
            expect(screen.getByText('Повторить тест')).toBeInTheDocument();
        });
    });
});
