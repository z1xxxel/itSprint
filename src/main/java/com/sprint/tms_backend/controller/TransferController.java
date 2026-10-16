package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.TransferDTO;
import com.sprint.tms_backend.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<String> transfer(@RequestBody TransferDTO request) {
        transferService.transferCoins(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount()
        );
        return ResponseEntity.ok("Перевод успешно выполнен");
    }
}