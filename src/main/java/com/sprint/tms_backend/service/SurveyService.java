package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final SurveyTemplateRepository templateRepository;
    private final RatingTransactionRepository ratingTxRepository;

    // ==========================================
    // === СОЗДАНИЕ ОПРОСА ===
    // ==========================================
    @Transactional
    public Survey createSurvey(CreateSurveyRequest request) {
        User creator = userRepository.findById(request.creatorId())
                .orElseThrow(() -> new RuntimeException("Создатель не найден"));

        if (!creator.getRole().has(Permission.USER_MANAGE) && !creator.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Только менеджер или админ может создавать опросы");
        }

        if (request.deadline() == null || request.deadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Дедлайн должен быть в будущем");
        }

        Survey survey = new Survey();
        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setCreator(creator);
        survey.setDeadline(request.deadline());
        survey.setRewardRating(request.rewardRating() != null ? request.rewardRating() : 0.0);
        survey.setPenaltyRating(request.penaltyRating() != null ? request.penaltyRating() : 0.0);
        survey.setIsActive(true);

        List<SurveyQuestion> questions = new ArrayList<>();
        for (CreateSurveyRequest.QuestionDto qDto : request.questions()) {
            SurveyQuestion question = new SurveyQuestion();
            question.setSurvey(survey);
            question.setText(qDto.text());

            List<SurveyOption> options = new ArrayList<>();
            for (CreateSurveyRequest.OptionDto oDto : qDto.options()) {
                SurveyOption option = new SurveyOption();
                option.setQuestion(question);
                option.setText(oDto.text());
                option.setValue(oDto.value());
                options.add(option);
            }
            question.setOptions(options);
            questions.add(question);
        }
        survey.setQuestions(questions);

        Survey saved = surveyRepository.save(survey);
        // Форсим инициализацию lazy-коллекций, чтобы Jackson не упал при сериализации
        initSurveyCollections(List.of(saved));
        return saved;
    }

    // ==========================================
    // === ПРОХОЖДЕНИЕ ОПРОСА ===
    // ==========================================
    @Transactional
    public void submitSurvey(Long surveyId, SubmitSurveyRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Опрос не найден"));

        if (!survey.getIsActive() || LocalDateTime.now().isAfter(survey.getDeadline())) {
            throw new RuntimeException("Опрос уже закрыт (дедлайн истек)");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (responseRepository.existsBySurveyIdAndUserId(surveyId, request.userId())) {
            throw new RuntimeException("Вы уже проходили этот опрос");
        }

        for (Map.Entry<Long, Long> entry : request.answers().entrySet()) {
            SurveyResponse response = new SurveyResponse();
            response.setSurvey(survey);
            response.setUser(user);

            SurveyQuestion question = new SurveyQuestion();
            question.setId(entry.getKey());
            response.setQuestion(question);

            SurveyOption option = new SurveyOption();
            option.setId(entry.getValue());
            response.setOption(option);

            responseRepository.save(response);
        }

        if (survey.getRewardRating() != null && survey.getRewardRating() > 0) {
            double currentRating = user.getRating() != null ? user.getRating() : 0.0;
            user.setRating(currentRating + survey.getRewardRating());
            userRepository.save(user);

            RatingTransaction rt = new RatingTransaction();
            rt.setUserId(user.getId());
            rt.setAmount(survey.getRewardRating());
            rt.setReason("Прохождение опроса: " + survey.getTitle());
            ratingTxRepository.save(rt);
        }
    }

    // ==========================================
    // === АВТОМАТИЧЕСКОЕ ЗАКРЫТИЕ И ШТРАФЫ ===
    // ==========================================
    @Transactional
    public void applyPenaltiesForExpiredSurveys() {
        List<Survey> expiredSurveys = surveyRepository.findByIsActiveTrueAndDeadlineBefore(LocalDateTime.now());

        for (Survey survey : expiredSurveys) {
            List<Long> answeredUserIds = responseRepository.findBySurveyId(survey.getId()).stream()
                    .map(r -> r.getUser().getId())
                    .distinct()
                    .toList();

            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                if (user.getRole() == UserRole.EMPLOYEE && !answeredUserIds.contains(user.getId())) {
                    double currentRating = user.getRating() != null ? user.getRating() : 0.0;
                    double newRating = currentRating + (survey.getPenaltyRating() != null ? survey.getPenaltyRating() : 0.0);

                    if (newRating < 0) newRating = 0.0;

                    user.setRating(newRating);
                    userRepository.save(user);

                    RatingTransaction rt = new RatingTransaction();
                    rt.setUserId(user.getId());
                    rt.setAmount(survey.getPenaltyRating());
                    rt.setReason("Штраф за непрохождение опроса: " + survey.getTitle());
                    ratingTxRepository.save(rt);
                }
            }

            survey.setIsActive(false);
            surveyRepository.save(survey);
        }
    }

    // ==========================================
    // === ПРОСМОТР ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<Survey> getAllSurveys() {
        List<Survey> surveys = surveyRepository.findAll();
        initSurveyCollections(surveys);
        return surveys;
    }

    @Transactional(readOnly = true)
    public List<Survey> getSurveysByStatus(Boolean isActive) {
        List<Survey> surveys = surveyRepository.findByIsActive(isActive);
        initSurveyCollections(surveys);
        return surveys;
    }

    @Transactional(readOnly = true)
    public List<SurveyResponse> getSurveyResults(Long surveyId) {
        return responseRepository.findBySurveyId(surveyId);
    }

    // ==========================================
    // === УДАЛЕНИЕ ===
    // ==========================================
    @Transactional
    public void deleteSurvey(Long surveyId, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.USER_MANAGE) && !actor.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Только админ или менеджер может удалять опросы");
        }

        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Опрос не найден"));

        responseRepository.deleteBySurveyId(surveyId);
        surveyRepository.delete(survey);
    }

    // ==========================================
    // === СВОДНАЯ СТАТИСТИКА ===
    // ==========================================
    @Transactional(readOnly = true)
    public SurveyStatsResponseDTO getSurveyStats(Long surveyId, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        Survey survey = surveyRepository.findById(surveyId).orElseThrow();
        List<SurveyResponse> allResponses = responseRepository.findBySurveyId(surveyId);
        long totalUsers = userRepository.count();
        long respondedUsers = responseRepository.countDistinctUsersBySurveyId(surveyId);
        double responseRate = totalUsers > 0 ? (double) respondedUsers / totalUsers * 100 : 0.0;

        List<SurveyStatsResponseDTO.QuestionStatsDTO> questionStats = new ArrayList<>();
        for (SurveyQuestion question : survey.getQuestions()) {
            List<SurveyResponse> questionAnswers = allResponses.stream()
                    .filter(r -> r.getQuestion().getId().equals(question.getId())).toList();
            long totalAnswers = questionAnswers.size();
            double averageValue = questionAnswers.stream().mapToInt(r -> r.getOption().getValue()).average().orElse(0.0);

            List<SurveyStatsResponseDTO.OptionStatsDTO> optionStats = new ArrayList<>();
            for (SurveyOption option : question.getOptions()) {
                long count = questionAnswers.stream().filter(r -> r.getOption().getId().equals(option.getId())).count();
                double percentage = totalAnswers > 0 ? (double) count / totalAnswers * 100 : 0.0;
                optionStats.add(new SurveyStatsResponseDTO.OptionStatsDTO(option.getId(), option.getText(), option.getValue(), count, Math.round(percentage * 10.0) / 10.0));
            }
            questionStats.add(new SurveyStatsResponseDTO.QuestionStatsDTO(question.getId(), question.getText(), totalAnswers, Math.round(averageValue * 10.0) / 10.0, optionStats));
        }
        return new SurveyStatsResponseDTO(survey.getId(), survey.getTitle(), survey.getDeadline(), survey.getIsActive(), totalUsers, respondedUsers, Math.round(responseRate * 10.0) / 10.0, questionStats);
    }

    // ==========================================
    // === ДЕТАЛЬНЫЕ ОТВЕТЫ ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<SurveyUserResponseDTO> getDetailedResponses(Long surveyId, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        List<SurveyResponse> responses = responseRepository.findBySurveyIdOrderByUserIdAscQuestionIdAsc(surveyId);
        Map<Long, List<SurveyResponse>> groupedByUser = responses.stream().collect(Collectors.groupingBy(r -> r.getUser().getId()));
        List<SurveyUserResponseDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<SurveyResponse>> entry : groupedByUser.entrySet()) {
            List<SurveyResponse> userResponses = entry.getValue();
            User user = userResponses.get(0).getUser();
            LocalDateTime answeredAt = userResponses.get(0).getAnsweredAt();
            List<SurveyUserResponseDTO.AnswerDetailDTO> answers = userResponses.stream()
                    .map(r -> new SurveyUserResponseDTO.AnswerDetailDTO(r.getQuestion().getText(), r.getOption().getText(), r.getOption().getValue())).toList();
            result.add(new SurveyUserResponseDTO(user.getId(), user.getName(), user.getEmail(), answeredAt, answers));
        }
        return result;
    }

    // ==========================================
    // === СОХРАНЕНИЕ КАК ШАБЛОН ===
    // ==========================================
    @Transactional
    public SurveyTemplate saveAsTemplate(Long surveyId, Long actorId, String templateName) {
        User actor = userRepository.findById(actorId).orElseThrow();
        Survey survey = surveyRepository.findById(surveyId).orElseThrow();
        SurveyTemplate template = new SurveyTemplate();
        template.setName(templateName);
        template.setDescription("Создан на основе опроса: " + survey.getTitle());
        template.setCreator(actor);
        List<SurveyTemplateQuestion> templateQuestions = new ArrayList<>();
        for (SurveyQuestion q : survey.getQuestions()) {
            SurveyTemplateQuestion tq = new SurveyTemplateQuestion();
            tq.setTemplate(template);
            tq.setText(q.getText());
            List<SurveyTemplateOption> tOpts = new ArrayList<>();
            for (SurveyOption o : q.getOptions()) {
                SurveyTemplateOption to = new SurveyTemplateOption();
                to.setQuestion(tq);
                to.setText(o.getText());
                to.setValue(o.getValue());
                tOpts.add(to);
            }
            tq.setOptions(tOpts);
            templateQuestions.add(tq);
        }
        template.setQuestions(templateQuestions);
        return templateRepository.save(template);
    }

    // ==========================================
    // === HELPER: force-init lazy-коллекций ===
    // ==========================================
    /**
     * Принудительно инициализирует lazy-коллекции Survey внутри транзакции.
     * Нужно, потому что spring.jpa.open-in-view=false — иначе Jackson при
     * сериализации в контроллере упадёт на LazyInitializationException.
     */
    private void initSurveyCollections(List<Survey> surveys) {
        for (Survey s : surveys) {
            if (s.getCreator() != null) {
                s.getCreator().getName();
            }
            if (s.getQuestions() != null) {
                for (SurveyQuestion q : s.getQuestions()) {
                    if (q.getOptions() != null) {
                        q.getOptions().size();
                    }
                }
            }
        }
    }
}