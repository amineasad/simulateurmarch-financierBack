package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Question;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Reponse;
import tn.esprit.examen.nomPrenomClasseExamen.services.QaService;

import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@CrossOrigin("*")
public class QaController {


    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/question")
    public Question createQuestion(@RequestParam String contenu) {
        return qaService.poserQuestion(contenu);
    }

    @PostMapping("/reponse")
    public Reponse createReponse(@RequestParam Long questionId, @RequestParam String contenu) {
        return qaService.ajouterReponse(questionId, contenu);
    }

    @GetMapping("/reponse/{questionId}")
    public Reponse getReponse(@PathVariable Long questionId) {
        return qaService.getReponseByQuestionId(questionId);
    }
    @PostMapping("/ask")
    public ResponseEntity<?> askAndAnswer(@RequestBody Map<String,String> body){
        String contenu = body.get("contenu");
        Question q = qaService.poserQuestionEtGenererReponse(contenu);
        // récupérer la réponse liée (ou renvoyer un DTO)
        Reponse r = q.getReponse(); // si bidirectionnel et initialisé
        return ResponseEntity.ok(Map.of("question", q, "reponse", r != null ? r.getContenu() : ""));
    }
}
