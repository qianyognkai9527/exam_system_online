package com.joker.ai.exam.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema
public class CategoryCountDto {

    private Long categoryId;
    private int count;
}
