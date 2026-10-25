package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.util.List;

public record DepartmentResultDTO(
        Integer place,
        String departmentName,
        Double totalRating,
        Integer memberCount,
        BigDecimal rewardCoinsPerMember,
        List<TopMemberDTO> topMembers
) {
    public record TopMemberDTO(
            Integer placeInDepartment,
            String userName,
            Double finalRating,
            BigDecimal rewardCoins
    ) {}
}