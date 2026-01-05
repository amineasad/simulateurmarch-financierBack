package tn.esprit.examen.nomPrenomClasseExamen.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.examen.nomPrenomClasseExamen.dto.CommentDto;
import tn.esprit.examen.nomPrenomClasseExamen.dto.CreatePostRequest;
import tn.esprit.examen.nomPrenomClasseExamen.dto.PostResponse;
import tn.esprit.examen.nomPrenomClasseExamen.entities.ForumComment;
import tn.esprit.examen.nomPrenomClasseExamen.entities.ForumLike;
import tn.esprit.examen.nomPrenomClasseExamen.entities.ForumPost;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.ForumLikeRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.ForumPostRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumLikeRepository likeRepository;
    private final UserRepository userRepository; // ✅ AJOUTÉ
    private final SimpMessagingTemplate messagingTemplate;

    private static final String UPLOAD_DIR = "C:/forum-uploads/";

    @PostConstruct
    public void init() {
        new File(UPLOAD_DIR).mkdirs();
    }

    // ========== ✅ MÉTHODE POUR OBTENIR UN UTILISATEUR (remplace les users simulés) ==========

    /**
     * Récupère un utilisateur par son ID
     * Utilise cette méthode en passant l'ID depuis le frontend
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ Utilisateur introuvable: " + userId));
    }

    // ========== POSTS ==========

    /**
     * Récupère tous les posts
     */
    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(post -> toResponse(post, null)) // Pas d'utilisateur spécifique
                .toList();
    }

    /**
     * ✅ Crée un post avec image pour un utilisateur spécifique
     */
    public PostResponse createPostWithImage(Long userId, String content, MultipartFile image) throws IOException {
        User author = getUserById(userId); // ✅ Vrai utilisateur

        ForumPost post = ForumPost.builder()
                .content(content)
                .author(author)
                .createdAt(LocalDateTime.now())
                .likeCount(0)
                .build();

        // Upload image
        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            post.setImagePath("/uploads/" + fileName);
        }

        post = postRepository.save(post);

        PostResponse response = toResponse(post, userId);
        messagingTemplate.convertAndSend("/topic/global-forum", response);

        log.info("✅ Post créé: ID={}, Auteur={}", post.getId(), author.getFullName());

        return response;
    }

    /**
     * ✅ Crée un post simple pour un utilisateur spécifique
     */
    public PostResponse createPost(Long userId, CreatePostRequest req) {
        User author = getUserById(userId); // ✅ Vrai utilisateur

        ForumPost post = ForumPost.builder()
                .content(req.content())
                .author(author)
                .createdAt(LocalDateTime.now())
                .likeCount(0)
                .build();

        post = postRepository.save(post);

        PostResponse response = toResponse(post, userId);
        messagingTemplate.convertAndSend("/topic/global-forum", response);

        log.info("✅ Post créé: ID={}, Auteur={}", post.getId(), author.getFullName());

        return response;
    }

    // ========== LIKES ==========

    /**
     * ✅ Toggle like pour un utilisateur spécifique
     */
    public void toggleLike(Long userId, Long postId) {
        User user = getUserById(userId); // ✅ Vrai utilisateur
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("❌ Post introuvable: " + postId));

        likeRepository.findByUserIdAndPostId(userId, postId)
                .ifPresentOrElse(
                        // Unlike
                        like -> {
                            likeRepository.delete(like);
                            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                            log.info("👎 Unlike: User={}, Post={}", userId, postId);
                        },
                        // Like
                        () -> {
                            ForumLike like = ForumLike.builder()
                                    .user(user)
                                    .post(post)
                                    .build();
                            likeRepository.save(like);
                            post.setLikeCount(post.getLikeCount() + 1);
                            log.info("👍 Like: User={}, Post={}", userId, postId);
                        }
                );

        postRepository.save(post);
        messagingTemplate.convertAndSend("/topic/global-forum", toResponse(post, userId));
    }

    // ========== COMMENTAIRES ==========

    /**
     * ✅ Ajoute un commentaire pour un utilisateur spécifique
     */
    public void addComment(Long userId, Long postId, String content) {
        User author = getUserById(userId); // ✅ Vrai utilisateur
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("❌ Post introuvable: " + postId));

        ForumComment comment = ForumComment.builder()
                .content(content)
                .author(author)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();

        post.getComments().add(comment);
        postRepository.save(post); // Cascade ALL

        messagingTemplate.convertAndSend("/topic/global-forum", toResponse(post, userId));

        log.info("💬 Commentaire ajouté: Post={}, Auteur={}", postId, author.getFullName());
    }

    // ========== CONVERSION DTO ==========

    /**
     * Convertit un ForumPost en PostResponse
     * @param post Le post à convertir
     * @param currentUserId L'ID de l'utilisateur qui consulte (pour savoir s'il a liké)
     */
    private PostResponse toResponse(ForumPost post, Long currentUserId) {
        // Vérifier si l'utilisateur actuel a liké (si on a un userId)
        boolean liked = false;
        if (currentUserId != null) {
            liked = likeRepository.findByUserIdAndPostId(currentUserId, post.getId()).isPresent();
        }

        // Convertir les commentaires
        List<CommentDto> comments = post.getComments() != null ?
                post.getComments().stream()
                        .map(c -> new CommentDto(
                                c.getId(),
                                c.getContent(),
                                c.getAuthor() != null ? c.getAuthor().getFullName() : "Anonyme",
                                c.getCreatedAt()
                        ))
                        .toList() : List.of();

        // Infos de l'auteur
        String authorName = post.getAuthor() != null ?
                post.getAuthor().getFullName() : "Inconnu";

        String authorPhoto = post.getAuthor() != null ?
                post.getAuthor().getProfilePhotoOrDefault() :
                "https://via.placeholder.com/150";

        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getCreatedAt(),
                authorName,
                authorPhoto,
                post.getLikeCount(),
                liked,
                comments,
                post.getImagePath()
        );
    }
}