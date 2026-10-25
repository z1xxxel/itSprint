package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;


import java.util.List;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    // Проверить, проходил ли уже пользователь этот опрос
    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);

    // Получить все ответы для опроса (для аналитики)
    List<SurveyResponse> findBySurveyId(Long surveyId);

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM SurveyResponse r WHERE r.survey.id = :surveyId")
    long countDistinctUsersBySurveyId(@Param("surveyId") Long surveyId);

    void deleteBySurveyId(Long surveyId);

    @EntityGraph(attributePaths = {"user", "question", "option"}) // Загружаем связи, чтобы не было N+1
    List<SurveyResponse> findBySurveyIdOrderByUserIdAscQuestionIdAsc(Long surveyId);

}