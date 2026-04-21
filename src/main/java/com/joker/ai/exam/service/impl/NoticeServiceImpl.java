package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.ai.exam.common.Result;
import com.joker.ai.exam.entity.Notice;
import com.joker.ai.exam.mapper.NoticeMapper;
import com.joker.ai.exam.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 公告表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public Result<List<Notice>> getActiveNotices() {
        return null;
    }

    @Override
    public Result<List<Notice>> getLatestNotices(int limit) {
        return null;
    }

    @Override
    public Result<List<Notice>> getAllNotices() {
        return null;
    }

    @Override
    public Result<String> addNotice(Notice notice) {
        return null;
    }

    @Override
    public Result<String> updateNotice(Notice notice) {
        return null;
    }

    @Override
    public Result<String> deleteNotice(Long id) {
        return null;
    }

    @Override
    public Result<String> toggleNoticeStatus(Long id, Boolean isActive) {
        return null;
    }
}
