package com.joker.ai.exam.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.joker.ai.exam.entity.Video;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 视频信息表 服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface VideoService extends IService<Video> {

    IPage<Video> getVideosForAdmin(Integer page, Integer size, Integer status, Integer uploaderType, String keyword);

    Map<String, Object> uploadVideoByAdmin(Video video, MultipartFile videoFile, MultipartFile coverFile, Long adminId);

    void auditVideo(Long videoId, Integer status, String reason, Long adminId);

    void offlineVideo(Long videoId, Long adminId);

    void deleteVideo(Long videoId);

    Map<String, Object> getVideoStatistics();

    Map<String, Object> getVideoDetailStats(Long videoId, Integer days);

    IPage<Video> getPublishedVideos(Integer page, Integer size, Long categoryId, String keyword, HttpServletRequest request);

    Video getVideoDetail(Long id, HttpServletRequest request);

    List<Video> getPopularVideos(Integer limit);

    List<Video> getLatestVideos(Integer limit);

    void recordVideoView(Long videoId, Integer viewDuration, HttpServletRequest request);


    boolean toggleVideoLike(Long videoId, HttpServletRequest request);

    Map<String, Object> submitVideo(Video video, MultipartFile videoFile, MultipartFile coverFile);

}
