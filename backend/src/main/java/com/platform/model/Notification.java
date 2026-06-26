package com.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Builder.Default
    private Long version = 0L;

    private String userId;           // кому уведомление
    private String type;             // event, question, answer, admin-reply
    private String title;
    private String message;

    private Long relatedId;          // id события / вопроса / сообщения
    private String relatedRoomSlug;  // для вопросов — questions-*

    private Boolean read = false;

    @CreationTimestamp
    private Instant createdAt;
}