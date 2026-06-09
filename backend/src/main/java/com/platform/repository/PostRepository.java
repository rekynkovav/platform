package com.platform.repository;

import com.platform.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Post> findFeed(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL AND p.createdAt < :before ORDER BY p.createdAt DESC")
    List<Post> findFeedBefore(@Param("before") LocalDateTime before, Pageable pageable);

    /**
     * Получить ленту по категории (для /api/feed/nutrition и т.д.)
     */
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL AND p.category = :category ORDER BY p.createdAt DESC")
    List<Post> findFeedByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL AND p.category = :category AND p.createdAt < :before ORDER BY p.createdAt DESC")
    List<Post> findFeedByCategoryBefore(@Param("category") String category, @Param("before") LocalDateTime before, Pageable pageable);

    List<Post> findByAuthorId(String authorId);

    long countByAuthorId(String authorId);

    @Query("SELECT p FROM Post p WHERE p.flagged = true AND p.deletedAt IS NULL")
    List<Post> findByIsFlaggedTrue();

    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :id")
    @Modifying(clearAutomatically = true)
    @Transactional
    void incrementViews(@Param("id") String id);
}
