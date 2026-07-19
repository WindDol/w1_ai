package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.SectionEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class HeadingPathResolver {

    /**
     * 将已有的 Section 父子层级投影为可检索的标题路径。
     * 此方法不会重新构建或修改论文原始 Outline。
     */
    public Map<String, String> resolve(List<SectionEntity> sections) {
        Map<String, SectionEntity> byId = new HashMap<>();
        for (SectionEntity section : sections) {
            byId.put(section.getId(), section);
        }

        Map<String, String> paths = new HashMap<>();
        for (SectionEntity section : sections) {
            List<String> headings = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            SectionEntity current = section;
            // visited 用于防止异常父子数据形成循环引用，导致索引任务死循环。
            while (current != null && current.getId() != null && visited.add(current.getId())) {
                if (current.getHeader() != null && !current.getHeader().isBlank()) {
                    headings.add(current.getHeader().trim());
                }
                current = current.getParentId() == null ? null : byId.get(current.getParentId());
            }
            Collections.reverse(headings);
            paths.put(section.getId(), String.join(" > ", headings));
        }
        return paths;
    }
}
