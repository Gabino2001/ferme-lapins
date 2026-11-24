package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService service;

    @GetMapping("/etat")
    public Map<String, Boolean> etat() { return Map.of("actif", service.actif()); }

    @PostMapping
    public Map<String, String> poser(@AuthenticationPrincipal Long uid, @RequestBody Dtos.QuestionAssistant q) {
        return Map.of("reponse", service.repondre(uid, q));
    }
}
