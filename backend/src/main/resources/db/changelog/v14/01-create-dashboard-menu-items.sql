-- V14: Dashboard menu items — persistent storage for admin dashboard content
-- Replaces sessionStorage-based storage with proper JPA entity

CREATE TABLE dashboard_menu_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key     VARCHAR(50)  NOT NULL,
    parent_id       UUID         REFERENCES dashboard_menu_items(id) ON DELETE CASCADE,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    media_url       TEXT,
    media_type      VARCHAR(20),
    created_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Fast lookup by section (main query pattern)
CREATE INDEX idx_dashboard_menu_section ON dashboard_menu_items(section_key, sort_order);

-- Fast lookup for submenus
CREATE INDEX idx_dashboard_menu_parent ON dashboard_menu_items(parent_id);

-- Unique ordering within section
CREATE UNIQUE INDEX idx_dashboard_menu_section_order ON dashboard_menu_items(section_key, sort_order);
