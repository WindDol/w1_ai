package cn.winddol.ai.types.common.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;
import java.util.stream.Collectors;

public class FingerprintUtils {
    /**
     * 生成增强型指纹：MD5(排序后的所有作者姓氏 + 年份 + 标题前缀)
     */
    public static String generateRefFingerprint(List<String> surnames, Integer year) {
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
