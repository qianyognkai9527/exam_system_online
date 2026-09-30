package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.ai.exam.common.CacheConstants;
import com.joker.ai.exam.entity.PaperQuestion;
import com.joker.ai.exam.entity.Question;
import com.joker.ai.exam.entity.QuestionAnswer;
import com.joker.ai.exam.entity.QuestionChoice;
import com.joker.ai.exam.mapper.QuestionAnswerMapper;
import com.joker.ai.exam.mapper.QuestionChoiceMapper;
import com.joker.ai.exam.mapper.QuestionsMapper;
import com.joker.ai.exam.service.PaperQuestionService;
import com.joker.ai.exam.service.QuestionService;
import com.joker.ai.exam.utils.RedisUtils;
import com.joker.ai.exam.vo.QuestionImportVo;
import com.joker.ai.exam.vo.QuestionQueryVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionsMapper, Question> implements QuestionService {

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Autowired
    @Qualifier(value = "threadPoolTaskExecutor")
    private Executor pool;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private PaperQuestionService paperQuestionService;

    @Override
    public void queryPage(IPage<Question> pageResult, QuestionQueryVo questionQueryVo) {
        this.baseMapper.queryQuestionPage(pageResult, questionQueryVo);
    }

    @Override
    public Question getQuestionById(Long id) {
        Question question = this.baseMapper.selectById(id);
        if (Objects.isNull(question)) {
            return question;
        }

        fillAnswerAndChoice(question);

        CompletableFuture.runAsync(() -> {
            this.zsetIncrementByQuestionId(id);
        }, pool);

        return question;
    }

    private void fillAnswerAndChoice(Question question) {
        LambdaQueryWrapper<QuestionAnswer> eq = Wrappers.<QuestionAnswer>lambdaQuery()
                .eq(QuestionAnswer::getQuestionId, question.getId());
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(eq);
        question.setAnswer(questionAnswer);
        if (question.getType().equals("CHOICE")) {
            LambdaQueryWrapper<QuestionChoice> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(QuestionChoice::getQuestionId, question.getId())
                    .orderByAsc(QuestionChoice::getSort);
            question.setChoices(questionChoiceMapper.selectList(lambdaQueryWrapper));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuestion(Question question) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.<Question>lambdaQuery()
                .eq(Question::getType, question.getType())
                .eq(Question::getTitle, question.getTitle());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该题目已存在");
        }
        save(question);
        QuestionAnswer answer = question.getAnswer();
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> choices = question.getChoices();
            StringBuilder choiceContent = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice choice = choices.get(i);
                choice.setSort(i + 1);
                choice.setQuestionId(question.getId());
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()) {
                    if (choiceContent.length() > 0) {
                        choiceContent.append(",");
                    }
                    choiceContent.append((char) ('A' + i));
                }
            }
            answer.setAnswer(choiceContent.toString());
            answer.setQuestionId(question.getId());
            questionAnswerMapper.insert(answer);
        }

    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateQuestion(Question question) {
        LambdaQueryWrapper<Question> ne = Wrappers.<Question>lambdaQuery().eq(Question::getTitle, question.getTitle())
                .ne(Question::getId, question.getId());
        if (this.count(ne) > 0) {
            throw new RuntimeException("该题目已存在");
        }
        updateById(question);
        //删除选项
        LambdaQueryWrapper<QuestionChoice> wrapper = Wrappers.<QuestionChoice>lambdaQuery()
                .eq(QuestionChoice::getQuestionId, question.getId());
        questionChoiceMapper.delete(wrapper);

        QuestionAnswer answer = question.getAnswer();
        questionAnswerMapper.updateById(answer);
        if ("CHOICE".equals(question.getType())) {
            StringBuilder answerContent = new StringBuilder();
            List<QuestionChoice> choices = question.getChoices();
            for (int i = 0; i < choices.size(); i++) {

                QuestionChoice choice = choices.get(i);
                choice.setSort(i + 1);
                choice.setQuestionId(question.getId());
                choice.setId(null);
                choice.setUpdateTime(null);
                choice.setCreateTime(null);
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()) {
                    if (answerContent.length() > 0) {
                        answerContent.append(",");
                    }
                    answerContent.append((char) ('A' + i));
                }
            }
            answer.setAnswer(answerContent.toString());
            answer.setQuestionId(question.getId());
            questionAnswerMapper.updateById(answer);
        }

    }

    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long id) {
        LambdaQueryWrapper<PaperQuestion> eq = Wrappers.<PaperQuestion>lambdaQuery().eq(PaperQuestion::getQuestionId, id);
        if (paperQuestionService.count(eq) > 0) {
            throw new RuntimeException("该题目已关联试卷，请先解除关联");
        }
        removeById(id);
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId, id));
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, id));
    }

    @Override
    public List<Question> queryPopularQuestion(Integer size) {
        List<Question> popularQuestions = new ArrayList<>(size);

        Set<Object> objects = redisUtils.zReverseRange(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size - 1);
        if (!CollectionUtils.isEmpty(objects)) {
            List<Long> idList = objects.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
            idList.stream().forEach(id -> {
                Question question = getById(id);
                if (Objects.nonNull(question)) {
                    popularQuestions.add(question);
                }
            });

        }
        int diff = size - popularQuestions.size();
        if (diff > 0) {
            //从数据查询最新的题目 diff个
            List<Long> existIds = popularQuestions.stream().map(question -> question.getId()).collect(Collectors.toList());
            LambdaQueryWrapper<Question> last = Wrappers.<Question>lambdaQuery().orderByDesc(Question::getCreateTime)
                    .notIn(!CollectionUtils.isEmpty(existIds), Question::getId, existIds)
                    .last(" limit " + diff);
            List<Question> list = this.list(last);
            popularQuestions.addAll(list);
        }

        popularQuestions.stream().forEach(question -> {
            fillAnswerAndChoice(question);
        });


        return popularQuestions;
    }

    @Override
    public String importQuestions(List<QuestionImportVo> questionImportVos) {

        //1.数据集合校验
        if (CollectionUtils.isEmpty(questionImportVos)) {
            return "批量导入结束，本地没有题目导入！传递数据为空！";
        }

        //2.处理数据
        int successCount = 0;
        for (QuestionImportVo questionImportVo : questionImportVos) {
            try {
                Question question = new Question();
                BeanUtils.copyProperties(questionImportVo, question);
                if ("CHOICE".equals(question.getType())) {
                    List<QuestionChoice> collect = questionImportVo.getChoices().stream().map(importChoice -> {
                        QuestionChoice questionChoice = new QuestionChoice();
                        questionChoice.setContent(importChoice.getContent());
                        questionChoice.setIsCorrect(importChoice.getIsCorrect());
                        questionChoice.setSort(importChoice.getSort());
                        return questionChoice;
                    }).collect(Collectors.toList());
                    question.setChoices(collect);

                }
                QuestionAnswer questionAnswer = new QuestionAnswer();
                if ("JUDGE".equals(question.getType())) {
                    questionAnswer.setAnswer(questionImportVo.getAnswer().toUpperCase());
                } else {
                    questionAnswer.setAnswer(questionImportVo.getAnswer());
                }
                questionAnswer.setKeywords(questionImportVo.getKeywords());
                question.setAnswer(questionAnswer);
                saveQuestion(question);
                successCount++;
            }catch (Exception e){
                log.error("导入题目失败：{}", questionImportVo);

            }

        }
        //4.拼接结果
        return "批量导入结束，成功导入%s条题目 数据共%s条！！" .formatted(successCount, questionImportVos.size());
    }

    private void zsetIncrementByQuestionId(Long questionId) {
        Double v = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY, questionId, 1);
        log.info("更新热点题目：{} 后的分数为：{}", questionId, v);
    }
}
