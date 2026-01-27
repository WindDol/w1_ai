package cn.winddol.ai.test;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTest
public class TestMarkdownParser {

    @Test
    public void test() throws Exception {
        // 1. 读取昨天的文件
        String filePath = "C:\\Users\\Mr Ding.LAPTOP-H54HCE12\\Desktop\\ai_pdf\\python\\output_test.md"; // 确保路径对
        String markdown = Files.readString(Paths.get(filePath));

        // 2. 解析
        MarkdownParser parser = new MarkdownParser();
        List<SectionPO> sections = parser.parse(markdown);

        // 3. 打印结果验证
        System.out.println("====== 解析结果 ======");
        System.out.println("共发现章节数: " + sections.size());

        for (SectionPO sec : sections) {
            System.out.println("------------------------------------------------");
            System.out.println("UUID: " + sec.uuid);
            System.out.println("Header: " + "#".repeat(sec.level) + " " + sec.header);
            System.out.println("Content Length: " + sec.getContent().length());
            // 打印前 50 个字看看内容对不对
            String preview = sec.getContent().length() > 50 ? sec.getContent().substring(0, 50) : sec.getContent();
            System.out.println("Content Preview: " + preview.replace("\n", " "));
        }

        // 4. 构建 Outline JSON
        System.out.println("\n====== Outline JSON (存入 papers 表) ======");
        System.out.println(parser.buildFlatOutline(sections).toString());
    }

    @Resource
    private IPaperRepository repository;

    @Test
    public void testInsert() throws Exception {
        // 1. 读取并解析文件 (Day 2 的代码)
        String markdown = Files.readString(Paths.get("C:\\Users\\Mr Ding.LAPTOP-H54HCE12\\Desktop\\ai_pdf\\python\\output_test.md"));
        MarkdownParser parser = new MarkdownParser();
        List<SectionPO> pos = parser.parse(markdown);

        // 2. 调用 Service 入库
        String title = "Identical phase oscillators with global sinusoidal coupling evolve by Möbius group action";
        repository.saveFullPaper(title, pos);

        System.out.println("入库成功！请在 DBeaver 中查看数据。");
    }
}
