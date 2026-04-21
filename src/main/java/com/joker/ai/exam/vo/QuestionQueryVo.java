package com.joker.ai.exam.vo;


import lombok.Data;

@Data
public class QuestionQueryVo {

    private Long categoryId;
    private String difficulty;
    private String type;
    private String keyword;
}
