package com.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ThePathOfTheHeartApplicationTests {

    @Test
    void contextLoads() {
        // Если контекст не загрузится, тест упадёт с понятной ошибкой
        assertNotNull(this, "Application context should load successfully");
    }
}
