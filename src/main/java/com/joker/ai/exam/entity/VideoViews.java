package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 视频观看记录表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("video_views")
@Schema(name = "VideoViews对象", description = "视频观看记录表")
public class VideoViews implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "观看ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "视频ID")
    private Long videoId;

    @Schema(name = "用户IP")
    private String userIp;

    @Schema(name = "用户代理信息")
    private String userAgent;

    @Schema(name = "观看时长（秒）")
    private Integer viewDuration;

    @Schema(name = "观看时间")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Integer getViewDuration() {
        return viewDuration;
    }

    public void setViewDuration(Integer viewDuration) {
        this.viewDuration = viewDuration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "VideoViews{" +
            "id = " + id +
            ", videoId = " + videoId +
            ", userIp = " + userIp +
            ", userAgent = " + userAgent +
            ", viewDuration = " + viewDuration +
            ", createdAt = " + createdAt +
        "}";
    }
}
