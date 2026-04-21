package com.joker.ai.exam.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 轮播图表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("banners")
@Schema(name = "Banners对象", title = "轮播图表")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "轮播图ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "轮播图标题")
    private String title;

    @Schema(name = "轮播图描述")
    private String description;

    @Schema(name = "图片URL")
    private String imageUrl;

    @Schema(name = "跳转链接")
    private String linkUrl;

    @Schema(name = "排序顺序")
    private Integer sortOrder;

    @Schema(name = "是否启用")
    private Boolean isActive;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @JsonIgnore
    @Schema(name = "更新时间")
    private LocalDateTime updateTime;

    @JsonIgnore
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
        return "Banner{" +
            "id = " + id +
            ", title = " + title +
            ", description = " + description +
            ", imageUrl = " + imageUrl +
            ", linkUrl = " + linkUrl +
            ", sortOrder = " + sortOrder +
            ", isActive = " + isActive +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", isDeleted = " + isDeleted +
        "}";
    }
}
