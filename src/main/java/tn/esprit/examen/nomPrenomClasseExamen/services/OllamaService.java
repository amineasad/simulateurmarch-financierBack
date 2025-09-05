package tn.esprit.examen.nomPrenomClasseExamen.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
@Service
public class OllamaService {
    private final RestTemplate rest = new RestTemplate();
    private final String ollamaUrl = "http://localhost:11434";

    public String askModel(String model, String question) {
        String url = ollamaUrl + "/api/chat";
        Map<String,Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(Map.of("role","user","content", question)));
        payload.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> resp = rest.postForEntity(url, request, Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            Object messageObj = resp.getBody().get("message");
            if (messageObj instanceof Map) {
                Map messageMap = (Map) messageObj;
                Object content = messageMap.get("content");
                if (content != null) {
                    return content.toString();
                }
            }
        }
        return "Désolé, l'IA n'a pas pu générer de réponse.";
    }

}
