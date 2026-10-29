package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.SystemSetting;
import com.sprint.tms_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getDashboard(actorId));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSetting>> getSettings(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getSettings(actorId));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<SystemSetting> updateSetting(@PathVariable String key, @RequestParam String value, @RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.updateSetting(key, value, actorId));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketDTO>> getAllTickets(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getAllTickets(actorId));
    }

    @GetMapping("/quests")
    public ResponseEntity<List<AdminQuestDTO>> getAllQuests(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getAllQuests(actorId));
    }

    @GetMapping("/quests/pending")
    public ResponseEntity<List<AdminQuestDTO>> getPendingQuests(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getPendingQuests(actorId));
    }

    @PutMapping("/quests/{id}/approve")
    public ResponseEntity<String> approveQuest(@PathVariable Long id, @RequestParam Long actorId) {
        adminService.approveQuest(id, actorId);
        return ResponseEntity.ok("Квест одобрен");
    }

    @PutMapping("/quests/{id}/reject")
    public ResponseEntity<String> rejectQuest(@PathVariable Long id, @RequestParam Long actorId, @RequestParam String reason) {
        adminService.rejectQuest(id, actorId, reason);
        return ResponseEntity.ok("Квест отклонен");
    }

    @PutMapping("/quests/{id}/cancel-completed")
    public ResponseEntity<String> cancelCompletedQuest(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam String reason) {
        adminService.cancelCompletedQuest(id, actorId, reason);
        return ResponseEntity.ok("Выполненный квест отменен, средства возвращены в казну");
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<CoinTransactionDTO>> getAllTransactions(
            @RequestParam Long actorId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminService.getAllTransactions(actorId, page, size));
    }

    @PutMapping("/transactions/{id}/cancel")
    public ResponseEntity<String> cancelTransaction(
            @PathVariable Long id, @RequestParam Long actorId, @RequestParam String reason) {
        adminService.cancelTransaction(id, actorId, reason);
        return ResponseEntity.ok("Транзакция отменена");
    }

    @PostMapping("/penalties")
    public ResponseEntity<String> applyPenalty(@RequestBody PenaltyRequest request) {
        adminService.applyPenalty(request);
        return ResponseEntity.ok("Штраф успешно применен");
    }
}