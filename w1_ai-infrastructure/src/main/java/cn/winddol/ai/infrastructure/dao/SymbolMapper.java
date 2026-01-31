package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SymbolMapper extends BaseMapper<Symbol> {
    List<Symbol> searchByVector(@Param("vectorStr") String vectorStr,
                                @Param("topK") int topK );
}
