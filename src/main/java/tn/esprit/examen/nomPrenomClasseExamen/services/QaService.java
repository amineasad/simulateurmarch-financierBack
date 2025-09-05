package tn.esprit.examen.nomPrenomClasseExamen.services;

import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Question;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Reponse;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.QuestionRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.ReponseRepository;

@Service
public class QaService {

    private final QuestionRepository questionRepo;
    private final ReponseRepository reponseRepo;
    private final OllamaService ollamaService;

    public QaService(QuestionRepository qR, ReponseRepository rR, OllamaService oS){
        this.questionRepo = qR;
        this.reponseRepo = rR;
        this.ollamaService = oS;
    }

    public Question poserQuestionEtGenererReponse(String contenu) {
        Question q = new Question();
        q.setContenu(contenu);
        Question savedQ = questionRepo.save(q);

        // Appel Ollama (synchronous)
        String aiAnswer = ollamaService.askModel("llama3.2", contenu);

        Reponse r = new Reponse();
        r.setContenu(aiAnswer);
        r.setQuestion(savedQ);
        Reponse savedR = reponseRepo.save(r);

        // Lier la réponse à la question et resauvegarder
        savedQ.setReponse(savedR);
        questionRepo.save(savedQ);

        return savedQ;
    }


    public Question poserQuestion(String contenu) {
        Question q = new Question();
        q.setContenu(contenu);
        return questionRepo.save(q);
    }

    public Reponse ajouterReponse(Long questionId, String contenu) {
        Question question = questionRepo.findById(questionId).orElseThrow();
        Reponse r = new Reponse();
        r.setContenu(contenu);
        r.setQuestion(question);
        return reponseRepo.save(r);
    }

    public Reponse getReponseByQuestionId(Long questionId) {
        return reponseRepo.findByQuestionId(questionId);
    }
}
