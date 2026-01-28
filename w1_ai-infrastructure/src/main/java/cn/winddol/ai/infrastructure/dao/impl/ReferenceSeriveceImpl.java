package cn.winddol.ai.infrastructure.dao.impl;

import cn.winddol.ai.infrastructure.dao.ReferenceMapper;
import cn.winddol.ai.infrastructure.dao.SymbolMapper;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ReferenceSeriveceImpl extends ServiceImpl<ReferenceMapper, Reference> implements ReferenceService{
}
