package com.runningolle.domain.community.controller;

import com.runningolle.domain.community.dto.ImageUploadResponse;
import com.runningolle.domain.community.storage.FileStorageService;
import com.runningolle.domain.community.storage.StoredFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/community/feed/images")
@RequiredArgsConstructor
public class FeedImageController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ImageUploadResponse> upload(
            @RequestPart("files") List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업로드할 파일이 없습니다."
            );
        }

        try {
            return ResponseEntity.ok(
                    new ImageUploadResponse(
                            fileStorageService.store(files)
                    )
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지 업로드에 실패했습니다."
            );
        }
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String fileName
    ) {
        try {
            StoredFile storedFile =
                    fileStorageService.load(fileName);

            MediaType mediaType;

            try {
                mediaType = MediaType.parseMediaType(
                        storedFile.contentType()
                );
            } catch (RuntimeException exception) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(
                            CacheControl.maxAge(
                                    Duration.ofDays(30)
                            ).cachePublic()
                    )
                    .body(storedFile.content());

        } catch (FileNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이미지를 찾을 수 없습니다."
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지 조회에 실패했습니다."
            );
        }
    }
}