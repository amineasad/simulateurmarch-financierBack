package com.example.forumbackend.controllers;

import com.example.forumbackend.dto.AddCommentRequest;
import com.example.forumbackend.dto.CreatePostRequest;
import com.example.forumbackend.dto.PostResponse;
import com.example.forumbackend.entities.forum.ForumPost;
import com.example.forumbackend.services.ForumService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/posts")
    public List<PostResponse> getAllPosts() {
        return forumService.getAllPosts();
    }

    // Post simple (texte seulement)
    @PostMapping("/post")
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest req) {
        return forumService.createPost(req); // ← c'était ÇA le bug !
    }

    // Post avec image (multipart)
    @PostMapping("/with-image")
    public ResponseEntity<PostResponse> createPostWithImage(
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam("image") MultipartFile image) {

        try {
            PostResponse savedPost = forumService.createPostWithImage(content, image);
            return ResponseEntity.ok(savedPost);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<Void> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody AddCommentRequest request) {
        forumService.addComment(postId, request.content());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long postId) {
        forumService.toggleLike(postId);
        return ResponseEntity.ok().build();
    }
}