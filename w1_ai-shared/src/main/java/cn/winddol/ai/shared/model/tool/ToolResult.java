package cn.winddol.ai.shared.model.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    private boolean success;
    private String content;
    private String error;
    @Builder.Default
    private List<ToolEvidence> evidence = List.of();

    public static ToolResult ok(String content) {
        return ToolResult.builder().success(true).content(content).build();
    }

    /**
     * 返回工具文本和该次调用实际读取到的证据，供上层输出可复查阅读轨迹。
     */
    public static ToolResult ok(String content, List<ToolEvidence> evidence) {
        return ToolResult.builder()
                .success(true)
                .content(content)
                .evidence(evidence == null ? List.of() : List.copyOf(evidence))
                .build();
    }

    public static ToolResult fail(String error) {
        return ToolResult.builder().success(false).error(error).build();
    }
}
