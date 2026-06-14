package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.paper.domain.SearchResultDTO;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReferenceMapper extends BaseMapper<Reference> {
    List<SearchResultDTO.ReferenceDTO> searchReferencesByVector(
            @Param("paperId") Long paperId,
            @Param("vectorStr") String vectorStr,
            @Param("topK") int topK
    );


    ReferenceItem selectJoinedReference(@Param("paperId") Long paperId,@Param("refIndex")  String refIndex);

    @Select("""
    SELECT global_ref_id 
    FROM paper_references 
    WHERE paper_id = #{paperId} 
      AND (ref_index = #{refIndex} OR ref_index = '[' || #{refIndex} || ']')
    LIMIT 1
""")
    Long selectGlobalRefId(@Param("paperId") Long paperId, @Param("refIndex") String refIndex);
}
