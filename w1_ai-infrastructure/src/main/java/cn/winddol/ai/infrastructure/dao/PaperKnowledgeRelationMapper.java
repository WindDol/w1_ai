package cn.winddol.ai.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.winddol.ai.infrastructure.dao.po.PaperKnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaperKnowledgeRelationMapper extends BaseMapper<PaperKnowledgeRelation> {

    /**
     * 联表查询：获取某篇论文的所有关系，并带上目标论文的标题
     * 用于 Agent 的 checkPaperRelations 工具
     */
    @Select("""
        SELECT r.*, p.title as target_paper_title 
        FROM paper_knowledge_relations r
        JOIN papers p ON r.target_paper_id = p.id
        WHERE r.source_paper_id = #{paperId}
    """)
    List<Map<String, Object>> selectRelationsWithTitle(@Param("paperId") Long paperId);
}