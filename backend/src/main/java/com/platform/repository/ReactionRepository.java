package com.platform.repository;

import com.platform.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, String> {

    Optional<Reaction> findByPostIdAndUserIdAndType(String postId, String userId, String type);

    long countByPostIdAndType(String postId, String type);

    @Modifying
    @Transactional
    @Query("DELETE FROM Reaction r WHERE r.postId = :postId AND r.userId = :userId AND r.type = :type")
    void deleteByPostIdAndUserIdAndType(String postId, String userId, String type);

    boolean existsByPostIdAndUserIdAndType(String postId, String userId, String type);
}
