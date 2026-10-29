package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.TreasuryLogDTO;
import com.sprint.tms_backend.model.Treasury;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.model.UserRole;
import com.sprint.tms_backend.repository.UserRepository;
import com.sprint.tms_backend.service.TreasuryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/treasury")
@RequiredArgsConstructor
public class TreasuryController {

    private final TreasuryService treasuryService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Treasury> getTreasury() {
        return ResponseEntity.ok(treasuryService.getTreasury());
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<TreasuryLogDTO>> getLogs(
            @RequestParam Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.HR) {
            throw new SecurityException("Только HR и ADMIN могут просматривать логи казны");
        }

        return ResponseEntity.ok(treasuryService.getLogs(page, size));
    }

    // === Пополнение (HR + ADMIN) ===
    @PostMapping("/fund")
    public ResponseEntity<Treasury> fundTreasury(
            @RequestParam Long actorId,
            @RequestParam BigDecimal amount) {

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.HR) {
            throw new SecurityException("Только HR и ADMIN могут пополнять казну");
        }

        return ResponseEntity.ok(treasuryService.fundTreasury(amount));
    }

    // === Ручное списание (ТОЛЬКО ADMIN) ===
    @PostMapping("/withdraw")
    public ResponseEntity<Treasury> withdrawTreasury(
            @RequestParam Long actorId,
            @RequestParam BigDecimal amount) {

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Только администратор может списывать из казны");
        }

        return ResponseEntity.ok(treasuryService.manualWithdraw(amount, actor.getId(), actor.getName()));
    }
}