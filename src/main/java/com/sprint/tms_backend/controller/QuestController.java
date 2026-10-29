package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.Quest;
import com.sprint.tms_backend.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // ==========================================
    // === ADMIN: создание с аудиторией ===
    // ==========================================
    @PostMapping("/admin/create")
    public ResponseEntity<Quest> createQuestWithAudience(
            @RequestBody CreateQuestWithAudienceRequest req) {
        return new ResponseEntity<>(
                questService.createQuestWithAudience(req), HttpStatus.CREATED);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<Quest> updateQuest(
            @PathVariable Long id,
            @RequestBody UpdateQuestRequest req) {
        return ResponseEntity.ok(questService.updateQuest(id, req));
    }

    @PutMapping("/admin/{id}/extend-deadline")
    public ResponseEntity<Quest> extendDeadline(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam String newDeadline) {
        LocalDateTime dt = LocalDateTime.parse(newDeadline);
        return ResponseEntity.ok(questService.extendDeadline(id, actorId, dt));
    }

    @GetMapping("/admin/{id}/participants")
    public ResponseEntity<List<AdminQuestParticipantDTO>> getParticipants(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(questService.getQuestParticipants(id, actorId));
    }

    @PostMapping("/admin/{id}/apply-penalty")
    public ResponseEntity<Map<String, Object>> applyPenalty(
            @PathVariable Long id,
            @RequestBody ApplyQuestPenaltyRequest req) {
        return ResponseEntity.ok(questService.applyQuestPenalty(id, req));
    }

    // ==========================================
    // === СОЗДАНИЕ (старое, публичное) ===
    // ==========================================
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

    // ==========================================
    // === РЕВЬЮ ===
    // ==========================================
    @GetMapping("/review")
    public ResponseEntity<List<UserQuestForReview>> getQuestsForReview(@RequestParam Long actorId) {
        return ResponseEntity.ok(questService.getQuestsForReview(actorId));
    }

    @GetMapping("/review/user/{userId}")
    public ResponseEntity<List<UserQuestForReview>> getQuestsForReviewByUser(
            @PathVariable Long userId,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(questService.getQuestsForReviewByUser(userId, actorId));
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

    // ==========================================
    // === СОТРУДНИК ===
    // ==========================================
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

    @GetMapping("/admin/logs")
    public ResponseEntity<List<AdminQuestLogDTO>> getAllQuestLogs(@RequestParam Long actorId) {
        return ResponseEntity.ok(questService.getAllQuestLogs(actorId));
    }
}