package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.joker.ai.exam.entity.Video;
import com.joker.ai.exam.mapper.VideoMapper;
import com.joker.ai.exam.service.VideoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 视频信息表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Override
    public IPage<Video> getVideosForAdmin(Integer page, Integer size, Integer status, Integer uploaderType, String keyword) {
        return null;
    }

    @Override
    public Map<String, Object> uploadVideoByAdmin(Video video, MultipartFile videoFile, MultipartFile coverFile, Long adminId) {
        return Map.of();
    }

    @Override
    public void auditVideo(Long videoId, Integer status, String reason, Long adminId) {

    }

    @Override
    public void offlineVideo(Long videoId, Long adminId) {

    }

    @Override
    public void deleteVideo(Long videoId) {

    }

    @Override
    public Map<String, Object> getVideoStatistics() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getVideoDetailStats(Long videoId, Integer days) {
        return Map.of();
    }

    @Override
    public IPage<Video> getPublishedVideos(Integer page, Integer size, Long categoryId, String keyword, HttpServletRequest request) {
        return null;
    }

    @Override
    public Video getVideoDetail(Long id, HttpServletRequest request) {
        return null;
    }

    @Override
    public List<Video> getPopularVideos(Integer limit) {
        return List.of();
    }

    @Override
    public List<Video> getLatestVideos(Integer limit) {
        return List.of();
    }

    @Override
    public void recordVideoView(Long videoId, Integer viewDuration, HttpServletRequest request) {

    }

    @Override
    public boolean toggleVideoLike(Long videoId, HttpServletRequest request) {
        return false;
    }

    @Override
    public Map<String, Object> submitVideo(Video video, MultipartFile videoFile, MultipartFile coverFile) {
        return Map.of();
    }
}
