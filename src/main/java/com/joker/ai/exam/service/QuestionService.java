package com.joker.ai.exam.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.joker.ai.exam.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.ai.exam.vo.CategoryCountDto;
import com.joker.ai.exam.vo.QuestionImportVo;
import com.joker.ai.exam.vo.QuestionQueryVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface QuestionService extends IService<Question> {


    void queryPage(IPage<Question> pageResult, QuestionQueryVo questionPageVo);

    Question getQuestionById(Long id);

    void saveQuestion(Question question);

    void updateQuestion(Question question);

    void deleteQuestion(Long id);

    List<Question> queryPopularQuestion(Integer size);

    String importQuestions(List<QuestionImportVo> questions);

}
