package com.platform.repository;

import com.platform.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findFeed(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdAt < :before ORDER BY p.createdAt DESC")
    List<Post> findFeedBefore(@Param("before") String before, Pageable pageable);

    List<Post> findByAuthorId(String authorId);

    long countByAuthorId(String authorId);

    @Query("SELECT p FROM Post p WHERE p.flagged = true")
    List<Post> findByIsFlaggedTrue();

    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :id")
    @Modifying
    @Transactional
    void incrementViews(@Param("id") String id);
}
