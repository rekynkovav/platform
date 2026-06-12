import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Questions from '../components/Help/Questions';
import { chatApi } from '../services/api';
import { getUsername, getUserId } from '../keycloak';

// Моки
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

jest.mock('../components/Help/Questions.css', () => ({}));

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
            expect(screen.getByText('Test question 1?')).toBeInTheDocument();
            expect(screen.getByText('Test question 2?')).toBeInTheDocument();
            expect(screen.getByText('Answer 2')).toBeInTheDocument();
        });
    });

    it('shows no questions message when empty', async () => {
        chatApi.getQuestionsDto.mockResolvedValue([]);
        render(<Questions />);

        await waitFor(() => {
            expect(screen.getByText(/Пока нет вопросов/)).toBeInTheDocument();
        });
    });

    it('shows ask question section for regular users', async () => {
        render(<Questions />);

        await waitFor(() => {
            expect(screen.getByText('Задать вопрос')).toBeInTheDocument();
            expect(screen.getByPlaceholderText('Введите ваш вопрос здесь...')).toBeInTheDocument();
            expect(screen.getByText('📤 Отправить вопрос')).toBeInTheDocument();
        });
    });

    it('sends a question', async () => {
        chatApi.sendQuestionDto.mockResolvedValue({});
        chatApi.getQuestionsDto.mockResolvedValue(mockQuestions);

        render(<Questions />);

        await waitFor(() => {
            expect(screen.getByPlaceholderText('Введите ваш вопрос здесь...')).toBeInTheDocument();
        });

        const input = screen.getByPlaceholderText('Введите ваш вопрос здесь...');
        fireEvent.change(input, { target: { value: 'New question?' } });
        fireEvent.click(screen.getByText('📤 Отправить вопрос'));

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
            expect(screen.getByPlaceholderText('Введите ваш вопрос здесь...')).toBeInTheDocument();
        });

        const input = screen.getByPlaceholderText('Введите ваш вопрос здесь...');
        fireEvent.change(input, { target: { value: 'Test' } });
        fireEvent.click(screen.getByText('📤 Отправить вопрос'));

        await waitFor(() => {
            expect(screen.getByText('Вопрос отправлен')).toBeInTheDocument();
        });
    });

    it('shows mode switcher for regular users', async () => {
        render(<Questions />);

        await waitFor(() => {
            expect(screen.getByText('Общие вопросы')).toBeInTheDocument();
            expect(screen.getByText('Вопросы по питанию')).toBeInTheDocument();
            expect(screen.getByText('Вопросы лично Вячеславу')).toBeInTheDocument();
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
            expect(screen.getByText('✅ Ответ дан')).toBeInTheDocument();
        });
    });

    it('displays waiting badge for unanswered questions to admins', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        sessionStorage.setItem('demoRole', 'MODERATOR_ADMIN');

        render(<Questions />);

        await waitFor(() => {
            expect(screen.getByText(/Ожидает ответа/)).toBeInTheDocument();
        });

        sessionStorage.removeItem('demoRole');
    });
});
