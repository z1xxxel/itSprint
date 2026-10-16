package com.sprint.tms_backend.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferDTO {
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;

}