package com.joker.ai.exam.common;

import java.util.List;
 
public interface TreeNode<T> {
    Long getId();
    Long getParentId();
    void setChildren(List<T> children);
}