package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joker.ai.exam.entity.Banner;
import com.joker.ai.exam.mapper.BannersMapper;
import com.joker.ai.exam.service.BannerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.ai.exam.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 轮播图表 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannersMapper, Banner> implements BannerService {


    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public List<Banner> listData() {
        LambdaQueryWrapper<Banner> bannerLambdaQueryWrapper = Wrappers.<Banner>lambdaQuery().orderByAsc(Banner::getSortOrder);
        List<Banner> list = this.list(bannerLambdaQueryWrapper);

        return list;
    }

    @Override
    public List<Banner> getActiveList() {
        LambdaQueryWrapper<Banner> eq = Wrappers.<Banner>lambdaQuery().orderByAsc(Banner::getSortOrder)
                .eq(Banner::getIsActive, true);
        return this.list(eq);

    }

    @Override
    public void updateActiveStatus(Long id, Boolean isActive) {
        LambdaUpdateWrapper<Banner> updateWrapper = Wrappers.<Banner>lambdaUpdate().eq(Banner::getId, id)
                .set(Banner::getIsActive, isActive);
        this.update(updateWrapper);
    }

    @Override
    public void removeData(Long id) {
        Banner banner   = getById(id);
        if(Objects.isNull( banner)){
            throw new RuntimeException("数据不存在");
        }
        removeById(id);
    }

    @Override
    public String uploadImage(MultipartFile file) throws Exception {
        if(file.isEmpty()){
            throw new RuntimeException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        if(Objects.isNull(contentType)|| !contentType.startsWith("image/")){
            throw new RuntimeException("上传文件格式错误");
        }
        if(file.getSize()>1024*1024*5){
            throw new RuntimeException("上传文件不能超过5MB");
        }

        String imgUrl = fileUploadService.uploadFile(file, "banners");

        return imgUrl;
    }

    @Override
    public void saveBanner(Banner banner) {
        if(Objects.isNull(banner.getIsActive())){
            banner.setIsActive(true);
        }
        if(Objects.isNull(banner.getSortOrder())){
            banner.setSortOrder(0);
        }
        save(banner);
    }
}
