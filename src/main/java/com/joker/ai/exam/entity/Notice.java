package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("notices")
@Schema(name = "Notices对象", description = "公告表")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "公告ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "公告标题")
    private String title;

    @Schema(name = "公告内容")
    private String content;

    @Schema(name = "公告类型：SYSTEM(系统)、FEATURE(新功能)、NOTICE(通知)")
    private String type;

    @Schema(name = "优先级：0-普通，1-重要，2-紧急")
    private Integer priority;

    @Schema(name = "是否启用")
    private Boolean isActive;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "更新时间")
    private LocalDateTime updateTime;

    @Schema(name = "0-未删除，1-已删除")
    private Byte isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Byte getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Byte isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "Notice{" +
            "id = " + id +
            ", title = " + title +
            ", content = " + content +
            ", type = " + type +
            ", priority = " + priority +
            ", isActive = " + isActive +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", isDeleted = " + isDeleted +
        "}";
    }
}
