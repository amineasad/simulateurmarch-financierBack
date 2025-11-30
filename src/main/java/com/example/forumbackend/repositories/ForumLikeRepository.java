package com.example.forumbackend.repositories;

import com.example.forumbackend.entities.forum.ForumLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {
    Optional<ForumLike> findByUserIdAndPostId(Long userId, Long postId);
}
