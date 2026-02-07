package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.domain.paperTools.model.entity.PaperEntity;
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
        SELECT id, title, metadata, abstract as abstractText,
               1 - (embedding <=> #{vectorStr}::vector) as score
        FROM papers
        WHERE id != #{excludeId}
          AND embedding IS NOT NULL
          AND (1 - (embedding <=> #{vectorStr}::vector)) > 0.5 -- 仅返回相关度高的
        ORDER BY embedding <=> #{vectorStr}::vector
        LIMIT #{limit}
    """)
    List<Map<String, Object>> searchSimilar(@Param("vectorStr") String vectorStr,
                                            @Param("limit") int limit,
                                            @Param("excludeId") Long excludeId);

    List<PaperEntity> searchPapers(@Param("query")String query, @Param("vectorStr")String vectorStr,@Param("threshold") double v, @Param("limit")int i);
}
