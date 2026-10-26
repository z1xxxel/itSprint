package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.model.UserRole;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CoinTransactionRepository coinTxRepository;

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
                    u.getBalance(), u.getIsBlocked()
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
}