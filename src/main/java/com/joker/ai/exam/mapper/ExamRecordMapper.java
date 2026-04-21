package com.joker.ai.exam.mapper;

import com.joker.ai.exam.entity.ExamRecord;
import com.joker.ai.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.ai.exam.vo.ExamRankingVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    List<ExamRankingVO> customQueryRanking(@Param("paperId") Integer paperId, @Param("limit") Integer limit);
}
