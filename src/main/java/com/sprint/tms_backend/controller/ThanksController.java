package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.ThanksFeedResponseDTO;
import com.sprint.tms_backend.DTO.ThanksStatsDTO;
import com.sprint.tms_backend.DTO.UserThanksDTO;
import com.sprint.tms_backend.service.ThanksService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thanks")
@RequiredArgsConstructor
public class ThanksController {

    private final ThanksService thanksService;

    @GetMapping("/feed")
    public ResponseEntity<List<ThanksFeedResponseDTO>> getFeed() {
        return ResponseEntity.ok(thanksService.getFeed());
    }

    // История благодарностей конкретного пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UserThanksDTO>> getUserThanks(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "all") String direction, // all | received | sent
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(thanksService.getUserThanks(userId, direction, page, size));
    }

    // Статистика по благодарностям
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ThanksStatsDTO> getStats(@PathVariable Long userId) {
        return ResponseEntity.ok(thanksService.getStats(userId));
    }
}