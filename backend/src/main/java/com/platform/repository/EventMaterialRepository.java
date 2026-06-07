package com.platform.repository;

import com.platform.model.EventMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventMaterialRepository extends JpaRepository<EventMaterial, UUID> {

    List<EventMaterial> findBySectionIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc(String sectionId);

    List<EventMaterial> findBySectionIdOrderBySortOrderAscCreatedAtDesc(String sectionId);

    long countBySectionId(String sectionId);
}
