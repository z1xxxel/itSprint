package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.AdminDashboardDTO;
import com.sprint.tms_backend.DTO.AdminQuestDTO;
import com.sprint.tms_backend.DTO.CoinTransactionDTO;
import com.sprint.tms_backend.DTO.SupportTicketDTO;
import com.sprint.tms_backend.model.Permission;
import com.sprint.tms_backend.model.Quest;
import com.sprint.tms_backend.model.SystemSetting;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.repository.SystemSettingRepository;
import com.sprint.tms_backend.repository.UserRepository;
import com.sprint.tms_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CoinTransactionRepository coinTxRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;

    @GetMapping("/transactions")
    public ResponseEntity<Page<CoinTransactionDTO>> getAllTransactions(
            @RequestParam Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Только HR или ADMIN могут смотреть глобальную экономику
        if (!actor.getRole().has(Permission.USER_MANAGE) && actor.getRole() != com.sprint.tms_backend.model.UserRole.ADMIN) {
            throw new SecurityException("Нет прав на просмотр глобальной истории");
        }

        Page<CoinTransactionDTO> result = coinTxRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(), t.getCreatedAt()
                ));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getDashboard(actorId));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSetting>> getSettings(@RequestParam Long actorId) {
        return ResponseEntity.ok(adminService.getSettings(actorId));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<SystemSetting> updateSetting(
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam Long actorId) {
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
}