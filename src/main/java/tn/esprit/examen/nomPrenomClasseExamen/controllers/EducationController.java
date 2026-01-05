package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.EducationResource;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.EducationRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/education")
@CrossOrigin("*")
public class EducationController {

    private final EducationRepository repo;

    public EducationController(EducationRepository repo) {
        this.repo = repo;
    }

    // List all resources (for /education)
    @GetMapping
    public ResponseEntity<List<EducationResource>> listAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<EducationResource> res = repo.findById(id);
        return res.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create (admin)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody EducationResource resource) {
        // ensure default difficulty if missing
        if (resource.getDifficulty() == null) {
            resource.setDifficulty(EducationResource.Difficulty.MID);
        }
        // basic guard: VIDEO should have url, ARTICLE may have content
        if (resource.getType() == EducationResource.ResourceType.VIDEO && (resource.getUrl() == null || resource.getUrl().isBlank())) {
            return ResponseEntity.badRequest().body("VIDEO requires a non-empty url");
        }
        EducationResource saved = repo.save(resource);
        return ResponseEntity.ok(saved);
    }

    // Update (admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EducationResource resource) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(resource.getTitle());
            existing.setType(resource.getType());
            existing.setContent(resource.getContent());
            existing.setUrl(resource.getUrl());
            existing.setDescription(resource.getDescription());
            // NEW: update difficulty (default MID if null)
            existing.setDifficulty(resource.getDifficulty());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return repo.findById(id).map(r -> {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
