package cn.winddol.ai.shared.model.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用过程中实际读取到的可复查证据。
 *
 * 该对象只记录来源和原文片段，不对 Agent 最终结论做真实性判断。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolEvidence {

    private String evidenceKey;
    private String evidenceType;
    private Long paperId;
    private String paperTitle;
    private String sectionId;
    private String chunkId;
    private String heading;
    private String headingPath;
    private Integer pageStart;
    private Integer pageEnd;
    private String referenceIndex;
    private String quote;
    private String accessedVia;
}
