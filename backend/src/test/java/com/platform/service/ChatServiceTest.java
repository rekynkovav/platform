package com.platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Test
    public void testGetMessagesByRoomWithPagination() {
        // Тест получения сообщений с пагинацией
        // TODO: Добавить тестовые данные и проверки
    }

    @Test
    public void testSendMessage() {
        // Тест отправки сообщения
        // TODO: Добавить тестовые данные и проверки
    }
}