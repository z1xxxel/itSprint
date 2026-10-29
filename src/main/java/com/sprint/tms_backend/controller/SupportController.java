package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    // === Создание ===
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketDTO> createTicket(@RequestBody CreateTicketRequest request) {
        return new ResponseEntity<>(supportService.createTicket(request), HttpStatus.CREATED);
    }

    // === Списки ===
    @GetMapping("/tickets/my/{userId}")
    public ResponseEntity<List<SupportTicketDTO>> getMyTickets(@PathVariable Long userId) {
        return ResponseEntity.ok(supportService.getMyTickets(userId));
    }

    @GetMapping("/tickets/pending")
    public ResponseEntity<List<SupportTicketDTO>> getPendingTickets() {
        return ResponseEntity.ok(supportService.getPendingTickets());
    }

    @GetMapping("/tickets/assigned/{supportId}")
    public ResponseEntity<List<SupportTicketDTO>> getAssignedTickets(@PathVariable Long supportId) {
        return ResponseEntity.ok(supportService.getAssignedTickets(supportId));
    }

    // === Действия ===
    @PutMapping("/tickets/{ticketId}/take")
    public ResponseEntity<SupportTicketDTO> takeTicket(
            @PathVariable Long ticketId,
            @RequestParam Long supportId) {
        return ResponseEntity.ok(supportService.takeTicket(ticketId, supportId));
    }

    @PutMapping("/tickets/{ticketId}/complete")
    public ResponseEntity<SupportTicketDTO> completeTicket(
            @PathVariable Long ticketId,
            @RequestParam Long supportId) {
        return ResponseEntity.ok(supportService.completeTicket(ticketId, supportId));
    }

    // === Сообщения ===
    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<TicketMessageDTO> sendMessage(
            @PathVariable Long ticketId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(supportService.sendMessage(ticketId, request));
    }

    @PutMapping("/tickets/{ticketId}/messages/{messageId}")
    public ResponseEntity<TicketMessageDTO> editTicketMessage(
            @PathVariable Long ticketId,
            @PathVariable Long messageId,
            @RequestParam Long userId,
            @RequestBody EditMessageRequest body) {
        return ResponseEntity.ok(
                supportService.editTicketMessage(ticketId, messageId, userId, body.text()));
    }

    @DeleteMapping("/tickets/{ticketId}/messages/{messageId}")
    public ResponseEntity<String> deleteTicketMessage(
            @PathVariable Long ticketId,
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        supportService.deleteTicketMessage(ticketId, messageId, userId);
        return ResponseEntity.ok("Сообщение удалено");
    }

    // === Чат тикета ===
    @GetMapping("/tickets/{ticketId}/chat")
    public ResponseEntity<TicketChatResponseDTO> getTicketChat(
            @PathVariable Long ticketId,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(supportService.getTicketChat(ticketId, actorId));
    }

    // === Отзыв ===
    @PostMapping("/tickets/{ticketId}/review")
    public ResponseEntity<SupportTicketDTO> leaveReview(
            @PathVariable Long ticketId,
            @RequestBody LeaveReviewRequest request) {
        return ResponseEntity.ok(supportService.leaveReview(ticketId, request));
    }

    // === Выдача ачивки ===
    @PostMapping("/tickets/{ticketId}/grant-achievement")
    public ResponseEntity<String> grantAchievement(
            @PathVariable Long ticketId,
            @RequestParam Long supportId,
            @RequestParam String achievementCode) {
        supportService.grantAchievementFromTicket(ticketId, supportId, achievementCode);
        return ResponseEntity.ok("Ачивка " + achievementCode + " успешно выдана!");
    }
}