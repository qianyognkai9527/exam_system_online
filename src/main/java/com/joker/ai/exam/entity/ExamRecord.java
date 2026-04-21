package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Data
@TableName("exam_records")
@Schema(name = "ExamRecords对象", description = "")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer examId;

    private String studentName;

    private Integer score;

    private String answers;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Integer windowSwitches;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "更新时间")
    private LocalDateTime updateTime;

    @Schema(name = "0-未删除，1-已删除")
    private Byte isDeleted;

    @TableField(exist = false)
    private List<AnswerRecord> answerRecords; // 答案记录列表

    @TableField(exist = false)
    private Paper paper; // 试卷信息
}
