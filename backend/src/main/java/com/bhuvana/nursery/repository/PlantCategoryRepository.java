package com.bhuvana.nursery.repository;

import com.bhuvana.nursery.entity.PlantCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantCategoryRepository
        extends JpaRepository<PlantCategory, UUID> {

    List<PlantCategory> findByPublishedTrueOrderByNameEnAsc();

    Optional<PlantCategory> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    List<PlantCategory> findAllByOrderByUpdatedAtDesc();

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
