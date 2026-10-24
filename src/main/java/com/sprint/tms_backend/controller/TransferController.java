package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.GrantRequest;
import com.sprint.tms_backend.DTO.TransferDTO;
import com.sprint.tms_backend.model.CoinTransaction;
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
    public ResponseEntity<CoinTransaction> transfer(@RequestBody TransferDTO request) {
        CoinTransaction tx = transferService.transferCoins(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount(),
                request.getComment()
        );
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/grant")
    public ResponseEntity<CoinTransaction> grant(@RequestBody GrantRequest request) {
        CoinTransaction tx = transferService.grantCoins(
                request.actorId(),
                request.toUserId(),
                request.amount(),
                request.comment()
        );
        return ResponseEntity.ok(tx);
    }
}