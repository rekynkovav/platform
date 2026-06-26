import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Questions from '../components/Help/Questions';
import { chatApi } from '../services/api';
import { getUsername, getUserId } from '../keycloak';
import { usePermissions } from '../hooks/usePermissions';

// Моки
jest.mock('react-window', () => ({
    VariableSizeList: ({ children, itemCount, itemSize }) => {
        const items = [];
        for (let i = 0; i < itemCount; i++) {
            items.push(children({ index: i, style: {} }));
        }
        return <div data-testid="virtual-list">{items}</div>;
    },
}));
jest.mock('react-virtualized-auto-sizer', () => ({
    AutoSizer: ({ children }) => children({ width: 800, height: 600 }),
}));
jest.mock('../services/api', () => ({
    chatApi: {
        getQuestionsDto: jest.fn(),
        sendQuestionDto: jest.fn(),
        answerQuestionDto: jest.fn(),
        deleteQuestionDto: jest.fn(),
        archiveAnsweredQuestionsDto: jest.fn(),
    },
}));

jest.mock('../keycloak', () => ({
    getUsername: jest.fn(),
    getUserId: jest.fn(),
}));

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

jest.mock('../components/Help/Questions.css', () => ({}));

// Helper: create a permissions object for a given role level
const createPermissions = (roleName, level) => ({
    roles: [roleName],
    level,
    hasRole: (r) => [roleName].includes(r),
    hasAnyRole: (rs) => rs.some(r => [roleName].includes(r)),
    hasLevel: (n) => level >= n,
    canViewAdmin: () => level >= 6,
    canUploadMedia: () => level >= 4,
    canStartBroadcast: () => level >= 6,
    canModerate: () => level >= 6,
    isConsultant: () => false,
});

describe('Questions', () => {
    const mockQuestions = [
        { id: 1, question: 'Test question 1?', answer: '', author: 'user1', date: '2025-06-01', isAnswered: false, answeredBy: '', answerDate: '' },
        { id: 2, question: 'Test question 2?', answer: 'Answer 2', author: 'user2', date: '2025-06-02', isAnswered: true, answeredBy: 'Admin', answerDate: '2025-06-03' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        getUsername.mockReturnValue('testuser');
        getUserId.mockReturnValue('testuser-id');
        chatApi.getQuestionsDto.mockResolvedValue(mockQuestions);
        // Default: regular user
        usePermissions.mockReturnValue(createPermissions('USER_0', 1));
    });

    it('renders questions header', async () => {
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getByText('Вопросы и ответы')).toBeInTheDocument();
        });
    });

    it('loads and displays questions', async () => {
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByText(/Test question 1\?/).length).toBeGreaterThan(0);
            expect(screen.getAllByText(/Test question 2\?/).length).toBeGreaterThan(0);
            expect(screen.getAllByText(/Answer 2/).length).toBeGreaterThan(0);
        });
    });

    it('shows no questions message when empty', async () => {
        chatApi.getQuestionsDto.mockResolvedValue([]);
        render(<Questions />);
        await waitFor(() => {
            const el = screen.getByText((content) => content.includes('нет вопросов'));
            expect(el).toBeInTheDocument();
        });
    });

    it('shows ask question section for regular users', async () => {
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByText(/Задать вопрос/).length).toBeGreaterThan(0);
            expect(screen.getAllByPlaceholderText('Введите ваш вопрос здесь...').length).toBeGreaterThan(0);
            expect(screen.getAllByText(/Отправить вопрос/).length).toBeGreaterThan(0);
        });
    });

    it('sends a question', async () => {
        chatApi.sendQuestionDto.mockResolvedValue({});
        chatApi.getQuestionsDto.mockResolvedValue(mockQuestions);

        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByPlaceholderText('Введите ваш вопрос здесь...').length).toBeGreaterThan(0);
        });

        const input = screen.getAllByPlaceholderText('Введите ваш вопрос здесь...')[0];
        fireEvent.change(input, { target: { value: 'New question?' } });
        fireEvent.click(screen.getAllByText(/Отправить вопрос/)[0]);

        await waitFor(() => {
            expect(chatApi.sendQuestionDto).toHaveBeenCalledWith('general', {
                question: 'New question?',
            });
        });
    });

    it('shows question sent notification', async () => {
        chatApi.sendQuestionDto.mockResolvedValue({});
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByPlaceholderText('Введите ваш вопрос здесь...').length).toBeGreaterThan(0);
        });

        const input = screen.getAllByPlaceholderText('Введите ваш вопрос здесь...')[0];
        fireEvent.change(input, { target: { value: 'Test' } });
        fireEvent.click(screen.getAllByText(/Отправить вопрос/)[0]);

        await waitFor(() => {
            expect(screen.getByText('Вопрос отправлен')).toBeInTheDocument();
        });
    });

    it('shows mode switcher for regular users', async () => {
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByText('Общие вопросы').length).toBeGreaterThan(0);
            expect(screen.getAllByText('Вопросы по питанию').length).toBeGreaterThan(0);
            expect(screen.getAllByText('Вопросы лично Вячеславу').length).toBeGreaterThan(0);
        });
    });

    it('shows error when load fails', async () => {
        chatApi.getQuestionsDto.mockRejectedValue(new Error('Network error'));
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить вопросы')).toBeInTheDocument();
        });
    });

    it('displays answered badge for answered questions', async () => {
        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByText(/Ответ дан/).length).toBeGreaterThan(0);
        });
    });

    it('displays waiting badge for unanswered questions to admins', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        usePermissions.mockReturnValue(createPermissions('SUPER_ADMIN', 8));

        render(<Questions />);
        await waitFor(() => {
            expect(screen.getAllByText(/Ожидает ответа/).length).toBeGreaterThan(0);
        });
    });
});
