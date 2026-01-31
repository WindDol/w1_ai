package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.po.Paper;
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
    @Test
    public void testPaper(){
        Paper paper = paperMapper.selectById(7l);
    }
}
