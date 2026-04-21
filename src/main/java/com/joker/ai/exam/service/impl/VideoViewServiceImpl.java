package com.joker.ai.exam.service.impl;

import com.joker.ai.exam.entity.VideoViews;
import com.joker.ai.exam.mapper.VideoViewsMapper;
import com.joker.ai.exam.service.VideoViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 视频观看记录表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class VideoViewServiceImpl extends ServiceImpl<VideoViewsMapper, VideoViews> implements VideoViewService {

}
