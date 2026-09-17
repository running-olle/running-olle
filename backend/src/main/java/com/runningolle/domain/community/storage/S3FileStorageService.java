package com.runningolle.domain.community.storage;

import jakarta.annotation.PreDestroy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(
        name = "app.storage.type",
        havingValue = "s3"
)
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;
    private final String publicBasePath;

    public S3FileStorageService(
            @Value("${app.storage.s3.region}") String region,
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.prefix:feed-images}") String prefix,
            @Value("${app.storage.s3.public-base-path:/api/community/feed/images/files}")
            String publicBasePath
    ) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.bucket = bucket;
        this.prefix = trimSlashes(prefix);
        this.publicBasePath = normalizeBasePath(publicBasePath);
    }

    @Override
    public List<String> store(List<MultipartFile> files)
            throws IOException {

        List<String> storedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String fileName = UUID.randomUUID()
                    + extractExtension(file.getOriginalFilename());

            String objectKey = createObjectKey(fileName);

            String contentType = StringUtils.hasText(file.getContentType())
                    ? file.getContentType()
                    : "application/octet-stream";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(
                        request,
                        RequestBody.fromInputStream(
                                inputStream,
                                file.getSize()
                        )
                );
            } catch (SdkException exception) {
                throw new IOException(
                        "S3 이미지 업로드에 실패했습니다.",
                        exception
                );
            }

            storedUrls.add(publicBasePath + "/" + fileName);
        }

        return storedUrls;
    }

    @Override
    public StoredFile load(String fileName) throws IOException {
        validateFileName(fileName);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(createObjectKey(fileName))
                .build();

        try {
            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObjectAsBytes(request);

            String contentType = response.response().contentType();
            if (!StringUtils.hasText(contentType)) {
                contentType = "application/octet-stream";
            }

            return new StoredFile(
                    response.asByteArray(),
                    contentType
            );
        } catch (NoSuchKeyException exception) {
            throw new FileNotFoundException(
                    "S3 이미지 파일을 찾을 수 없습니다."
            );
        } catch (SdkException exception) {
            throw new IOException(
                    "S3 이미지 조회에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        String fileName = extractFileName(fileUrl);

        if (!StringUtils.hasText(fileName)) {
            return;
        }

        try {
            validateFileName(fileName);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(createObjectKey(fileName))
                    .build();

            s3Client.deleteObject(request);
        } catch (IOException | SdkException ignored) {
            // S3 삭제 실패가 게시글 처리 전체를 중단시키지 않게 한다.
        }
    }

    @PreDestroy
    public void close() {
        s3Client.close();
    }

    private String createObjectKey(String fileName) {
        return prefix + "/" + fileName;
    }

    private void validateFileName(String fileName) throws IOException {
        if (!StringUtils.hasText(fileName)
                || fileName.equals(".")
                || fileName.equals("..")
                || !fileName.matches("[a-zA-Z0-9._-]+")) {
            throw new IOException("허용되지 않은 파일명입니다.");
        }
    }

    private String extractFileName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }

        String pathValue = fileUrl;

        try {
            if (fileUrl.startsWith("http://")
                    || fileUrl.startsWith("https://")) {
                pathValue = URI.create(fileUrl).getPath();
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        String expectedPrefix = publicBasePath + "/";

        if (!pathValue.startsWith(expectedPrefix)) {
            return null;
        }

        return pathValue.substring(expectedPrefix.length());
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

    private String trimSlashes(String value) {
        String result = value == null ? "" : value.trim();

        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String normalizeBasePath(String value) {
        String result = value.startsWith("/")
                ? value
                : "/" + value;

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}