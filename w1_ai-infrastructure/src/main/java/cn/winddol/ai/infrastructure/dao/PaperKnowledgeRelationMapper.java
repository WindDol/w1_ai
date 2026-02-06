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
        -- 第一部分：当前论文是作者（Source），评价别人（Target）
        SELECT 
            r.target_paper_id as related_paper_id, 
            p.title as related_paper_title, 
            r.relation_type, 
            r.description,
            'OUTGOING' as direction -- 主动评价
        FROM paper_knowledge_relations r
        JOIN papers p ON r.target_paper_id = p.id
        WHERE r.source_paper_id = #{paperId}
        
        UNION ALL
        
        -- 第二部分：当前论文是被评价者（Target），别人（Source）评价它
        SELECT 
            r.source_paper_id as related_paper_id, 
            p.title as related_paper_title, 
            r.relation_type, 
            r.description,
            'INCOMING' as direction -- 被动评价
        FROM paper_knowledge_relations r
        JOIN papers p ON r.source_paper_id = p.id
        WHERE r.target_paper_id = #{paperId}
    """)
    List<Map<String, Object>> selectBidirectionalRelations(@Param("paperId") Long paperId);
}