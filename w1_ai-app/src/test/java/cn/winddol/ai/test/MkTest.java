package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.parser.PythonParserAdapter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MkTest {

    @Resource
    private PythonParserAdapter ip;


    @Test
    public void test() {
        String s = ip.parsePdfToMarkdown("C:\\Users\\Mr Ding.LAPTOP-H54HCE12\\Desktop\\ai_pdf\\18.pdf");
        log.info(s);
    }
}
