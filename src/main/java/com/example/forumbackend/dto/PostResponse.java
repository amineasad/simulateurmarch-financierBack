package com.example.forumbackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        String authorFullName,      // nom + prenom
        String authorPhotoProfil,   // photoProfil du User
        int likeCount,
        boolean userLiked,
        List<CommentDto> comments,
        String imagePath) {}
