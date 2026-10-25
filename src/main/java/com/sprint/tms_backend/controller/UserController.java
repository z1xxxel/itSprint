package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CoinTransactionDTO;
import com.sprint.tms_backend.DTO.UserProfileResponseDTO;
import com.sprint.tms_backend.model.User;
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

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<CoinTransactionDTO>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CoinTransactionDTO> result = coinTxRepository
                .findHistoryByUser(id, pageable)
                .map(t -> new CoinTransactionDTO(
                        t.getId(),
                        t.getFromUserId(),
                        t.getToUserId(),
                        t.getAmount(),
                        t.getType(),
                        t.getComment(),
                        t.getTag(), // <--- ПЕРЕДАЕМ ТЕГ
                        t.getCreatedAt()
                ));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }
}