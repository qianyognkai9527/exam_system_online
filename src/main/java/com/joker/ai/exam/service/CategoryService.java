package com.joker.ai.exam.service;

import com.joker.ai.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.ai.exam.vo.CategoryTreeVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
public interface CategoryService extends IService<Category> {

    List<CategoryTreeVo> treeList();

    List<Category> findCategory();

    void saveData(Category category);

    void updateData(Category category);

    void deleteCategory(Long id);
}
