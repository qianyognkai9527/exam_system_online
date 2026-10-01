package com.joker.ai.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前登录用户资料")
public class UserProfileVo {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    @Schema(description = "角色", allowableValues = {"ADMIN", "TEACHER", "STUDENT"})
    private String role;
}
