package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.ingest.StoredPaperFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface IFileStorageService {

    StoredPaperFile storeUploadedFile(String jobId, MultipartFile file) throws IOException;

    File resolveFile(String path);

    String writeTextArtifact(String jobId, String filename, String content) throws IOException;

    String readTextArtifact(String path) throws IOException;

    void deleteJobFiles(String jobId);
}
