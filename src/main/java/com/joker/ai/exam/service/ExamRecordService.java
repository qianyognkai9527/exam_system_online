package com.joker.ai.exam.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.ai.exam.entity.ExamRecord;
import com.joker.ai.exam.vo.ExamRankingVO;
import com.joker.ai.exam.vo.StartExamVo;
import com.joker.ai.exam.vo.SubmitAnswerVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface ExamRecordService extends IService<ExamRecord> {

    ExamRecord startExam(StartExamVo startExamVo);

    ExamRecord getExamRecordDetail(Integer id);

    void submitAnswers(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;
    ExamRecord gradeExam(Integer examRecordId) throws InterruptedException;

    void queryExamRecordPage(IPage<ExamRecord> pageResult, String studentName, String studentNumber, Integer status, String startDate, String endDate);

    void removeExamRecord(Integer id);

    List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit);
}
