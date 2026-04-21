package com.joker.ai.exam.service.impl;

import com.joker.ai.exam.entity.VideoCategory;
import com.joker.ai.exam.mapper.VideoCategoriesMapper;
import com.joker.ai.exam.service.VideoCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 视频分类表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class VideoCategoryServiceImpl extends ServiceImpl<VideoCategoriesMapper, VideoCategory> implements VideoCategoryService {

    @Override
    public void deleteCategory(Long id) {

    }

    @Override
    public void updateCategory(VideoCategory category) {

    }

    @Override
    public void addCategory(VideoCategory category) {

    }

    @Override
    public VideoCategory getCategoryById(Long id) {
        return null;
    }

    @Override
    public List<VideoCategory> getChildCategories(Long parentId) {
        return List.of();
    }

    @Override
    public List<VideoCategory> getTopCategories() {
        return List.of();
    }

    @Override
    public List<VideoCategory> getCategoryTree() {
        return List.of();
    }

    @Override
    public List<VideoCategory> getAllCategories() {
        return List.of();
    }
}
