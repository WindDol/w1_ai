package cn.winddol.ai.agent.librarian.api;

public interface ILibrarianAgent {
    void initiateAudit(Long paperId, String title);
    void auditAgainstLibrary(Long newPaperId);
}
