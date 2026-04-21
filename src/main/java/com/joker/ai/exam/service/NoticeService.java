package com.joker.ai.exam.service;

import com.joker.ai.exam.common.Result;
import com.joker.ai.exam.entity.Notice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 公告表 服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface NoticeService extends IService<Notice> {

    Result<List<Notice>> getActiveNotices();

    Result<List<Notice>> getLatestNotices(int limit);

    Result<List<Notice>> getAllNotices();

    Result<String> addNotice(Notice notice);

    Result<String> updateNotice(Notice notice);

    Result<String> deleteNotice(Long id);

    Result<String> toggleNoticeStatus(Long id, Boolean isActive);

}
