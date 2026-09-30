package com.joker.ai.exam.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.ai.exam.entity.AnswerRecord;
import com.joker.ai.exam.entity.ExamRecord;
import com.joker.ai.exam.entity.Paper;
import com.joker.ai.exam.entity.Question;
import com.joker.ai.exam.mapper.ExamRecordMapper;
import com.joker.ai.exam.service.AnswerRecordService;
import com.joker.ai.exam.service.ExamRecordService;
import com.joker.ai.exam.service.KimiService;
import com.joker.ai.exam.service.PaperService;
import com.joker.ai.exam.vo.ExamRankingVO;
import com.joker.ai.exam.vo.GradingResult;
import com.joker.ai.exam.vo.StartExamVo;
import com.joker.ai.exam.vo.SubmitAnswerVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {


    @Resource
    private PaperService paperService;

    @Resource
    private AnswerRecordService answerRecordService;
    @Autowired
    private KimiService kimiService;

    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        String studentName = startExamVo.getStudentName();

        LambdaQueryWrapper<ExamRecord> queryWrapper = Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getStudentName, studentName)
                .eq(ExamRecord::getStatus, "进行中")
                .eq(ExamRecord::getExamId, startExamVo.getPaperId())
                .last(" limit 1");
        ExamRecord examRecord = this.getOne(queryWrapper);
        if (Objects.nonNull(examRecord)) {
            log.warn("当前用户存在进行中的考试");
            return examRecord;
        }
        ExamRecord record = new ExamRecord();
        record.setExamId(startExamVo.getPaperId());
        record.setStudentName(studentName);
        record.setStatus("进行中");
        record.setStartTime(LocalDateTime.now());
        record.setWindowSwitches(0);
        save(record);
        return record;
    }

    @Override
    public ExamRecord getExamRecordDetail(Integer id) {
        ExamRecord examRecord = getById(id);
        if (Objects.isNull(examRecord)) {
            throw new RuntimeException("考试记录不存在");
        }
        Paper paperDetail = paperService.getPaperDetail(examRecord.getExamId().longValue());
        examRecord.setPaper(paperDetail);
        LambdaQueryWrapper<AnswerRecord> eq = Wrappers.<AnswerRecord>lambdaQuery()
                .eq(AnswerRecord::getExamRecordId, id);
        List<AnswerRecord> answerRecordList = answerRecordService.list(eq);
        if (CollectionUtils.isEmpty(answerRecordList)) {
            return examRecord;
        }
        List<Long> questionIds = paperDetail.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
        answerRecordList.sort((a1, a2) -> {
            int a1Index = questionIds.indexOf(a1.getQuestionId());
            int a2Index = questionIds.indexOf(a2.getQuestionId());
            return Integer.compare(a1Index, a2Index);
        });
        examRecord.setAnswerRecords(answerRecordList);

        return examRecord;
    }

    @Override
    public void submitAnswers(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {

        //宏观： 提交答案中间表保存  修改考试记录数据（已完成 ，结束时间）  触发开始判卷（examRecordId）
        //1.中间表保存问题
        if (CollectionUtils.isNotEmpty(answers)) {
            List<AnswerRecord> answerRecordList = answers.stream().map(vo -> {
                //examRecordId, vo.getQuestionId(), vo.getUserAnswer()
                AnswerRecord answerRecord = new AnswerRecord();
                answerRecord.setExamRecordId(examRecordId);
                answerRecord.setQuestionId(vo.getQuestionId());
                answerRecord.setUserAnswer(vo.getUserAnswer());
                return answerRecord;
            }).collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        //2. 暂时修改下考试记录状态（状态 -》 已完成 || 结束时间 - 设置）
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        updateById(examRecord);

        //3.调用判卷的接口
        gradeExam(examRecordId);
    }

    @Override
    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
        log.info("开始判卷");
        //1.获取考试记录和相关的信息（试卷和答题记录）
        ExamRecord examRecord = getExamRecordDetail(examRecordId);
        Paper paper = examRecord.getPaper();

        if (paper == null) {
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
            updateById(examRecord);
            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (CollectionUtils.isEmpty(answerRecords)) {
            //没有提交
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }
        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3. 修改每个答题记录的状态（得分，是否正确 0 1 2 ，text-》ai评语））
        int correctNumber = 0; //正确题目数量
        int totalScore = 0; //总得分
        //将正确题目转成map,方便每次判断获取正确答案
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));
        for (AnswerRecord answerRecord : answerRecords) {

            Question question = questionMap.get(answerRecord.getQuestionId().longValue());
            String systemAnswer = question.getAnswer().getAnswer();
            String userAnswer = answerRecord.getUserAnswer();
            if ("JUDGE".equalsIgnoreCase(question.getType())) {
                userAnswer = judgeToBooleanStr(userAnswer);
            }
            try {
                if (!"TEXT".equals(question.getType())) {
                    //非简答题
                    if (systemAnswer.equalsIgnoreCase(userAnswer)) {
                        //回答正确
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                        totalScore += question.getScore();
                        correctNumber++;
                    } else if (systemAnswer.contains(userAnswer)) {
                        //部分正确
                        answerRecord.setIsCorrect(2);
                        answerRecord.setScore(question.getPaperScore().intValue() / 2);
                        totalScore += question.getScore() / 2;
                        correctNumber++;
                    } else {
                        //回答错误
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                    }
                } else {
                    //简答题
                    String gradlePrompt = kimiService.buildGradingPrompt(question, userAnswer, question.getScore());
                    String result = kimiService.callKimi(gradlePrompt);
                    GradingResult gradingResult = JSONObject.parseObject(result, GradingResult.class);
                    Integer aiScore = gradingResult.getScore();
                    if (aiScore >= question.getScore()) {
                        //正确
                        answerRecord.setScore(question.getScore());
                        answerRecord.setIsCorrect(1);
                        totalScore += question.getScore();
                        answerRecord.setAiCorrection(gradingResult.getFeedback());
                        correctNumber++;
                    } else if (aiScore == 0) {
                        //错误
                        answerRecord.setScore(0);
                        answerRecord.setIsCorrect(0);
                        answerRecord.setAiCorrection(gradingResult.getReason());
                    } else {
                        //部分正确
                        answerRecord.setScore(aiScore);
                        answerRecord.setIsCorrect(2);
                        answerRecord.setAiCorrection(gradingResult.getReason());
                        totalScore += aiScore;
                        correctNumber++;
                    }


                }

            } catch (Exception e) {
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
                answerRecord.setAiCorrection("判卷过程中出错，直接零分");
            }

        }
        answerRecordService.updateBatchById(answerRecords);
        String string = kimiService.buildSummaryPrompt(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);
        String summary = kimiService.callKimi(string);
        examRecord.setStatus("已批阅");
        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        updateById(examRecord);
        return examRecord;
    }

    @Override
    public void queryExamRecordPage(IPage<ExamRecord> pageResult, String studentName, String studentNumber, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(studentName), ExamRecord::getStudentName, studentName);
        if (Objects.nonNull(status)) {
            String statusStr = switch (status) {
                case 0 -> "进行中";
                case 1 -> "已完成";
                case 2 -> "已批阅";
                default -> "未知";
            };
            queryWrapper.eq(ExamRecord::getStatus, statusStr);
        }
        queryWrapper.ge(StringUtils.isNotBlank(startDate), ExamRecord::getStartTime, startDate);
        queryWrapper.le(StringUtils.isNotBlank(endDate), ExamRecord::getEndTime, endDate);
        page(pageResult, queryWrapper);
        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return;
        }
        List<Integer> collect = pageResult.getRecords().stream().map(ExamRecord::getExamId).collect(Collectors.toList());
        List<Paper> papers = paperService.listByIds(collect);
        Map<Integer, Paper> paperMap = papers.stream().collect(Collectors.toMap(Paper::getId, v -> v));
        pageResult.getRecords().forEach(examRecord -> {
            Paper paper = paperMap.get(examRecord.getExamId());
            examRecord.setPaper(paper);

        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeExamRecord(Integer id) {
        ExamRecord examRecord = getById(id);
        if(Objects.isNull(examRecord)){
            throw new RuntimeException("数据不存在");

        }
        if("进行中".equals(examRecord.getStatus())){
            throw new RuntimeException("进行中的考试不能删除");
        }

        removeById(id);
        answerRecordService.remove(Wrappers.<AnswerRecord>lambdaQuery().eq(AnswerRecord::getExamRecordId, id));

    }

    @Override
    public List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit) {
        return this.baseMapper.customQueryRanking(paperId,limit);


    }


    private String judgeToBooleanStr(String userAnswer) {
        userAnswer = userAnswer.toUpperCase();
        switch (userAnswer) {
            case "T":
            case "TRUE":
            case "对":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
            case "错误":
                return "FALSE";
            default:
                return userAnswer;
        }

    }
}
