package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.Post;
import com.platform.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class UserPostRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    // ==================== UserRepository ====================

    @Test
    void userRepository_FindByUsername_ReturnsUser() {
        User user = User.builder().username("testuser").email("test@test.com").role("USER").build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void userRepository_FindByUsername_NotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void userRepository_ExistsByUsername_Works() {
        User user = User.builder().username("exists_user").email("e@test.com").role("USER").build();
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("exists_user"));
        assertFalse(userRepository.existsByUsername("no_such_user"));
    }

    @Test
    void userRepository_FindBannedUsers_ReturnsOnlyBanned() {
        userRepository.save(User.builder().username("normal").email("n@t.com").role("USER").banned(false).build());
        userRepository.save(User.builder().username("banned1").email("b1@t.com").role("USER").banned(true).build());
        userRepository.save(User.builder().username("banned2").email("b2@t.com").role("USER").banned(true).build());

        List<User> banned = userRepository.findBannedUsers();
        assertEquals(2, banned.size());
        assertTrue(banned.stream().allMatch(User::isBanned));
    }

    // ==================== PostRepository ====================

    @Test
    void postRepository_FindFeed_ExcludesDeletedAndOrdersByCreatedAtDesc() {
        postRepository.save(Post.builder().content("First").authorId("a1").build());
        postRepository.save(Post.builder().content("Second").authorId("a1").build());
        postRepository.save(Post.builder().content("Deleted").authorId("a1").deletedAt(LocalDateTime.now()).build());

        List<Post> feed = postRepository.findFeed(PageRequest.of(0, 10));
        assertEquals(2, feed.size());
        assertTrue(feed.stream().noneMatch(p -> "Deleted".equals(p.getContent())));
    }

    @Test
    void postRepository_FindFeedByCategory_FiltersCorrectly() {
        postRepository.save(Post.builder().content("Nutrition 1").authorId("a1").category("nutrition").build());
        postRepository.save(Post.builder().content("General 1").authorId("a1").category("general").build());

        List<Post> nutritionFeed = postRepository.findFeedByCategory("nutrition", PageRequest.of(0, 10));
        assertEquals(1, nutritionFeed.size());
        assertEquals("Nutrition 1", nutritionFeed.get(0).getContent());
    }

    @Test
    void postRepository_IncrementViews_IncrementsCount() {
        Post post = Post.builder().content("View test").authorId("a1").viewsCount(5).build();
        Post saved = postRepository.save(post);
        postRepository.flush();

        postRepository.incrementViews(saved.getId());
        postRepository.flush();
        entityManager.clear();

        Post updated = postRepository.findById(saved.getId()).orElseThrow();
        assertEquals(6, updated.getViewsCount());
    }
}
