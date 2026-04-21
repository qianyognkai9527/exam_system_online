package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
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
@TableName("questions")
@Schema(name = "Questions对象", description = "")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String type;

    private Boolean multi;

    private Long categoryId;

    private String difficulty;

    private Integer score;

    private String analysis;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "更新时间")
    private LocalDateTime updateTime;

    @Schema(name = "0-未删除，1-已删除")
    private Byte isDeleted;

    @TableField(exist = false)
    private QuestionAnswer answer;

    @TableField(exist = false)
    private List<QuestionChoice> choices;

    @Schema(description = "在特定试卷中的分值",
            example = "10.0")
    @TableField(exist = false)  // 标记为非数据库字段
    private BigDecimal paperScore;
}
