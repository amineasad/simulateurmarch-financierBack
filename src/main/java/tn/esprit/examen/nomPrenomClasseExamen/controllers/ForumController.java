package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.examen.nomPrenomClasseExamen.dto.AddCommentRequest;
import tn.esprit.examen.nomPrenomClasseExamen.dto.CreatePostRequest;
import tn.esprit.examen.nomPrenomClasseExamen.dto.PostResponse;
import tn.esprit.examen.nomPrenomClasseExamen.services.ForumService;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Slf4j
public class ForumController {

    private final ForumService forumService;

    // ========== RÉCUPÉRATION DES POSTS ==========

    /**
     * Récupère tous les posts
     * GET /api/forum/posts
     */
    @GetMapping("/posts")
    public List<PostResponse> getAllPosts() {
        log.info("📋 Récupération de tous les posts");
        return forumService.getAllPosts();
    }

    // ========== CRÉATION DE POSTS ==========

    /**
     * ✅ Post simple (texte seulement) - AVEC USER ID
     * POST /api/forum/post/user/{userId}
     * Body: { "content": "Mon message" }
     */
    @PostMapping("/post/user/{userId}")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable Long userId,
            @Valid @RequestBody CreatePostRequest req) {
        log.info("✍️ Création d'un post pour l'utilisateur {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(forumService.createPost(userId, req));
    }

    /**
     * ✅ Post avec image (multipart) - AVEC USER ID
     * POST /api/forum/with-image/user/{userId}
     * Form-data:
     *   - content: "Mon message" (optionnel)
     *   - image: (file)
     */
    @PostMapping("/with-image/user/{userId}")
    public ResponseEntity<PostResponse> createPostWithImage(
            @PathVariable Long userId,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam("image") MultipartFile image) {
        try {
            log.info("✍️ Création d'un post avec image pour l'utilisateur {}", userId);
            PostResponse savedPost = forumService.createPostWithImage(userId, content, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du post avec image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== COMMENTAIRES ==========

    /**
     * ✅ Ajoute un commentaire - AVEC USER ID
     * POST /api/forum/posts/{postId}/comment/user/{userId}
     * Body: { "content": "Mon commentaire" }
     */
    @PostMapping("/posts/{postId}/comment/user/{userId}")
    public ResponseEntity<Void> addComment(
            @PathVariable Long postId,
            @PathVariable Long userId,
            @Valid @RequestBody AddCommentRequest request) {
        log.info("💬 Ajout d'un commentaire sur post {} par utilisateur {}", postId, userId);
        forumService.addComment(userId, postId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ========== LIKES ==========

    /**
     * ✅ Toggle like - AVEC USER ID
     * POST /api/forum/posts/{postId}/like/user/{userId}
     */
    @PostMapping("/posts/{postId}/like/user/{userId}")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long postId,
            @PathVariable Long userId) {
        log.info("👍 Toggle like sur post {} par utilisateur {}", postId, userId);
        forumService.toggleLike(userId, postId);
        return ResponseEntity.ok().build();
    }

    // ========== 🔄 ENDPOINTS COMPATIBLES (pour ne pas casser l'ancien frontend) ==========

    /**
     * ⚠️ DEPRECATED : Utiliser /post/user/{userId} à la place
     * POST /api/forum/post
     *
     * Cette version utilise l'userId=1 par défaut pour la compatibilité
     */
    @Deprecated
    @PostMapping("/post")
    public ResponseEntity<PostResponse> createPostLegacy(@Valid @RequestBody CreatePostRequest req) {
        log.warn("⚠️ Utilisation de l'ancien endpoint /post (sans userId). Utilise userId=1 par défaut");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(forumService.createPost(1L, req));
    }

    /**
     * ⚠️ DEPRECATED : Utiliser /with-image/user/{userId} à la place
     * POST /api/forum/with-image
     */
    @Deprecated
    @PostMapping("/with-image")
    public ResponseEntity<PostResponse> createPostWithImageLegacy(
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam("image") MultipartFile image) {
        try {
            log.warn("⚠️ Utilisation de l'ancien endpoint /with-image (sans userId). Utilise userId=1 par défaut");
            PostResponse savedPost = forumService.createPostWithImage(1L, content, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du post avec image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ⚠️ DEPRECATED : Utiliser /posts/{postId}/comment/user/{userId} à la place
     * POST /api/forum/posts/{postId}/comment
     */
    @Deprecated
    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<Void> addCommentLegacy(
            @PathVariable Long postId,
            @Valid @RequestBody AddCommentRequest request) {
        log.warn("⚠️ Utilisation de l'ancien endpoint /comment (sans userId). Utilise userId=1 par défaut");
        forumService.addComment(1L, postId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * ⚠️ DEPRECATED : Utiliser /posts/{postId}/like/user/{userId} à la place
     * POST /api/forum/posts/{postId}/like
     */
    @Deprecated
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> toggleLikeLegacy(@PathVariable Long postId) {
        log.warn("⚠️ Utilisation de l'ancien endpoint /like (sans userId). Utilise userId=1 par défaut");
        forumService.toggleLike(1L, postId);
        return ResponseEntity.ok().build();
    }
}