package cn.winddol.ai.paper.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface IFileStorageService {

    File saveTempFile(MultipartFile file) throws IOException;

    void deleteTempFile(File file);
}
