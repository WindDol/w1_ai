package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReferenceMapper extends BaseMapper<Reference> {
    List<SearchResultDTO.ReferenceDTO> searchReferencesByVector(
            @Param("paperId") Long paperId,
            @Param("vectorStr") String vectorStr,
            @Param("topK") int topK
    );
}
