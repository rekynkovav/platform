package com.platform.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Материал раздела EventsHub (О нас, Важные материалы, Интервью и т.д.)
 */
@Entity
@Table(name = "event_materials", indexes = {
    @Index(name = "idx_materials_section", columnList = "section_id"),
    @Index(name = "idx_materials_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column()
    private UUID id;

    @Version
    private Long version = 0L;

    /** Идентификатор раздела: about, important, interviews, beginners, reflections, participants, music, motivation, movies, results, challenge */
    @Column(name = "section_id", nullable = false, length = 50)
    private String sectionId;

    /** Тип материала: text, audio, video, photo, link */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** URL файла или внешней ссылки */
    @Column(name = "url", length = 1000)
    private String url;

    /** ID медиафайла в системе (для загруженных файлов) */
    @Column(name = "media_id")
    private String mediaId;

    /** Автор материала */
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "author_name")
    private String authorName;

    /** Порядок сортировки внутри раздела */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
