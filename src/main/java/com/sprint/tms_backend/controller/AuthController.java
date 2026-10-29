package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.LoginRequest;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.email(), request.password());

        return ResponseEntity.ok(Map.of(
                "message", "Успешный вход",
                "userId", user.getId(),
                "name", user.getName(),
                "role", user.getRole(),
                "balance", user.getBalance(),
                "rating", user.getRating()
        ));
    }
}