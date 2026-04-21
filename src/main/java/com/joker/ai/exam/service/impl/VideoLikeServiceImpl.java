package com.joker.ai.exam.service.impl;

import com.joker.ai.exam.entity.VideoLike;
import com.joker.ai.exam.mapper.VideoLikesMapper;
import com.joker.ai.exam.service.VideoLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 视频点赞表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class VideoLikeServiceImpl extends ServiceImpl<VideoLikesMapper, VideoLike> implements VideoLikeService {

}
