package cn.winddol.ai.infrastructure.utils;

import cn.winddol.ai.paper.model.entity.OutlineNode;
import cn.winddol.ai.paper.model.entity.SectionPO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreeBuilderUtil {

    /**
     * 将扁平的 SectionPO 列表转换为树状结构
     */
    public static List<OutlineNode> buildTree(List<SectionPO> flatSections) {
        List<OutlineNode> roots = new ArrayList<>();
        // 使用 Map 暂存所有节点，方便查找
        Map<String, OutlineNode> nodeMap = new LinkedHashMap<>();

        // 1. 先把所有 PO 转成 Node
        for (SectionPO po : flatSections) {
            OutlineNode node = OutlineNode.builder().id(po.uuid).title(po.header).level(po.level)
                    .build();
            nodeMap.put(po.uuid, node);
        }

        // 2. 组装树关系
        for (SectionPO po : flatSections) {
            OutlineNode currentNode = nodeMap.get(po.uuid);

            // 如果 parentId 为空，或者 parentId 在 Map 里找不到（可能是虚拟根），则视为根节点
            if (po.parentId == null || !nodeMap.containsKey(po.parentId)) {
                roots.add(currentNode);
            } else {
                // 找到父节点，加入其 children 列表
                OutlineNode parent = nodeMap.get(po.parentId);
                parent.getChildren().add(currentNode);
            }
        }
        return roots;
    }
}