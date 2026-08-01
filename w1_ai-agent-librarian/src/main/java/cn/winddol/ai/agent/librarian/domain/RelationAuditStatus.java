package cn.winddol.ai.agent.librarian.domain;

/**
 * 关系审计结果是否可直接展示，低置信或缺少证据时必须人工复核。
 */
public enum RelationAuditStatus {
    CONFIRMED,
    PENDING_REVIEW,
    LEGACY
}
