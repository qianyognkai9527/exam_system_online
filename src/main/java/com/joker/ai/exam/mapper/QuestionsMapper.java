package com.joker.ai.exam.mapper;

import cn.hutool.db.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.joker.ai.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.ai.exam.vo.CategoryCountDto;
import com.joker.ai.exam.vo.QuestionQueryVo;
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
public interface QuestionsMapper extends BaseMapper<Question> {

    List<CategoryCountDto> selectCategoryCount();

    IPage<Question> queryQuestionPage(@Param("page") IPage<Question> pageResult, @Param("questionQueryVo") QuestionQueryVo questionQueryVo);

    /**
     * 根据试卷id查询题目集合
     * @param paperId
     * @return
     */
    List<Question> customQueryQuestionListByPaperId(@Param("paperId") Long paperId);

}
