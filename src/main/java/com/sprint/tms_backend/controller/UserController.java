package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.CoinTransaction;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.model.UserRole;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.repository.RatingTransactionRepository;
import com.sprint.tms_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sprint.tms_backend.repository.RatingTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CoinTransactionRepository coinTxRepository;
    private final RatingTransactionRepository ratingTxRepository;

    // Создание пользователя (только для HR/MANAGER/ADMIN)
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestParam(required = false) Long actorId,
            @RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(actorId, user));
    }

    @GetMapping
    public ResponseEntity<List<UserListResponseDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(u -> {
            double lifetime = u.getLifetimeRating() != null ? u.getLifetimeRating() : 0.0;
            String lvl;
            if (lifetime < 10) lvl = "Junior";
            else if (lifetime < 50) lvl = "Middle";
            else if (lifetime < 100) lvl = "Senior";
            else lvl = "Legend";

            return new UserListResponseDTO(
                    u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.getRating(), u.getLifetimeRating(), lvl,
                    u.getBalance(), u.getIsBlocked(),
                    u.getDepartment() != null ? u.getDepartment().getId() : null,
                    u.getDepartment() != null ? u.getDepartment().getName() : null
            );
        }).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // СБРОС ПАРОЛЯ (Только для ADMIN)
    @PutMapping("/{id}/password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody ChangePasswordRequest request) {
        userService.changeUserPassword(id, request.newPassword(), actorId);
        return ResponseEntity.ok("Пароль успешно изменен");
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request, actorId));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<User> changeRole(
            @PathVariable Long id,
            @RequestParam UserRole role,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(userService.changeUserRole(id, role, actorId));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<User> toggleBlock(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(userService.toggleBlockUser(id, actorId));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<CoinTransactionDTO>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(coinTxRepository.findHistoryByUser(id, pageable)
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(),
                        t.getIsCancelled(), t.getCancellationReason(), t.getCreatedAt()
                )));
    }

    // История баланса для графика
    @GetMapping("/{id}/balance-history")
    public ResponseEntity<Page<CoinTransactionDTO>> getBalanceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(coinTxRepository.findHistoryByUser(id, pageable)
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(),
                        t.getIsCancelled(), t.getCancellationReason(), t.getCreatedAt()
                )));
    }

    // История рейтинга для графика
    @GetMapping("/{id}/rating-history")
    public ResponseEntity<Page<RatingTransactionDTO>> getRatingHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ratingTxRepository
                .findAllByUserIdOrderByCreatedAtDesc(id, pageable)
                .map(rt -> new RatingTransactionDTO(
                        rt.getId(), rt.getAmount(), rt.getReason(), rt.getCreatedAt()
                )));
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<User> updateAvatar(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody UpdateAvatarRequest request) {
        return ResponseEntity.ok(userService.updateAvatar(id, request.avatarUrl(), actorId));
    }

    @GetMapping("/{id}/activity-chart")
    public ResponseEntity<List<ActivityPointDTO>> getActivityChart(@PathVariable Long id) {
        userService.getUserById(id); // проверить что существует

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        LocalDateTime since = from.atStartOfDay();

        List<CoinTransaction> txs = coinTxRepository.findAllHistorySince(id, since);

        Map<LocalDate, BigDecimal> earnedByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> spentByDay = new HashMap<>();

        for (CoinTransaction tx : txs) {
            if (Boolean.TRUE.equals(tx.getIsCancelled())) continue;
            LocalDate d = tx.getCreatedAt().toLocalDate();
            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;

            if (id.equals(tx.getToUserId())) {
                earnedByDay.merge(d, amt, BigDecimal::add);
            } else if (id.equals(tx.getFromUserId())) {
                spentByDay.merge(d, amt, BigDecimal::add);
            }
        }

        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        List<ActivityPointDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String label = days[day.getDayOfWeek().getValue() - 1];
            result.add(new ActivityPointDTO(
                    day.toString(),
                    label,
                    earnedByDay.getOrDefault(day, BigDecimal.ZERO),
                    spentByDay.getOrDefault(day, BigDecimal.ZERO)
            ));
        }
        return ResponseEntity.ok(result);
    }
}