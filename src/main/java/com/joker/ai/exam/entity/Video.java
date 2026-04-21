package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 视频信息表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("videos")
@Schema(name = "Videos对象", description = "视频信息表")
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "视频ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "视频标题")
    private String title;

    @Schema(name = "视频描述")
    private String description;

    @Schema(name = "分类ID")
    private Long categoryId;

    @Schema(name = "视频文件URL")
    private String fileUrl;

    @Schema(name = "封面图片URL")
    private String coverUrl;

    @Schema(name = "视频时长（秒）")
    private Integer duration;

    @Schema(name = "文件大小（字节）")
    private Long fileSize;

    @Schema(name = "上传者名称")
    private String uploaderName;

    @Schema(name = "上传者类型：1-用户投稿，2-管理员上传")
    private Byte uploaderType;

    @Schema(name = "上传用户ID（用户投稿时）")
    private Long userId;

    @Schema(name = "管理员ID（管理员上传时）")
    private Long adminId;

    @Schema(name = "状态：0-待审核，1-已发布，2-已拒绝，3-已下架")
    private Byte status;

    @Schema(name = "审核管理员ID")
    private Long auditAdminId;

    @Schema(name = "审核时间")
    private LocalDateTime auditTime;

    @Schema(name = "审核原因（拒绝时）")
    private String auditReason;

    @Schema(name = "观看次数")
    private Long viewCount;

    @Schema(name = "点赞次数")
    private Long likeCount;

    @Schema(name = "标签，逗号分隔")
    private String tags;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public Byte getUploaderType() {
        return uploaderType;
    }

    public void setUploaderType(Byte uploaderType) {
        this.uploaderType = uploaderType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Long getAuditAdminId() {
        return auditAdminId;
    }

    public void setAuditAdminId(Long auditAdminId) {
        this.auditAdminId = auditAdminId;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditReason() {
        return auditReason;
    }

    public void setAuditReason(String auditReason) {
        this.auditReason = auditReason;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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
        return "Video{" +
            "id = " + id +
            ", title = " + title +
            ", description = " + description +
            ", categoryId = " + categoryId +
            ", fileUrl = " + fileUrl +
            ", coverUrl = " + coverUrl +
            ", duration = " + duration +
            ", fileSize = " + fileSize +
            ", uploaderName = " + uploaderName +
            ", uploaderType = " + uploaderType +
            ", userId = " + userId +
            ", adminId = " + adminId +
            ", status = " + status +
            ", auditAdminId = " + auditAdminId +
            ", auditTime = " + auditTime +
            ", auditReason = " + auditReason +
            ", viewCount = " + viewCount +
            ", likeCount = " + likeCount +
            ", tags = " + tags +
            ", createdAt = " + createdAt +
            ", updatedAt = " + updatedAt +
        "}";
    }
}
