package com.example.forumbackend.services;

import com.example.forumbackend.dto.CommentDto;
import com.example.forumbackend.dto.CreatePostRequest;
import com.example.forumbackend.dto.PostResponse;
import com.example.forumbackend.entities.User;
import com.example.forumbackend.entities.forum.ForumComment;
import com.example.forumbackend.entities.forum.ForumLike;
import com.example.forumbackend.entities.forum.ForumPost;
import com.example.forumbackend.repositories.ForumLikeRepository;
import com.example.forumbackend.repositories.ForumPostRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumLikeRepository likeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String UPLOAD_DIR = "C:/forum-uploads/"; // Change selon ton OS

    @PostConstruct
    public void init() {
        new File(UPLOAD_DIR).mkdirs(); // crée le dossier s'il n'existe pas
    }



    //hedhom users static li testit behom
    // 3 utilisateurs simulés (comme avant)
    private User getCurrentUser() {
        Random rand = new Random();
        long[] ids = {1L, 2L, 3L};
        long userId = ids[rand.nextInt(3)];

        User user = new User();
        user.setId(userId);
        user.setNom(switch ((int) userId) {
            case 1 -> "Dev";
            case 2 -> "Dupont";
            case 3 -> "Martin";
            default -> "Inconnu";
        });
        user.setPrenom(switch ((int) userId) {
            case 1 -> "Forum";
            case 2 -> "Lucas";
            case 3 -> "Emma";
            default -> "X";
        });
        user.setPhotoProfil(switch ((int) userId) {
            case 1 -> "https://via.placeholder.com/150";
            case 2 -> "https://randomuser.me/api/portraits/men/32.jpg";
            case 3 -> "https://randomuser.me/api/portraits/women/44.jpg";
            default -> "https://via.placeholder.com/150";
        });
        return user;
    }

    // NOUVEAU : tous les posts (triés par date)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PostResponse createPostWithImage(String content, MultipartFile image) throws IOException {
        User author = getCurrentUser(); // même utilisateur simulé que partout

        ForumPost post = new ForumPost();
        post.setContent(content);
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        post.setLikeCount(0);

        // === UPLOAD IMAGE ===
        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            post.setImagePath("/uploads/" + fileName); // accessible via http://localhost:8080/uploads/...
        }

        post = postRepository.save(post);

        // Envoi en temps réel à tout le monde
        PostResponse response = toResponse(post);
        messagingTemplate.convertAndSend("/topic/global-forum", response);

        return response;
    }

    public void toggleLike(Long postId) {
        User user = getCurrentUser();
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post introuvable"));

        likeRepository.findByUserIdAndPostId(user.getId(), postId)
                .ifPresentOrElse(
                        like -> {
                            likeRepository.delete(like);
                            post.setLikeCount(post.getLikeCount() - 1);
                        },
                        () -> {
                            likeRepository.save(ForumLike.builder().user(user).post(post).build());
                            post.setLikeCount(post.getLikeCount() + 1);
                        }
                );

        postRepository.save(post);
        messagingTemplate.convertAndSend("/topic/global-forum", toResponse(post));
    }

    public void addComment(Long postId, String content) {
        User author = getCurrentUser();
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post introuvable"));

        ForumComment comment = new ForumComment();
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());

        post.getComments().add(comment);
        postRepository.save(post); // Cascade.ALL

        messagingTemplate.convertAndSend("/topic/global-forum", toResponse(post));
    }

    private PostResponse toResponse(ForumPost post) {
        User current = getCurrentUser();
        boolean liked = likeRepository.findByUserIdAndPostId(current.getId(), post.getId()).isPresent();

        List<CommentDto> comments = post.getComments() != null ?
                post.getComments().stream()
                        .map(c -> new CommentDto(
                                c.getId(),
                                c.getContent(),
                                c.getAuthor() != null ? c.getAuthor().getNom() + " " + c.getAuthor().getPrenom() : "Anonyme",
                                c.getCreatedAt()
                        ))
                        .toList() : List.of();

        String authorName = post.getAuthor() != null ?
                post.getAuthor().getNom() + " " + post.getAuthor().getPrenom() : "Inconnu";

        String photo = post.getAuthor() != null && post.getAuthor().getPhotoProfil() != null ?
                post.getAuthor().getPhotoProfil() : "https://via.placeholder.com/150";
        String imagePath = post.getImagePath();
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getCreatedAt(),
                authorName,
                photo,
                post.getLikeCount(),
                liked,
                comments,
                imagePath
        );
    }

    // Création d’un post simple (texte seulement) — MANQUAIT !
    public PostResponse createPost(CreatePostRequest req) {
        User author = getCurrentUser();

        ForumPost post = new ForumPost();
        post.setContent(req.content());
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        post.setLikeCount(0);

        post = postRepository.save(post);

        PostResponse response = toResponse(post);
        messagingTemplate.convertAndSend("/topic/global-forum", response);

        return response;
    }
}