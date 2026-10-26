package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.model.Permission;
import com.sprint.tms_backend.model.Treasury;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.UserRepository;
import com.sprint.tms_backend.service.TreasuryService;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/fund")
    public ResponseEntity<Treasury> fundTreasury(
            @RequestParam Long actorId,
            @RequestParam BigDecimal amount) {

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (actor.getRole() != com.sprint.tms_backend.model.UserRole.ADMIN && actor.getRole() != com.sprint.tms_backend.model.UserRole.HR) {
            throw new SecurityException("Только HR и ADMIN могут пополнять казну");
        }

        return ResponseEntity.ok(treasuryService.fundTreasury(amount));
    }
}