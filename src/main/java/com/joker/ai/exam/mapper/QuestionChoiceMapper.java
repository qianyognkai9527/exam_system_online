package com.joker.ai.exam.mapper;

import com.joker.ai.exam.entity.QuestionChoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {
    //第二步：根据题目id查询对应的选项集合
    @Select("select * from question_choices where is_deleted = 0 and question_id = #{questionId} order by sort asc ;")
    List<QuestionChoice> selectListByQuestionId(Long questionId);

}
