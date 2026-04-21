package com.joker.ai.exam.service;

import com.joker.ai.exam.entity.Question;
import com.joker.ai.exam.vo.AiGenerateRequestVo;
import com.joker.ai.exam.vo.GradingResult;
import com.joker.ai.exam.vo.QuestionImportVo;

import java.util.List;

public interface KimiService {

    public String buildPrompt(AiGenerateRequestVo request);

    List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException;

    String callKimi(String prompt) throws InterruptedException;

    String buildGradingPrompt(Question question, String userAnswer, Integer maxScore);

    String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount);
    /**
     * 使用ai,进行简答题判断
     * @param question
     * @param userAnswer
     * @param maxScore
     * @return
     */
    GradingResult gradingTextQuestion(Question question, String userAnswer, Integer maxScore) throws InterruptedException;
}
