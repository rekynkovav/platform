package com.platform.repository;

import com.platform.model.entity.Post;
import com.platform.model.entity.User;
import com.platform.model.enums.PostStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    private User author1;
    private User author2;
    private Post post1;
    private Post post2;
    private Post post3;
    private Post post4;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test authors
        author1 = User.builder()
                .id(UUID.randomUUID())
                .username("author1")
                .email("author1@example.com")
                .passwordHash("hash1")
                .build();

        author2 = User.builder()
                .id(UUID.randomUUID())
                .username("author2")
                .email("author2@example.com")
                .passwordHash("hash2")
                .build();

        entityManager.persist(author1);
        entityManager.persist(author2);

        // Create test posts
        post1 = Post.builder()
                .id(UUID.randomUUID())
                .content("First post content")
                .author(author1)
                .status(PostStatus.PUBLISHED)
                .likesCount(10)
                .commentsCount(5)
                .createdAt(Instant.now().minusDays(4))
                .build();

        post2 = Post.builder()
                .id(UUID.randomUUID())
                .content("Second post content")
                .author(author1)
                .status(PostStatus.PUBLISHED)
                .likesCount(25)
                .commentsCount(12)
                .createdAt(Instant.now().minusDays(2))
                .build();

        post3 = Post.builder()
                .id(UUID.randomUUID())
                .content("Third post content")
                .author(author2)
                .status(PostStatus.PUBLISHED)
                .likesCount(5)
                .commentsCount(2)
                .createdAt(Instant.now().minusDays(1))
                .build();

        post4 = Post.builder()
                .id(UUID.randomUUID())
                .content("Draft post content")
                .author(author1)
                .status(PostStatus.DRAFT)
                .likesCount(0)
                .commentsCount(0)
                .createdAt(Instant.now().minusHours(6))
                .build();

        entityManager.persist(post1);
        entityManager.persist(post2);
        entityManager.persist(post3);
        entityManager.persist(post4);
        entityManager.flush();
    }

    @Test
    void findByAuthorId_ValidAuthor_ReturnsPosts() {
        // Act
        List<Post> posts = postRepository.findByAuthorId(author1.getId());

        // Assert
        assertThat(posts).hasSize(3); // post1, post2, post4
        assertThat(posts).extracting(Post::getContent)
                .containsExactlyInAnyOrder(
                        "First post content",
                        "Second post content",
                        "Draft post content"
                );
    }

    @Test
    void findByAuthorId_NonExistentAuthor_ReturnsEmptyList() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        List<Post> posts = postRepository.findByAuthorId(nonExistentId);

        // Assert
        assertThat(posts).isEmpty();
    }

    @Test
    void findByStatus_ValidStatus_ReturnsPosts() {
        // Act
        List<Post> publishedPosts = postRepository.findByStatus(PostStatus.PUBLISHED);

        // Assert
        assertThat(publishedPosts).hasSize(3); // post1, post2, post3
        assertThat(publishedPosts).extracting(Post::getStatus)
                .containsOnly(PostStatus.PUBLISHED);
    }

    @Test
    void findByStatus_DraftPosts_ReturnsDrafts() {
        // Act
        List<Post> draftPosts = postRepository.findByStatus(PostStatus.DRAFT);

        // Assert
        assertThat(draftPosts).hasSize(1); // post4
        assertThat(draftPosts.get(0).getContent()).isEqualTo("Draft post content");
        assertThat(draftPosts.get(0).getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void findByAuthorIdAndStatus_ValidParams_ReturnsPosts() {
        // Act
        List<Post> posts = postRepository.findByAuthorIdAndStatus(
                author1.getId(),
                PostStatus.PUBLISHED
        );

        // Assert
        assertThat(posts).hasSize(2); // post1, post2
        assertThat(posts).extracting(Post::getContent)
                .containsExactlyInAnyOrder(
                        "First post content",
                        "Second post content"
                );
        assertThat(posts).extracting(Post::getStatus)
                .containsOnly(PostStatus.PUBLISHED);
    }

    @Test
    void findByAuthorIdAndStatus_NoMatches_ReturnsEmptyList() {
        // Act
        List<Post> posts = postRepository.findByAuthorIdAndStatus(
                author2.getId(),
                PostStatus.DRAFT
        );

        // Assert
        assertThat(posts).isEmpty();
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ReturnsSortedPosts() {
        // Act
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertThat(posts).hasSize(4);
        // Should be sorted by createdAt descending (newest first)
        assertThat(posts.get(0).getContent()).isEqualTo("Draft post content"); // 6 hours ago
        assertThat(posts.get(1).getContent()).isEqualTo("Third post content"); // 1 day ago
        assertThat(posts.get(2).getContent()).isEqualTo("Second post content"); // 2 days ago
        assertThat(posts.get(3).getContent()).isEqualTo("First post content"); // 4 days ago
    }

    @Test
    void findAllByOrderByLikesCountDesc_ReturnsSortedByLikes() {
        // Act
        List<Post> posts = postRepository.findAllByOrderByLikesCountDesc();

        // Assert
        assertThat(posts).hasSize(4);
        // Should be sorted by likesCount descending
        assertThat(posts.get(0).getLikesCount()).isEqualTo(25); // post2
        assertThat(posts.get(1).getLikesCount()).isEqualTo(10); // post1
        assertThat(posts.get(2).getLikesCount()).isEqualTo(5);  // post3
        assertThat(posts.get(3).getLikesCount()).isEqualTo(0);  // post4
    }

    @Test
    void findAllByOrderByCommentsCountDesc_ReturnsSortedByComments() {
        // Act
        List<Post> posts = postRepository.findAllByOrderByCommentsCountDesc();

        // Assert
        assertThat(posts).hasSize(4);
        // Should be sorted by commentsCount descending
        assertThat(posts.get(0).getCommentsCount()).isEqualTo(12); // post2
        assertThat(posts.get(1).getCommentsCount()).isEqualTo(5);  // post1
        assertThat(posts.get(2).getCommentsCount()).isEqualTo(2);  // post3
        assertThat(posts.get(3).getCommentsCount()).isEqualTo(0);  // post4
    }

    @Test
    void findByCreatedAtBefore_ValidDate_ReturnsOlderPosts() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(3);

        // Act
        List<Post> posts = postRepository.findByCreatedAtBefore(cutoff);

        // Assert
        assertThat(posts).hasSize(1); // post1 (4 days ago)
        assertThat(posts.get(0).getContent()).isEqualTo("First post content");
    }

    @Test
    void findByCreatedAtAfter_ValidDate_ReturnsNewerPosts() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(3);

        // Act
        List<Post> posts = postRepository.findByCreatedAtAfter(cutoff);

        // Assert
        assertThat(posts).hasSize(3); // post2, post3, post4
        assertThat(posts).extracting(Post::getContent)
                .containsExactlyInAnyOrder(
                        "Second post content",
                        "Third post content",
                        "Draft post content"
                );
    }

    @Test
    void findByCreatedAtBetween_ValidRange_ReturnsPosts() {
        // Arrange
        Instant start = Instant.now().minusDays(3);
        Instant end = Instant.now().minusDays(1);

        // Act
        List<Post> posts = postRepository.findByCreatedAtBetween(start, end);

        // Assert
        assertThat(posts).hasSize(1); // post2 (2 days ago)
        assertThat(posts.get(0).getContent()).isEqualTo("Second post content");
    }

    @Test
    void countByAuthorId_ValidAuthor_ReturnsCount() {
        // Act
        long count = postRepository.countByAuthorId(author1.getId());

        // Assert
        assertThat(count).isEqualTo(3); // post1, post2, post4
    }

    @Test
    void countByAuthorId_NonExistentAuthor_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        long count = postRepository.countByAuthorId(nonExistentId);

        // Assert
        assertThat(count).isEqualTo(0);
    }

    @Test
    void countByStatus_ValidStatus_ReturnsCount() {
        // Act
        long publishedCount = postRepository.countByStatus(PostStatus.PUBLISHED);
        long draftCount = postRepository.countByStatus(PostStatus.DRAFT);

        // Assert
        assertThat(publishedCount).isEqualTo(3);
        assertThat(draftCount).isEqualTo(1);
    }

    @Test
    void incrementLikesCount_ValidPost_IncrementsCount() {
        // Act
        int updatedCount = postRepository.incrementLikesCount(post1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Post updatedPost = entityManager.find(Post.class, post1.getId());
        assertThat(updatedPost.getLikesCount()).isEqualTo(11); // Was 10
    }

    @Test
    void incrementLikesCount_NonExistentPost_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        int updatedCount = postRepository.incrementLikesCount(nonExistentId);

        // Assert
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void decrementLikesCount_ValidPost_DecrementsCount() {
        // Act
        int updatedCount = postRepository.decrementLikesCount(post2.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Post updatedPost = entityManager.find(Post.class, post2.getId());
        assertThat(updatedPost.getLikesCount()).isEqualTo(24); // Was 25
    }

    @Test
    void incrementCommentsCount_ValidPost_IncrementsCount() {
        // Act
        int updatedCount = postRepository.incrementCommentsCount(post3.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Post updatedPost = entityManager.find(Post.class, post3.getId());
        assertThat(updatedPost.getCommentsCount()).isEqualTo(3); // Was 2
    }

    @Test
    void decrementCommentsCount_ValidPost_DecrementsCount() {
        // Act
        int updatedCount = postRepository.decrementCommentsCount(post1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Post updatedPost = entityManager.find(Post.class, post1.getId());
        assertThat(updatedPost.getCommentsCount()).isEqualTo(4); // Was 5
    }

    @Test
    void updateStatus_ValidPost_UpdatesStatus() {
        // Act
        int updatedCount = postRepository.updateStatus(post4.getId(), PostStatus.PUBLISHED);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Post updatedPost = entityManager.find(Post.class, post4.getId());
        assertThat(updatedPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    void findTopByOrderByLikesCountDesc_ReturnsMostLikedPost() {
        // Act
        Optional<Post> topPost = postRepository.findTopByOrderByLikesCountDesc();

        // Assert
        assertThat(topPost).isPresent();
        assertThat(topPost.get().getLikesCount()).isEqualTo(25); // post2
        assertThat(topPost.get().getContent()).isEqualTo("Second post content");
    }

    @Test
    void findTopByOrderByCommentsCountDesc_ReturnsMostCommentedPost() {
        // Act
        Optional<Post> topPost = postRepository.findTopByOrderByCommentsCountDesc();

        // Assert
        assertThat(topPost).isPresent();
        assertThat(topPost.get().getCommentsCount()).isEqualTo(12); // post2
        assertThat(topPost.get().getContent()).isEqualTo("Second post content");
    }

    @Test
    void findByContentContainingIgnoreCase_ValidKeyword_ReturnsPosts() {
        // Act
        List<Post> posts = postRepository.findByContentContainingIgnoreCase("post content");

        // Assert
        assertThat(posts).hasSize(4); // All posts contain "post content"
    }

    @Test
    void findByContentContainingIgnoreCase_SpecificKeyword_ReturnsPosts() {
        // Act
        List<Post> posts = postRepository.findByContentContainingIgnoreCase("second");

        // Assert
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getContent()).isEqualTo("Second post content");
    }

    @Test
    void findByContentContainingIgnoreCase_NoMatches_ReturnsEmptyList() {
        // Act
        List<Post> posts = postRepository.findByContentContainingIgnoreCase("nonexistent");

        // Assert
        assertThat(posts).isEmpty();
    }

    @Test
    void findAllPublishedPosts_ReturnsPublishedPosts() {
        // Act
        Page<Post> page = postRepository.findAllPublishedPosts(PageRequest.of(0, 10));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3); // post1, post2, post3
        assertThat(page.getContent()).extracting(Post::getStatus)
                .containsOnly(PostStatus.PUBLISHED);
    }

    @Test
    void findAllPublishedPosts_WithPagination_ReturnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Act
        Page<Post> page = postRepository.findAllPublishedPosts(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Third post content"); // Newest
        assertThat(page.getContent().get(1).getContent()).isEqualTo("Second post content");
    }

    @Test
    void deleteByAuthorId_ValidAuthor_DeletesPosts() {
        // Act
        long deletedCount = postRepository.deleteByAuthorId(author1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(deletedCount).isEqualTo(3); // post1, post2, post4
        List<Post> remainingPosts = postRepository.findAll();
        assertThat(remainingPosts).hasSize(1); // post3
        assertThat(remainingPosts.get(0).getAuthor().getId()).isEqualTo(author2.getId());
    }

    @Test
    void deleteByAuthorId_NonExistentAuthor_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        long deletedCount = postRepository.deleteByAuthorId(nonExistentId);

        // Assert
        assertThat(deletedCount).isEqualTo(0);
    }
}