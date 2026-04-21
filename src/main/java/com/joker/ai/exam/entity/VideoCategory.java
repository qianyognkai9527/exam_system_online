package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 视频分类表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("video_categories")
@Schema(name = "VideoCategories对象", description = "视频分类表")
public class VideoCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "分类ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "分类名称")
    private String name;

    @Schema(name = "分类描述")
    private String description;

    @Schema(name = "父级分类ID，0为顶级")
    private Long parentId;

    @Schema(name = "排序权重")
    private Integer sortOrder;

    @Schema(name = "状态：1-启用，0-禁用")
    private Byte status;

    @Schema(name = "创建时间")
    private LocalDateTime createdAt;

    @Schema(name = "更新时间")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "VideoCategory{" +
            "id = " + id +
            ", name = " + name +
            ", description = " + description +
            ", parentId = " + parentId +
            ", sortOrder = " + sortOrder +
            ", status = " + status +
            ", createdAt = " + createdAt +
            ", updatedAt = " + updatedAt +
        "}";
    }
}
