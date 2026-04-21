package com.joker.ai.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.joker.ai.exam.common.TreeBuilder;
import com.joker.ai.exam.entity.Category;
import com.joker.ai.exam.entity.Question;
import com.joker.ai.exam.mapper.CategoriesMapper;
import com.joker.ai.exam.mapper.QuestionsMapper;
import com.joker.ai.exam.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.ai.exam.service.QuestionService;
import com.joker.ai.exam.vo.CategoryCountDto;
import com.joker.ai.exam.vo.CategoryTreeVo;
import jodd.bean.BeanUtil;
import kotlin.jvm.internal.Lambda;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, Category> implements CategoryService {

    @Autowired
    private QuestionsMapper questionsMapper;


    @Override
    public List<CategoryTreeVo> treeList() {
        List<Category> list = list();
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }
        List<CategoryTreeVo> collect = list.stream().map(item -> {
            CategoryTreeVo categoryTreeVo = new CategoryTreeVo();
            BeanUtils.copyProperties(item, categoryTreeVo);
            return categoryTreeVo;
        }).collect(Collectors.toList());
        List<CategoryTreeVo> categoryTreeVos = TreeBuilder.buildTree(collect, 0L);
        return categoryTreeVos;
    }

    @Override
    public List<Category> findCategory() {
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort);
        List<Category> list = this.list(categoryLambdaQueryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<CategoryCountDto> categoryCountDtos = questionsMapper.selectCategoryCount();
        Map<Long, Integer> collect = categoryCountDtos.stream().collect(Collectors.toMap(CategoryCountDto::getCategoryId, CategoryCountDto::getCount));
        list.stream().forEach(item -> {
            item.setCount(collect.getOrDefault(item.getId(), 0));
        });
        return list;
    }

    @Override
    public void saveData(Category category) {
        Long parentId = category.getParentId();
        LambdaQueryWrapper<Category> eq = Wrappers.<Category>lambdaQuery().eq(Category::getId, parentId)
                .eq(Category::getName, category.getName());
        Category one = this.getOne(eq);
        if (one != null) {
            throw new RuntimeException("该分类已存在");
        }
        this.save(category);
    }

    @Override
    public void updateData(Category category) {

        Long parentId = category.getParentId();
        LambdaQueryWrapper<Category> ne = Wrappers.<Category>lambdaQuery().eq(Category::getId, parentId)
                .eq(Category::getName, category.getName())
                .ne(Category::getId, category.getId())
                .last(" limit 1");
        Category one = this.getOne(ne);
        if (one != null) {
            throw new RuntimeException("该分类已存在");
        }
        this.updateById(category);

    }

    @Override
    public void deleteCategory(Long id) {
        Category category = getById(id);
        if(category.getParentId()==0){
            throw new RuntimeException("不能删除根节点");
        }
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper=new LambdaQueryWrapper<>();
        questionLambdaQueryWrapper.eq(Question::getCategoryId,id);
        Long l = questionsMapper.selectCount(questionLambdaQueryWrapper);
        if(l>0){
            throw new RuntimeException("该分类下有"+l+"个题目，请先删除题目");
        }
        removeById(id);
    }
}
