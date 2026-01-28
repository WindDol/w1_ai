package cn.winddol.ai.infrastructure.dao.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import java.util.List;

/**
 * 专门处理 List<OutlineNode> 的类型转换器
 */
public class OutlineNodeTypeHandler extends JacksonTypeHandler {

    public OutlineNodeTypeHandler(Class<?> type) {
        super(type);
    }

    // 重写解析逻辑，使用 TypeReference 明确指定泛型
    @Override
    protected Object parse(String json) {
        try {
            return getObjectMapper().readValue(json, new TypeReference<List<OutlineNode>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}