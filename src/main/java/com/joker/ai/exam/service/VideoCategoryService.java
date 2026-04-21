package com.joker.ai.exam.service;

import com.joker.ai.exam.entity.VideoCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 视频分类表 服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface VideoCategoryService extends IService<VideoCategory> {

    void deleteCategory(Long id);

    void updateCategory(VideoCategory category);


    void addCategory(VideoCategory category);

    VideoCategory getCategoryById(Long id);

    List<VideoCategory> getChildCategories(Long parentId);

    List<VideoCategory> getTopCategories();

    List<VideoCategory> getCategoryTree();

    List<VideoCategory> getAllCategories();


}
