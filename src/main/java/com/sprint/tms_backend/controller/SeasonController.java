package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.LeaderboardEntryDTO;
import com.sprint.tms_backend.DTO.SeasonInfoDTO;
import com.sprint.tms_backend.DTO.SeasonResultDTO;
import com.sprint.tms_backend.model.Season;
import com.sprint.tms_backend.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    // Текущий сезон + лидерборд (доступно всем)
    // Текущий сезон. Передаем actorId (или userId), чтобы вернуть данные по его отделу
    @GetMapping("/current")
    public ResponseEntity<SeasonInfoDTO> getCurrentSeason(
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(seasonService.getCurrentSeasonInfo(actorId));
    }

    // Лидерборд (доступно всем)
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard() {
        return ResponseEntity.ok(seasonService.getLeaderboard());
    }

    // История закрытых сезонов (доступно всем)
    @GetMapping("/history")
    public ResponseEntity<List<Season>> getHistory() {
        return ResponseEntity.ok(seasonService.getPastSeasons());
    }

    // Результаты конкретного сезона (топ игроков)
    @GetMapping("/{id}/results")
    public ResponseEntity<List<SeasonResultDTO>> getSeasonResults(@PathVariable Long id) {
        return ResponseEntity.ok(seasonService.getSeasonResults(id));
    }
}