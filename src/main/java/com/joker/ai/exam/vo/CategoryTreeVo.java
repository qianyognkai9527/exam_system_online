package com.joker.ai.exam.vo;

import com.google.common.collect.Lists;
import com.joker.ai.exam.common.TreeNode;
import com.joker.ai.exam.entity.Category;
import lombok.Data;

import java.util.List;
@Data
public class CategoryTreeVo extends Category implements TreeNode<CategoryTreeVo> {

    private List<CategoryTreeVo> children= Lists.newArrayList();

    @Override
    public void setChildren(List<CategoryTreeVo> children) {
        this.children = children;
    }
}
