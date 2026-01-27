package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.infrastructure.dao.po.Paper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}
