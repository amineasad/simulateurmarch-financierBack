package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "education_resources")
public class EducationResource {

    public enum ResourceType { ARTICLE, VIDEO }

    // NEW: difficulty levels
    public enum Difficulty { EASY, MID, ADV }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    // For ARTICLE: store the article text (or short summary)
    @Column(columnDefinition = "TEXT")
    private String content;

    // For VIDEO: store a URL (YouTube, Vimeo, or internal)
    private String url;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    // NEW: difficulty with a default (MID)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty = Difficulty.MID;

    // Constructors
    public EducationResource() {}

    public EducationResource(String title, ResourceType type, String content, String url, String description) {
        this.title = title;
        this.type = type;
        this.content = content;
        this.url = url;
        this.description = description;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // NEW
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = (difficulty == null) ? Difficulty.MID : difficulty; }
}
