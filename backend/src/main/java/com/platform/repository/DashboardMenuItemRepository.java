package com.platform.repository;

import com.platform.model.DashboardMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardMenuItemRepository extends JpaRepository<DashboardMenuItem, UUID> {

    /** All items in a section, ordered. */
    List<DashboardMenuItem> findBySectionKeyOrderBySortOrderAsc(String sectionKey);

    /** Top-level items in a section (parent = null). */
    List<DashboardMenuItem> findBySectionKeyAndParentIsNullOrderBySortOrderAsc(String sectionKey);

    /** Children of a parent item. */
    List<DashboardMenuItem> findByParentIdOrderBySortOrderAsc(UUID parentId);

    /** All items across all sections. */
    List<DashboardMenuItem> findAllByOrderBySectionKeyAscSortOrderAsc();

    /** Count items in a section. */
    long countBySectionKey(String sectionKey);

    /** Check if section is empty. */
    boolean existsBySectionKey(String sectionKey);

    /** Delete all items in a section (for full replace). */
    @Modifying
    void deleteBySectionKey(String sectionKey);

    /** Get max sort_order in a section. */
    @Query("SELECT COALESCE(MAX(m.sortOrder), 0) FROM DashboardMenuItem m WHERE m.sectionKey = :sectionKey")
    int findMaxSortOrder(@Param("sectionKey") String sectionKey);
}
