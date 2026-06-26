package com.platform.repository;

import com.platform.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
    List<Comment> findByPostId(String postId);
    long countByPostId(String postId);

    /**
     * Batch fetch top-N comments per post IDs in a single query.
     * Uses window function (ROW_NUMBER) to avoid N+1 queries.
     * Results ordered by post_id, then by rn (1 = most recent).
     */
    @Query(value = """
            SELECT * FROM (
                SELECT c.*,
                       ROW_NUMBER() OVER (PARTITION BY c.post_id ORDER BY c.created_at DESC) as rn
                FROM comments c
                WHERE c.post_id IN :postIds
            ) ranked
            WHERE ranked.rn <= :topN
            ORDER BY ranked.post_id, ranked.rn
            """, nativeQuery = true)
    List<Comment> findTopNByPostIds(@Param("postIds") Set<String> postIds,
                                    @Param("topN") int topN);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.postId = :postId")
    void deleteByPostId(String postId);
}
