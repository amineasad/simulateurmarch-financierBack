package com.example.forumbackend.dto;

import java.time.LocalDateTime;

public record CommentDto(Long id,
                         String content,
                         String authorFullName,
                         LocalDateTime createdAt
) {}
