package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.paper.api.IFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

    @Value("${upload.path:./temp_pdf/}")
    private String uploadPath;

    @Override
    public File saveTempFile(MultipartFile file) throws IOException {
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();
        File tempFile = new File(dir, UUID.randomUUID() + ".pdf");
        file.transferTo(tempFile);
        return tempFile;
    }

    @Override
    public void deleteTempFile(File file) {
        if (file != null && file.exists()) file.delete();
    }
}
