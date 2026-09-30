package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.joker.ai.exam.entity.ExamRecord;
import com.joker.ai.exam.entity.Paper;
import com.joker.ai.exam.entity.PaperQuestion;
import com.joker.ai.exam.entity.Question;
import com.joker.ai.exam.mapper.PaperMapper;
import com.joker.ai.exam.mapper.QuestionsMapper;
import com.joker.ai.exam.service.ExamRecordService;
import com.joker.ai.exam.service.PaperQuestionService;
import com.joker.ai.exam.service.PaperService;
import com.joker.ai.exam.service.QuestionService;
import com.joker.ai.exam.vo.AiPaperVo;
import com.joker.ai.exam.vo.PaperVo;
import com.joker.ai.exam.vo.RuleVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {


    @Autowired
    private PaperQuestionService paperQuestionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ExamRecordService examRecordService;
    @Autowired
    private QuestionsMapper questionsMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Paper createPaper(PaperVo paperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperVo, paper);
        paper.setStatus("DRAFT");
        if (CollectionUtils.isEmpty(paperVo.getQuestions())) {
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            save(paper);
            return paper;
        }
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();
        paper.setQuestionCount(questions.size());
        paper.setTotalScore(questions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        this.save(paper);

        List<PaperQuestion> collect = questions.entrySet().stream().map(entrySet -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paper.getId());
            paperQuestion.setQuestionId(entrySet.getKey().longValue());
            paperQuestion.setScore(entrySet.getValue());
            return paperQuestion;
        }).collect(Collectors.toList());

        paperQuestionService.saveBatch(collect);

        return paper;
    }

    @Override
    @Transactional
    public Paper createPaperWithAI(AiPaperVo aiPaperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo, paper);
        paper.setStatus("DRAFT");
        this.save(paper);

        List<RuleVo> rules = aiPaperVo.getRules();
        AtomicInteger questionCount = new AtomicInteger();
        AtomicReference<BigDecimal> totalScore = new AtomicReference<>(BigDecimal.ZERO);
        List<PaperQuestion> allPaperQuestions = Lists.newArrayList();

        rules.stream()
                .filter(ruleVo -> ruleVo.getCount() > 0)
                .collect(Collectors.toList()).forEach(ruleVo -> {
                    LambdaQueryWrapper<Question> in = Wrappers.<Question>lambdaQuery()
                            .eq(Question::getType, ruleVo.getType())
                            .in(org.apache.commons.collections4.CollectionUtils.isNotEmpty(ruleVo.getCategoryIds()),
                                    Question::getCategoryId, ruleVo.getCategoryIds());
                    List<Question> questionAllList = questionService.list(in);
                    if (CollectionUtils.isEmpty(questionAllList)) {
                        log.warn("没有找到题目");
                        return;
                    }
                    int realNumber = Math.min(ruleVo.getCount(), questionAllList.size());
                    questionCount.addAndGet(realNumber);
                    totalScore.set(totalScore.get()
                            .add(new BigDecimal(ruleVo.getScore()).multiply(new BigDecimal(realNumber))));
                    Collections.shuffle(questionAllList);
                    List<Question> questions = questionAllList.subList(0, realNumber);
                    questions.stream().forEach(question -> {
                        PaperQuestion paperQuestion = new PaperQuestion();
                        paperQuestion.setPaperId(paper.getId());
                        paperQuestion.setQuestionId(question.getId());
                        paperQuestion.setScore(new BigDecimal(ruleVo.getScore()));
                        allPaperQuestions.add(paperQuestion);
                    });

                });
        paper.setQuestionCount(questionCount.get());
        paper.setTotalScore(totalScore.get());
        this.updateById(paper);
        paperQuestionService.saveBatch(allPaperQuestions);


        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(Integer id, PaperVo paperVo) {
        Paper paper = getById(id);
        if (Objects.isNull(paper)) {
            log.warn("没有找到试卷");
            throw new RuntimeException("没有找到试卷");
        }
        if ("PUBLISHED".equals(paper.getStatus())) {
            throw new RuntimeException("试卷已发布，不能修改");
        }
        LambdaQueryWrapper<Paper> last = Wrappers.<Paper>lambdaQuery()
                .ne(Paper::getId, id)
                .eq(Paper::getName, paperVo.getName())
                .last(" limit 1");
        Paper one = this.getOne(last);
        if (one != null) {
            throw new RuntimeException("该试卷名称已存在");
        }
        paper = new Paper();
        paper.setId(id);
        BeanUtils.copyProperties(paperVo, paper);
        paper.setQuestionCount(paperVo.getQuestions().size());
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        this.updateById(paper);

        paperQuestionService.remove(Wrappers.<PaperQuestion>lambdaQuery().eq(PaperQuestion::getPaperId, id));
        List<PaperQuestion> collect = paperVo.getQuestions().entrySet().stream().map(entrySet -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(id);
            paperQuestion.setQuestionId(entrySet.getKey().longValue());
            paperQuestion.setScore(entrySet.getValue());
            return paperQuestion;
        }).collect(Collectors.toList());
        paperQuestionService.saveBatch(collect);


    }

    @Override
    public void updatePaperStatus(Integer id, String status) {
        LambdaUpdateWrapper<Paper> set = Wrappers.<Paper>lambdaUpdate()
                .eq(Paper::getId, id)
                .set(Paper::getStatus, status);
        this.update(set);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePaper(Integer id) {
        Paper paper = getById(id);
        if (Objects.isNull(paper)) {
            log.warn("没有找到试卷");
            throw new RuntimeException("没有找到试卷");
        }
        if ("PUBLISHED".equals(paper.getStatus())) {
            throw new RuntimeException("试卷已发布，不能删除");
        }
        LambdaQueryWrapper<ExamRecord> eq = Wrappers.<ExamRecord>lambdaQuery().eq(ExamRecord::getExamId, id);
        if (examRecordService.count(eq) > 0) {
            throw new RuntimeException("该试卷有考试记录，不能删除");
        }
        removeById(id);
        paperQuestionService.remove(Wrappers.<PaperQuestion>lambdaQuery().eq(PaperQuestion::getPaperId, id));
    }

    @Override
    public Paper getPaperDetail(Long id) {
        Paper paper = getById(id);
        if (Objects.isNull(paper)) {
            log.warn("没有找到试卷");
            throw new RuntimeException("没有找到试卷");
        }
        List<Question> questions = questionsMapper.customQueryQuestionListByPaperId(id);
        questions.sort((o1, o2)->Integer.compare(typeToInt(o1.getType()), typeToInt(o2.getType())));
        paper.setQuestions(questions);
        return paper;
    }

    /**
     * 获取题目类型的排序顺序
     * @param type 题目类型
     * @return 排序序号
     */
    private int typeToInt(String type) {
        switch (type) {
            case "CHOICE": return 1; // 选择题
            case "JUDGE": return 2;  // 判断题
            case "TEXT": return 3;   // 简答题
            default: return 4;       // 其他类型
        }
    }
}
