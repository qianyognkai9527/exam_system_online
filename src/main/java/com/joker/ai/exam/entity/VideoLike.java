package com.joker.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 视频点赞表
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@TableName("video_likes")
@Schema(name = "VideoLikes对象", description = "视频点赞表")
public class VideoLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "点赞ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "视频ID")
    private Long videoId;

    @Schema(name = "用户IP（匿名点赞）")
    private String userIp;

    @Schema(name = "用户代理信息")
    private String userAgent;

    @Schema(name = "点赞时间")
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "VideoLike{" +
            "id = " + id +
            ", videoId = " + videoId +
            ", userIp = " + userIp +
            ", userAgent = " + userAgent +
            ", createdAt = " + createdAt +
        "}";
    }
}
