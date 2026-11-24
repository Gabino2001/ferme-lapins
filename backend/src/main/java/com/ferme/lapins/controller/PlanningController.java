package com.ferme.lapins.controller;

import com.ferme.lapins.dto.LignePlanning;
import com.ferme.lapins.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {
    private final PlanningService service;

    @GetMapping
    public List<LignePlanning> planning(@AuthenticationPrincipal Long uid, @RequestParam(defaultValue = "21") int delai) {
        return service.calculer(uid, delai);
    }
}
