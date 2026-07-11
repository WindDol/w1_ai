package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.parser.PythonParserAdapter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MonkeyOcrExternalIT {

    @Resource
    private PythonParserAdapter ip;


    @Test
    public void parsePdfThroughConfiguredMonkeyOcr() {
        String pdfPath = System.getenv("MONKEYOCR_TEST_PDF");
        Assume.assumeTrue("Set MONKEYOCR_TEST_PDF to run this external test",
                pdfPath != null && !pdfPath.isBlank());

        String markdown = ip.parsePdfToMarkdown(pdfPath);
        assertFalse(markdown.isBlank());
        log.info("MonkeyOCR external test returned {} characters", markdown.length());
    }
}
