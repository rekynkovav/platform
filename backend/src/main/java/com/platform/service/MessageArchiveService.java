package com.platform.service;

import com.platform.model.Message;
import com.platform.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageArchiveService {

    private final MessageRepository messageRepository;

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
    @Transactional
    public void archiveOldMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        var oldMessages = messageRepository.findByCreatedAtBefore(cutoff);

        log.info("Archiving {} old messages (older than {})", oldMessages.size(), cutoff);

        // В реальном проекте здесь была бы логика сохранения в архивную таблицу
        // и удаления из основной таблицы
        // messageRepository.deleteAll(oldMessages);

        log.info("Archived {} messages", oldMessages.size());
    }
}