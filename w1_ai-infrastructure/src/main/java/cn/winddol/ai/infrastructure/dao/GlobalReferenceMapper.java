package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.infrastructure.dao.po.GlobalReference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GlobalReferenceMapper extends BaseMapper<GlobalReference> {
    @Select("""
    SELECT id, title, abstract as abstractText, linked_paper_id as linkedPaperId, source_type
    FROM global_references
    WHERE id = #{id}
""")
    GlobalReference selectGlobalById(@Param("id") Long id);

    List<GlobalReference> getTopFrequentReferences(@Param("limit") Integer limit);
}
