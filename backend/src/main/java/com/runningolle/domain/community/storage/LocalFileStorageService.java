package com.runningolle.domain.community.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
        name = "app.storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootPath;
    private final String publicBasePath;

    public LocalFileStorageService(
            @Value("${app.storage.local-dir:uploads}") String localDir,
            @Value("${app.storage.public-base-path:/uploads}") String publicBasePath
    ) throws IOException {
        this.rootPath = Paths.get(localDir).toAbsolutePath().normalize();
        this.publicBasePath = publicBasePath.startsWith("/")
                ? publicBasePath
                : "/" + publicBasePath;

        Files.createDirectories(this.rootPath);
    }

    @Override
    public List<String> store(List<MultipartFile> files) throws IOException {
        List<String> storedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String fileName = UUID.randomUUID()
                    + extractExtension(file.getOriginalFilename());

            Path destination = resolveSafeFile(fileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            storedUrls.add(publicBasePath + "/" + fileName);
        }

        return storedUrls;
    }

    @Override
    public StoredFile load(String fileName) throws IOException {
        Path target = resolveSafeFile(fileName);

        if (!Files.isRegularFile(target)) {
            throw new FileNotFoundException("이미지 파일을 찾을 수 없습니다.");
        }

        String contentType = Files.probeContentType(target);
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }

        return new StoredFile(
                Files.readAllBytes(target),
                contentType
        );
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        String pathValue = fileUrl;

        try {
            if (fileUrl.startsWith("http://")
                    || fileUrl.startsWith("https://")) {
                pathValue = URI.create(fileUrl).getPath();
            }
        } catch (RuntimeException ignored) {
            return;
        }

        if (!pathValue.startsWith(publicBasePath + "/")) {
            return;
        }

        String fileName = pathValue.substring(
                (publicBasePath + "/").length()
        );

        try {
            Files.deleteIfExists(resolveSafeFile(fileName));
        } catch (IOException ignored) {
            // 파일 삭제 실패가 게시글 처리 전체를 중단시키지 않게 한다.
        }
    }

    private Path resolveSafeFile(String fileName) throws IOException {
        if (!StringUtils.hasText(fileName)) {
            throw new IOException("파일명이 올바르지 않습니다.");
        }

        Path target = rootPath.resolve(fileName).normalize();

        if (!target.startsWith(rootPath)
                || !rootPath.equals(target.getParent())) {
            throw new IOException("허용되지 않은 파일 경로입니다.");
        }

        return target;
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)
                || !fileName.contains(".")) {
            return "";
        }

        String extension = fileName
                .substring(fileName.lastIndexOf('.'))
                .toLowerCase(Locale.ROOT);

        if (!extension.matches("\\.[a-z0-9]{1,10}")) {
            return "";
        }

        return extension;
    }
}