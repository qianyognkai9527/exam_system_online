package com.joker.ai.exam.service;

import com.joker.ai.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 轮播图表 服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface BannerService extends IService<Banner> {

    List<Banner> listData();

    List<Banner> getActiveList();

    void updateActiveStatus(Long id, Boolean isActive);

    void removeData(Long id);

    String uploadImage(MultipartFile file) throws Exception;

    void saveBanner(Banner banner);
}
