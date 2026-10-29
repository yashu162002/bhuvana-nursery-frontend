package com.bhuvana.nursery.repository;

import com.bhuvana.nursery.entity.Plant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    @Query(
        value = """
            SELECT p FROM Plant p
            JOIN FETCH p.category c
            WHERE p.published = true
              AND p.archived = false
              AND c.published = true
              AND (cast(:categorySlug as string) IS NULL OR c.slug = :categorySlug)
              AND (
                cast(:search as string) IS NULL
                OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                OR LOWER(p.nameKn) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                OR LOWER(p.scientificName) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
              )
            """,
        countQuery = """
            SELECT COUNT(p) FROM Plant p
            JOIN p.category c
            WHERE p.published = true
              AND p.archived = false
              AND c.published = true
              AND (cast(:categorySlug as string) IS NULL OR c.slug = :categorySlug)
              AND (
                cast(:search as string) IS NULL
                OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                OR LOWER(p.nameKn) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                OR LOWER(p.scientificName) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
              )
            """
    )
    Page<Plant> findPublicPlants(
        @Param("categorySlug") String categorySlug,
        @Param("search") String search,
        Pageable pageable
    );

    Optional<Plant> findBySlugAndPublishedTrueAndArchivedFalse(String slug);

    @Query("SELECT p FROM Plant p JOIN FETCH p.category ORDER BY p.updatedAt DESC")
    List<Plant> findAllByOrderByUpdatedAtDesc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByImageUrl(String imageUrl);

    long countByImageUrlStartingWith(String prefix);
}
