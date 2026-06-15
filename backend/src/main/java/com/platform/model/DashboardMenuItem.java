package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistent dashboard menu item.
 * 18 sections (broadcasts, exercises, etc.) with flat or nested items.
 * Parent-child via parent_id for submenu support.
 */
@Entity
@Table(name = "dashboard_menu_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Section key: broadcasts, exercises, recommendations, etc. */
    @Column(name = "section_key", nullable = false, length = 50)
    private String sectionKey;

    /** Parent item id for submenu nesting (null = top-level) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DashboardMenuItem parent;

    /** Position within section/submenu */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(name = "media_url")
    private String mediaUrl;

    /** audio, video, text, photo, link */
    @Column(name = "media_type", length = 20)
    private String mediaType;

    /** Keycloak user UUID of creator */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
