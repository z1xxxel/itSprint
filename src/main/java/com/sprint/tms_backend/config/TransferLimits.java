package com.sprint.tms_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "tms.limits")
@Data
public class TransferLimits {

    private Peer peer = new Peer();
    private Manager manager = new Manager();

    @Data
    public static class Peer {
        private BigDecimal dailyPerRecipient = new BigDecimal("50");
        private BigDecimal dailyTotal = new BigDecimal("200");
    }

    @Data
    public static class Manager {
        private BigDecimal dailyPerSubordinate = new BigDecimal("500");
        private BigDecimal dailyTotal = new BigDecimal("2000");
    }
}