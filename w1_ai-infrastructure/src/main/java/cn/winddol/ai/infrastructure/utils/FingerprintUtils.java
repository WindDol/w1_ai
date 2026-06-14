package cn.winddol.ai.infrastructure.utils;

import cn.winddol.ai.paper.api.IFingerprintUtils;
import org.apache.commons.codec.digest.DigestUtils;import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FingerprintUtils implements IFingerprintUtils {
    /**
     * 生成增强型指纹：MD5(排序后的所有作者姓氏 + 年份 + 标题前缀)
     */
    public String generateRefFingerprint(List<String> surnames, Integer year) {
        String sYear = (year == null) ? "0000" : String.valueOf(year);

        // 1. 作者标准化：去空格、转小写、排序
        String authorPath = "unknown";
        if (surnames != null && !surnames.isEmpty()) {
            authorPath = surnames.stream()
                    .map(s -> s.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                    .sorted()
                    .collect(Collectors.joining("|"));
        }

        return DigestUtils.md5Hex(authorPath + "|" + sYear + "|" );
    }
}
