package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CreateQuestRequest;
import com.sprint.tms_backend.DTO.QuestResponseDTO;
import com.sprint.tms_backend.model.Quest;
import com.sprint.tms_backend.model.UserQuest;
import com.sprint.tms_backend.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // --- Для менеджеров ---
    @PostMapping
    public ResponseEntity<Quest> createQuest(@RequestBody CreateQuestRequest request) {
        return new ResponseEntity<>(questService.createQuest(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<String> deleteQuest(
            @PathVariable Long questId,
            @RequestParam Long actorId) {
        questService.deleteQuest(questId, actorId);
        return ResponseEntity.ok("Квест удален");
    }

    @GetMapping("/review")
    public ResponseEntity<List<UserQuest>> getQuestsForReview(@RequestParam Long actorId) {
        return ResponseEntity.ok(questService.getQuestsForReview(actorId));
    }

    @PutMapping("/review/{userQuestId}/approve")
    public ResponseEntity<String> approveQuest(
            @PathVariable Long userQuestId,
            @RequestParam Long actorId) {
        questService.approveQuest(userQuestId, actorId);
        return ResponseEntity.ok("Квест подтвержден, монеты начислены");
    }

    @PutMapping("/review/{userQuestId}/reject")
    public ResponseEntity<String> rejectQuest(
            @PathVariable Long userQuestId,
            @RequestParam Long actorId,
            @RequestParam String reason) {
        questService.rejectQuest(userQuestId, actorId, reason);
        return ResponseEntity.ok("Квест отклонен");
    }

    // --- Для сотрудников ---
    @GetMapping
    public ResponseEntity<List<QuestResponseDTO>> getAvailableQuests(@RequestParam Long userId) {
        return ResponseEntity.ok(questService.getAvailableQuests(userId));
    }

    @PostMapping("/{questId}/start")
    public ResponseEntity<String> startQuest(
            @PathVariable Long questId,
            @RequestParam Long userId) {
        questService.startQuest(questId, userId);
        return ResponseEntity.ok("Квест взят в работу");
    }

    @PostMapping("/{questId}/submit")
    public ResponseEntity<String> submitQuest(
            @PathVariable Long questId,
            @RequestParam Long userId) {
        questService.submitQuest(questId, userId);
        return ResponseEntity.ok("Квест отправлен на проверку");
    }
}