package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.CreateTemplateRequest;
import com.sprint.tms_backend.DTO.TemplateResponseDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.SurveyTemplateRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurveyTemplateService {

    private final SurveyTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional
    public SurveyTemplate createTemplate(CreateTemplateRequest request) {
        User creator = userRepository.findById(request.creatorId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!creator.getRole().has(Permission.USER_MANAGE) && !creator.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Только админ или менеджер может создавать шаблоны");
        }

        if (templateRepository.existsByName(request.name())) {
            throw new RuntimeException("Шаблон с таким названием уже существует");
        }

        SurveyTemplate template = new SurveyTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        template.setCreator(creator);

        List<SurveyTemplateQuestion> questions = new ArrayList<>();
        for (CreateTemplateRequest.QuestionDto qDto : request.questions()) {
            SurveyTemplateQuestion question = new SurveyTemplateQuestion();
            question.setTemplate(template);
            question.setText(qDto.text());

            List<SurveyTemplateOption> options = new ArrayList<>();
            for (CreateTemplateRequest.OptionDto oDto : qDto.options()) {
                SurveyTemplateOption option = new SurveyTemplateOption();
                option.setQuestion(question);
                option.setText(oDto.text());
                option.setValue(oDto.value());
                options.add(option);
            }
            question.setOptions(options);
            questions.add(question);
        }
        template.setQuestions(questions);

        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponseDTO getTemplateById(Long id) {
        SurveyTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Шаблон не найден"));
        return mapToDto(template);
    }

    @Transactional
    public void deleteTemplate(Long templateId, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.USER_MANAGE) && !actor.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Нет прав на удаление шаблона");
        }

        SurveyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Шаблон не найден"));

        templateRepository.delete(template);
    }

    private TemplateResponseDTO mapToDto(SurveyTemplate t) {
        List<TemplateResponseDTO.QuestionDto> qDtos = t.getQuestions() == null ? List.of() :
                t.getQuestions().stream().map(q -> new TemplateResponseDTO.QuestionDto(
                        q.getId(),
                        q.getText(),
                        q.getOptions() == null ? List.of() :
                                q.getOptions().stream().map(o -> new TemplateResponseDTO.OptionDto(
                                        o.getId(), o.getText(), o.getValue()
                                )).toList()
                )).toList();

        return new TemplateResponseDTO(
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getCreator().getName(),
                t.getCreatedAt(),
                qDtos
        );
    }
}