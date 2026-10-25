package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final SeasonResultRepository seasonResultRepository;
    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final AchievementService achievementService;
    private final RatingTransactionRepository ratingTxRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentResultRepository departmentResultRepository;
    private final DepartmentMemberResultRepository departmentMemberResultRepository;

    // Награды (общий лидерборд)
    private static final BigDecimal[] REWARDS = {
            new BigDecimal("5000"), new BigDecimal("2500"), new BigDecimal("1000")
    };

    // Награда каждому участнику выигравшего подразделения
    private static final BigDecimal WINNING_DEPT_REWARD = new BigDecimal("50");

    @Transactional
    public SeasonInfoDTO getCurrentSeasonInfo(Long currentUserId) {
        Season currentSeason = getOrCreateCurrentSeason();

        // 1. Индивидуальный лидерборд (ВСЕ пользователи)
        List<User> allUsers = userRepository.findAllByOrderByRatingDesc();
        List<LeaderboardEntryDTO> currentLeaderboard = new ArrayList<>();
        int place = 1;
        for (User u : allUsers) {
            if (u.getRating() != null && u.getRating() > 0) {
                currentLeaderboard.add(new LeaderboardEntryDTO(place++, u.getId(), u.getName(), u.getRating()));
            }
        }

        // 2. Командный лидерборд (места отделов)
        List<DepartmentLeaderboardEntryDTO> deptLeaderboard = buildDepartmentLeaderboard();

        // 3. Внутренний лидерборд подразделения текущего пользователя
        String myDeptName = "Без подразделения";
        Integer myDeptPlace = null;
        List<LeaderboardEntryDTO> myDeptLeaderboard = new ArrayList<>();

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null && currentUser.getDepartment() != null) {
                Department myDept = currentUser.getDepartment();
                myDeptName = myDept.getName();

                // Ищем место отдела
                final Long deptId = myDept.getId();
                myDeptPlace = deptLeaderboard.stream()
                        .filter(d -> d.departmentId().equals(deptId))
                        .map(DepartmentLeaderboardEntryDTO::place)
                        .findFirst().orElse(null);

                // Строим рейтинг сотрудников ВНУТРИ отдела
                List<User> deptUsers = userRepository.findByDepartmentId(deptId);
                deptUsers.sort(Comparator.comparing((User u) -> u.getRating() != null ? u.getRating() : 0.0).reversed());
                int deptPlace = 1;
                for (User u : deptUsers) {
                    if (u.getRating() != null && u.getRating() > 0) {
                        myDeptLeaderboard.add(new LeaderboardEntryDTO(deptPlace++, u.getId(), u.getName(), u.getRating()));
                    }
                }
            }
        }

        // 4. История прошлого сезона
        List<Season> pastSeasons = seasonRepository.findByIsActiveFalseOrderByEndDateDesc();
        List<SeasonResultDTO> previousResults = new ArrayList<>();
        List<DepartmentResultDTO> previousDeptResults = new ArrayList<>();
        if (!pastSeasons.isEmpty()) {
            Season last = pastSeasons.get(0);
            previousResults = getSeasonResults(last.getId());
            previousDeptResults = getDepartmentResults(last.getId());
        }

        return new SeasonInfoDTO(
                currentSeason.getId(),
                currentSeason.getName(),
                currentSeason.getStartDate(),
                currentSeason.getEndDate(),
                currentSeason.getIsActive(),
                currentLeaderboard,
                previousResults,
                myDeptName,
                myDeptPlace,
                myDeptLeaderboard,
                previousDeptResults
        );
    }

    private List<DepartmentLeaderboardEntryDTO> buildDepartmentLeaderboard() {
        List<Department> departments = departmentRepository.findAll();
        List<DepartmentLeaderboardEntryDTO> result = new ArrayList<>();
        record DeptScore(Department dept, double totalRating, int memberCount) {}
        List<DeptScore> scores = new ArrayList<>();

        for (Department d : departments) {
            List<User> members = userRepository.findByDepartmentId(d.getId());
            double total = members.stream().mapToDouble(u -> u.getRating() != null ? u.getRating() : 0.0).sum();
            scores.add(new DeptScore(d, total, members.size()));
        }

        scores.sort(Comparator.comparingDouble(DeptScore::totalRating).reversed());

        int place = 1;
        for (DeptScore s : scores) {
            if (s.memberCount() == 0) continue;
            result.add(new DepartmentLeaderboardEntryDTO(place++, s.dept().getId(), s.dept().getName(), s.totalRating(), s.memberCount()));
        }
        return result;
    }
    @Transactional(readOnly = true)
    public Integer getDepartmentPlace(Long departmentId) {
        List<DepartmentLeaderboardEntryDTO> board = buildDepartmentLeaderboard();
        return board.stream()
                .filter(d -> d.departmentId().equals(departmentId))
                .map(DepartmentLeaderboardEntryDTO::place)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard() {
        List<User> allUsers = userRepository.findAllByOrderByRatingDesc();
        List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
        int place = 1;
        for (User u : allUsers) {
            if (u.getRating() != null && u.getRating() > 0) {
                leaderboard.add(new LeaderboardEntryDTO(place++, u.getId(), u.getName(), u.getRating()));
            }
        }
        return leaderboard;
    }

    @Transactional(readOnly = true)
    public List<Season> getPastSeasons() {
        return seasonRepository.findByIsActiveFalseOrderByEndDateDesc();
    }

    @Transactional(readOnly = true)
    public List<SeasonResultDTO> getSeasonResults(Long seasonId) {
        return seasonResultRepository.findBySeasonIdOrderByPlaceAsc(seasonId).stream()
                .map(r -> new SeasonResultDTO(r.getPlace(), r.getUserName(), r.getFinalRating(), r.getRewardCoins()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResultDTO> getDepartmentResults(Long seasonId) {
        List<DepartmentResult> deptResults = departmentResultRepository.findBySeasonIdOrderByPlaceAsc(seasonId);
        List<DepartmentResultDTO> result = new ArrayList<>();

        for (DepartmentResult dr : deptResults) {
            List<DepartmentMemberResult> members = departmentMemberResultRepository
                    .findBySeasonIdAndDepartmentIdOrderByPlaceInDepartmentAsc(seasonId, dr.getDepartmentId());

            List<DepartmentResultDTO.TopMemberDTO> topMembers = members.stream()
                    .map(m -> new DepartmentResultDTO.TopMemberDTO(
                            m.getPlaceInDepartment(), m.getUserName(), m.getFinalRating(), BigDecimal.ZERO
                    )).toList();

            result.add(new DepartmentResultDTO(dr.getPlace(), dr.getDepartmentName(), dr.getTotalRating(), dr.getMemberCount(), dr.getRewardCoinsPerMember(), topMembers));
        }
        return result;
    }

    @Transactional
    public Season getOrCreateCurrentSeason() {
        return seasonRepository.findByIsActiveTrue().orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            int quarter = (now.getMonthValue() - 1) / 3 + 1;
            int year = now.getYear();
            LocalDateTime start = LocalDateTime.of(year, (quarter - 1) * 3 + 1, 1, 0, 0);
            LocalDateTime end = start.plusMonths(3).minusDays(1).withHour(23).withMinute(59).withSecond(59);

            Season s = new Season();
            s.setName("Q" + quarter + " " + year);
            s.setStartDate(start);
            s.setEndDate(end);
            s.setIsActive(true);
            return seasonRepository.save(s);
        });
    }

    @Transactional
    public void closeExpiredSeasons() {
        List<Season> expired = seasonRepository.findByIsActiveTrueAndEndDateBefore(LocalDateTime.now());
        for (Season season : expired) {
            closeSeason(season);
        }
    }

    private void closeSeason(Season season) {
        log.info("Начало закрытия сезона: {}", season.getName());
        awardIndividualWinners(season);
        awardDepartmentWinners(season);
        resetAllRatings(season);

        season.setIsActive(false);
        season.setClosedAt(LocalDateTime.now());
        seasonRepository.save(season);
        createNextSeason(season);
    }

    // === 1. Общие награды (не изменилось) ===
    private void awardIndividualWinners(Season season) {
        List<User> usersByRating = userRepository.findAllByOrderByRatingDesc();
        int rewardPlaces = Math.min(REWARDS.length, usersByRating.size());

        for (int i = 0; i < rewardPlaces; i++) {
            User user = usersByRating.get(i);
            if (user.getRating() == null || user.getRating() <= 0) break;

            BigDecimal reward = REWARDS[i];
            SeasonResult result = new SeasonResult();
            result.setSeason(season);
            result.setUserId(user.getId());
            result.setUserName(user.getName());
            result.setPlace(i + 1);
            result.setFinalRating(user.getRating());
            result.setRewardCoins(reward);
            seasonResultRepository.save(result);

            addCoinsAndLog(user, reward, "Награда за " + (i + 1) + " место в сезоне " + season.getName());

            try {
                if (i == 0) achievementService.grantAchievementByCode(user.getId(), "SEASON_CHAMPION");
                if (i < 3) achievementService.grantAchievementByCode(user.getId(), "SEASON_TOP_3");
            } catch (Exception e) {
                log.error("Не удалось выдать ачивку пользователю {} за сезон", user.getId(), e);
            }
        }
    }

    // === 2. Командные награды (изменено) ===
    private void awardDepartmentWinners(Season season) {
        List<Department> departments = departmentRepository.findAll();
        record DeptData(Department dept, List<User> members, double totalRating) {}
        List<DeptData> deptDataList = new ArrayList<>();

        for (Department d : departments) {
            List<User> members = userRepository.findByDepartmentId(d.getId());
            if (members.isEmpty()) continue;
            double total = members.stream().mapToDouble(u -> u.getRating() != null ? u.getRating() : 0.0).sum();
            deptDataList.add(new DeptData(d, members, total));
        }

        if (deptDataList.isEmpty()) return;
        deptDataList.sort(Comparator.comparingDouble(DeptData::totalRating).reversed());

        // Победившее подразделение получает по 50 монет каждому участнику
        DeptData winner = deptDataList.get(0);
        if (winner.totalRating() > 0) {
            for (User member : winner.members()) {
                addCoinsAndLog(member, WINNING_DEPT_REWARD, "Бонус за победу подразделения «" + winner.dept().getName() + "» в сезоне " + season.getName());
            }
        }

        // Сохраняем результат каждого отдела + выдаем ачивки топ-3
        int deptPlace = 1;
        for (DeptData dd : deptDataList) {
            DepartmentResult dr = new DepartmentResult();
            dr.setSeason(season);
            dr.setDepartmentId(dd.dept().getId());
            dr.setDepartmentName(dd.dept().getName());
            dr.setPlace(deptPlace);
            dr.setTotalRating(dd.totalRating());
            dr.setMemberCount(dd.members().size());
            dr.setRewardCoinsPerMember(deptPlace == 1 ? WINNING_DEPT_REWARD : BigDecimal.ZERO);
            departmentResultRepository.save(dr);

            List<User> sortedMembers = dd.members().stream()
                    .sorted(Comparator.comparing((User u) -> u.getRating() != null ? u.getRating() : 0.0).reversed())
                    .toList();

            int memberRewardPlaces = Math.min(3, sortedMembers.size());
            for (int i = 0; i < memberRewardPlaces; i++) {
                User member = sortedMembers.get(i);
                if (member.getRating() == null || member.getRating() <= 0) break;

                DepartmentMemberResult dmr = new DepartmentMemberResult();
                dmr.setSeason(season);
                dmr.setDepartmentId(dd.dept().getId());
                dmr.setUserId(member.getId());
                dmr.setUserName(member.getName());
                dmr.setPlaceInDepartment(i + 1);
                dmr.setFinalRating(member.getRating());
                departmentMemberResultRepository.save(dmr);

                // ВЫДАЕМ АЧИВКИ ЗА ТОП ВНУТРИ ОТДЕЛА
                try {
                    String achievementCode = "DEPT_TOP_" + (i + 1);
                    achievementService.grantAchievementByCode(member.getId(), achievementCode);
                } catch (Exception e) {
                    log.error("Ошибка при выдаче ачивки {} пользователю {}", "DEPT_TOP_" + (i + 1), member.getId(), e);
                }
            }
            deptPlace++;
        }
    }

    private void resetAllRatings(Season season) {
        for (User user : userRepository.findAll()) {
            if (user.getRating() != null && user.getRating() > 0) {
                RatingTransaction rt = new RatingTransaction();
                rt.setUserId(user.getId());
                rt.setAmount(-user.getRating());
                rt.setReason("Сброс рейтинга после сезона " + season.getName());
                ratingTxRepository.save(rt);

                user.setRating(0.0);
                userRepository.save(user);
            }
        }
    }

    private void addCoinsAndLog(User user, BigDecimal amount, String comment) {
        BigDecimal current = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(current.add(amount));
        userRepository.save(user);

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(null);
        tx.setToUserId(user.getId());
        tx.setAmount(amount);
        tx.setType(CoinTransactionType.SEASON_REWARD);
        tx.setComment(comment);
        coinTxRepository.save(tx);
    }

    private void createNextSeason(Season closedSeason) {
        LocalDateTime nextStart = closedSeason.getEndDate().plusDays(1);
        int quarter = (nextStart.getMonthValue() - 1) / 3 + 1;
        int year = nextStart.getYear();
        Season next = new Season();
        next.setName("Q" + quarter + " " + year);
        next.setStartDate(nextStart);
        next.setEndDate(nextStart.plusMonths(3).minusDays(1).withHour(23).withMinute(59).withSecond(59));
        next.setIsActive(true);
        seasonRepository.save(next);
    }
}