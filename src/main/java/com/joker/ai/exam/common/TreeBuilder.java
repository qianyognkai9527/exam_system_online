package com.joker.ai.exam.common;

import java.util.*;
import java.util.stream.Collectors;
 
public class TreeBuilder {
 
    /**
     * 将平铺列表构建成树形结构
     *
     * @param nodes      所有节点（平铺）
     * @param rootParentId 根节点的 parentId（如 "0" 或 null）
     * @param <T>        节点类型
     * @return 树形列表
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes, Long rootParentId) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
 
        // 1. 创建 id -> node 的映射，提升查找效率 O(1)
        Map<Long, T> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TreeNode::getId, node -> node));
 
        // 2. 初始化 children 列表
        Map<Long, List<T>> childrenMap = new HashMap<>();
        for (T node : nodes) {
            childrenMap.put(node.getId(), new ArrayList<>());
        }
 
        // 3. 遍历所有节点，构建父子关系
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            Long parentId = node.getParentId();
            if (Objects.equals(parentId, rootParentId)) {
                // 是根节点
                roots.add(node);
            } else {
                // 找到父节点，添加到其 children
                T parent = nodeMap.get(parentId);
                if (parent != null) {
                    childrenMap.get(parentId).add(node);
                }
                // 如果父节点不存在，可选择忽略或抛异常
            }
        }
 
        // 4. 设置每个节点的 children
        for (Map.Entry<Long, List<T>> entry : childrenMap.entrySet()) {
            Long nodeId = entry.getKey();
            T node = nodeMap.get(nodeId);
            if (node != null) {
                node.setChildren(entry.getValue());
            }
        }
 
        return roots;
    }
}