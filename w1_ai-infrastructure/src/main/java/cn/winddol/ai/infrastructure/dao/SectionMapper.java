package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.infrastructure.dao.po.Section;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SectionMapper extends BaseMapper<Section> {
    List<Section> searchByVector(@Param("vectorStr") String vectorStr,
                                 @Param("topK") int topK);
}
