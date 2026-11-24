package com.ferme.lapins.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.security.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Assistant d'élevage : appelle l'API Claude avec les données de la ferme. Désactivé sans ANTHROPIC_API_KEY. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {
    private static final int TAILLE_PHOTO_MAX = 1_500_000;

    @Value("${ANTHROPIC_API_KEY:}") private String cle;
    @Value("${ANTHROPIC_MODEL:claude-sonnet-5-5}") private String modele;
    @Value("${ASSISTANT_LIMITE:30}") private int limite;

    private final ContexteFermeService contexte;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<Long, Deque<Instant>> appels = new ConcurrentHashMap<>();

    public boolean actif() { return cle != null && !cle.isBlank(); }

    public String repondre(Long uid, Dtos.QuestionAssistant q) {
        if (!actif()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Assistant non configuré : définissez la variable ANTHROPIC_API_KEY sur le serveur");
        }
        String question = q.question() == null ? "" : q.question().trim();
        boolean photo = q.photo() != null && !q.photo().isBlank();
        if (question.isEmpty() && !photo) throw new IllegalArgumentException("Question vide");
        if (question.length() > 2000) throw new IllegalArgumentException("Question trop longue (2000 caractères maximum)");
        if (photo && q.photo().length() > TAILLE_PHOTO_MAX) throw new IllegalArgumentException("Photo trop lourde");
        limiter(Auth.utilisateurId() != null ? Auth.utilisateurId() : uid);

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", modele);
            body.put("max_tokens", 900);
            body.put("system", systeme(uid));
            ArrayNode messages = body.putArray("messages");

            List<Dtos.MessageChat> histo = q.historique() == null ? List.of() : q.historique();
            int debut = Math.max(0, histo.size() - 10);
            boolean premier = true;
            for (Dtos.MessageChat m : histo.subList(debut, histo.size())) {
                if (m.content() == null || m.content().isBlank()) continue;
                String role = "assistant".equals(m.role()) ? "assistant" : "user";
                if (premier && !"user".equals(role)) continue; // la conversation doit commencer par l'utilisateur
                premier = false;
                ObjectNode n = messages.addObject();
                n.put("role", role);
                n.put("content", m.content().length() > 4000 ? m.content().substring(0, 4000) : m.content());
            }
            ObjectNode courant = messages.addObject();
            courant.put("role", "user");
            if (photo) {
                ArrayNode contenu = courant.putArray("content");
                ObjectNode image = contenu.addObject();
                image.put("type", "image");
                ObjectNode source = image.putObject("source");
                source.put("type", "base64");
                source.put("media_type", typeImage(q.photo()));
                source.put("data", q.photo());
                ObjectNode texte = contenu.addObject();
                texte.put("type", "text");
                texte.put("text", question.isEmpty() ? "Que vois-tu sur cette photo de lapin ?" : question);
            } else {
                courant.put("content", question);
            }

            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(60))
                    .header("x-api-key", cle)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                log.warn("Assistant : réponse {} de l'API : {}", res.statusCode(), res.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L’assistant est indisponible pour le moment");
            }
            JsonNode racine = mapper.readTree(res.body());
            StringBuilder sb = new StringBuilder();
            for (JsonNode bloc : racine.path("content")) {
                if ("text".equals(bloc.path("type").asText())) sb.append(bloc.path("text").asText());
            }
            String reponse = sb.toString().trim();
            return reponse.isEmpty() ? "Je n’ai pas de réponse à proposer pour le moment." : reponse;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L’assistant est indisponible pour le moment");
        } catch (Exception e) {
            log.warn("Assistant : erreur {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L’assistant est indisponible pour le moment");
        }
    }

    private String systeme(Long uid) {
        return """
                Tu es l'assistant d'une application de gestion de ferme de lapins. Tu aides l'éleveur à comprendre ce qui se passe \
                dans sa ferme et à prendre de bonnes décisions. Réponds en français, simplement, de façon courte et concrète, \
                sans jargon inutile. Appuie-toi sur les données de la ferme ci-dessous ; si une information manque, dis-le au lieu de l'inventer. \
                Santé : donne des pistes et des gestes de précaution (isoler, hygiène, eau propre, ventilation...), mais ne pose jamais \
                de diagnostic certain, ne donne ni dose ni nom de médicament précis, et recommande un vétérinaire pour toute maladie, \
                mortalité anormale ou doute. Si on te montre une photo d'un lapin, décris ce que tu observes et les causes possibles, \
                avec les mêmes précautions. Les montants sont exprimés dans la devise indiquée dans les données.

                DONNÉES DE LA FERME :
                """ + contexte.construire(uid);
    }

    private void limiter(Long cleUtilisateur) {
        Deque<Instant> d = appels.computeIfAbsent(cleUtilisateur, k -> new ArrayDeque<>());
        synchronized (d) {
            Instant seuil = Instant.now().minus(Duration.ofHours(1));
            while (!d.isEmpty() && d.peekFirst().isBefore(seuil)) d.pollFirst();
            if (d.size() >= limite) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite atteinte : " + limite + " questions par heure");
            }
            d.addLast(Instant.now());
        }
    }

    private String typeImage(String base64) {
        if (base64.startsWith("iVBOR")) return "image/png";
        if (base64.startsWith("R0lG")) return "image/gif";
        if (base64.startsWith("UklG")) return "image/webp";
        return "image/jpeg";
    }
}
