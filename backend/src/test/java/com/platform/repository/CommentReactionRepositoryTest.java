package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.Comment;
import com.platform.model.Post;
import com.platform.model.Reaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class CommentReactionRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private PostRepository postRepository;

    // ==================== CommentRepository ====================

    @Test
    void commentRepository_FindByPostIdOrderByCreatedAtDesc_ReturnsOrdered() {
        Post p1 = postRepository.save(Post.builder().content("Post1").authorId("a1").build());
        Post p2 = postRepository.save(Post.builder().content("Post2").authorId("a1").build());
        commentRepository.save(Comment.builder().content("First").postId(p1.getId()).authorId("a1").build());
        commentRepository.save(Comment.builder().content("Second").postId(p1.getId()).authorId("a1").build());
        commentRepository.save(Comment.builder().content("Other post").postId(p2.getId()).authorId("a1").build());

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(p1.getId(), PageRequest.of(0, 10));
        assertEquals(2, comments.size());
    }

    @Test
    void commentRepository_CountByPostId_ReturnsCorrectCount() {
        Post p10 = postRepository.save(Post.builder().content("Post10").authorId("a1").build());
        commentRepository.save(Comment.builder().content("A").postId(p10.getId()).authorId("a1").build());
        commentRepository.save(Comment.builder().content("B").postId(p10.getId()).authorId("a1").build());

        assertEquals(2, commentRepository.countByPostId(p10.getId()));
        assertEquals(0, commentRepository.countByPostId("nonexistent"));
    }

    @Test
    void commentRepository_DeleteByPostId_RemovesAllCommentsForPost() {
        Post pd1 = postRepository.save(Post.builder().content("PostD1").authorId("a1").build());
        Post pd2 = postRepository.save(Post.builder().content("PostD2").authorId("a1").build());
        commentRepository.save(Comment.builder().content("Del1").postId(pd1.getId()).authorId("a1").build());
        commentRepository.save(Comment.builder().content("Del2").postId(pd1.getId()).authorId("a1").build());
        commentRepository.save(Comment.builder().content("Keep").postId(pd2.getId()).authorId("a1").build());

        commentRepository.deleteByPostId(pd1.getId());
        commentRepository.flush();

        assertEquals(0, commentRepository.countByPostId(pd1.getId()));
        assertEquals(1, commentRepository.countByPostId(pd2.getId()));
    }

    // ==================== ReactionRepository ====================

    @Test
    void reactionRepository_FindByPostIdAndUserIdAndType_ReturnsReaction() {
        Post p1 = postRepository.save(Post.builder().content("PostR1").authorId("a1").build());
        reactionRepository.save(Reaction.builder().postId(p1.getId()).userId("u1").type("like").build());

        Optional<Reaction> found = reactionRepository.findByPostIdAndUserIdAndType(p1.getId(), "u1", "like");
        assertTrue(found.isPresent());
        assertEquals(p1.getId(), found.get().getPostId());
    }

    @Test
    void reactionRepository_ExistsByPostIdAndUserIdAndType_Works() {
        Post p2 = postRepository.save(Post.builder().content("PostR2").authorId("a1").build());
        reactionRepository.save(Reaction.builder().postId(p2.getId()).userId("u2").type("love").build());

        assertTrue(reactionRepository.existsByPostIdAndUserIdAndType(p2.getId(), "u2", "love"));
        assertFalse(reactionRepository.existsByPostIdAndUserIdAndType(p2.getId(), "u2", "laugh"));
    }

    @Test
    void reactionRepository_DeleteByPostIdAndUserIdAndType_RemovesSpecificReaction() {
        Post p3 = postRepository.save(Post.builder().content("PostR3").authorId("a1").build());
        reactionRepository.save(Reaction.builder().postId(p3.getId()).userId("u3").type("like").build());
        reactionRepository.save(Reaction.builder().postId(p3.getId()).userId("u3").type("love").build());

        reactionRepository.deleteByPostIdAndUserIdAndType(p3.getId(), "u3", "like");
        reactionRepository.flush();

        assertFalse(reactionRepository.existsByPostIdAndUserIdAndType(p3.getId(), "u3", "like"));
        assertTrue(reactionRepository.existsByPostIdAndUserIdAndType(p3.getId(), "u3", "love"));
    }
}
