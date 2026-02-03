package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.ReferenceMapper;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import dev.ai4j.openai4j.Json;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Repeat;

@Slf4j
@SpringBootTest
public class TestMapper {
    @Resource
    private PaperMapper paperMapper;
    @Resource
    private ReferenceMapper referenceMapper;
    @Test
    public void testPaper(){
        Paper paper = paperMapper.selectById(7l);
    }

    @Test
    public void testReference(){
        Reference reference = new Reference();
        reference.setId(24l);
        reference.setTitle("1");
        referenceMapper.updateById(reference);

    }
}
