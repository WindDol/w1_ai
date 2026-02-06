package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.infrastructure.dao.po.Paper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
    @Select("""
        SELECT id, title, metadata, 
               1 - (abstract_embedding <=> #{vectorStr}::vector) as score
        FROM papers
        WHERE id != #{excludeId} 
          AND abstract_embedding IS NOT NULL
          AND (1 - (abstract_embedding <=> #{vectorStr}::vector)) > 0.5 -- 仅返回相关度高的
        ORDER BY abstract_embedding <=> #{vectorStr}::vector
        LIMIT #{limit}
    """)
    List<Map<String, Object>> searchSimilar(@Param("vectorStr") String vectorStr,
                                            @Param("limit") int limit,
                                            @Param("excludeId") Long excludeId);
}
