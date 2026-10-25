package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.ThanksTag;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferDTO {
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String comment;
    private ThanksTag tag; // <--- ENUM
}