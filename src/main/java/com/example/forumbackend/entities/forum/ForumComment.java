package com.example.forumbackend.entities.forum;
import jakarta.persistence.*;
import lombok.*;
import com.example.forumbackend.entities.User;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "forum_comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
