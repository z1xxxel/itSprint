package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.AchievementResponseDTO;
import com.sprint.tms_backend.DTO.GrantAchievementRequest;
import com.sprint.tms_backend.model.Achievement;
import com.sprint.tms_backend.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AchievementResponseDTO>> getUserAchievements(@PathVariable Long userId) {
        return ResponseEntity.ok(achievementService.getUserAchievements(userId));
    }

    @PostMapping("/grant")
    public ResponseEntity<String> grantAchievement(@RequestBody GrantAchievementRequest request) {
        achievementService.grantManualAchievement(
                request.actorId(),
                request.userId(),
                request.achievementCode()
        );
        return ResponseEntity.ok("Ачивка успешно выдана");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }
}