package cn.winddol.ai.infrastructure.utils;

import cn.winddol.ai.domain.agent.model.entity.EvalRecord;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class FileUtil {


    public void appendJson(String path, EvalRecord record) {
        try {
            // 使用Files类的write方法，指定APPEND选项来追加内容
            Files.write(Paths.get(path), JSON.toJSONString(record).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
