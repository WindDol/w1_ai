package cn.winddol.ai.paper.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface IFileStorageService {

    File saveTempFile(MultipartFile file);

    void deleteTempFile(File file);
}
