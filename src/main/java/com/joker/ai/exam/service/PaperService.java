package com.joker.ai.exam.service;

import com.joker.ai.exam.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.ai.exam.vo.AiPaperVo;
import com.joker.ai.exam.vo.PaperVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface PaperService extends IService<Paper> {

    Paper createPaper(PaperVo paperVo);

    Paper createPaperWithAI(AiPaperVo aiPaperVo);

    void updatePaper(Integer id, PaperVo paperVo);

    void updatePaperStatus(Integer id, String status);

    void removePaper(Integer id);

    Paper getPaperDetail(Long id);
}
