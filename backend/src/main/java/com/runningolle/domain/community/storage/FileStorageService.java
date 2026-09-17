package com.runningolle.domain.community.storage;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    List<String> store(List<MultipartFile> files) throws IOException;

    StoredFile load(String fileName) throws IOException;

    void deleteByUrl(String fileUrl);
}