package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.infrastructure.dao.po.Section;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SectionMapper extends BaseMapper<Section> {
    List<SearchResultDTO.SectionDTO> searchByVector(@Param("paperId") Long paperId,
                                                   @Param("vectorStr") String vectorStr, @Param("topK") int topK);
}
