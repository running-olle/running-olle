package com.runningolle.domain.mypage.controller;

import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.mypage.service.MyPageService;
import com.runningolle.domain.community.dto.ImageUploadResponse;
import com.runningolle.domain.community.storage.FileStorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;
    private final FileStorageService fileStorageService;
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
    @GetMapping public MyPageDtos.Dashboard dashboard(Authentication authentication) { return service.dashboard(userId(authentication)); }
    @GetMapping("/runs") public List<MyPageDtos.Run> runs(Authentication authentication) { return service.runs(userId(authentication)); }
    @GetMapping("/visits") public List<MyPageDtos.Visit> visits(Authentication authentication) { return service.visits(userId(authentication)); }
    @GetMapping("/runs/{id}") public MyPageDtos.RunDetail runDetail(Authentication authentication, @PathVariable UUID id) { return service.runDetail(userId(authentication), id); }
    @GetMapping("/bookmarks") public List<MyPageDtos.Bookmark> bookmarks(Authentication authentication) { return service.bookmarks(userId(authentication)); }
    @DeleteMapping("/bookmarks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(Authentication authentication, @PathVariable UUID id) { service.deleteBookmark(userId(authentication), id); }
    @GetMapping("/reports")
    public List<MyPageDtos.RunTripReportSummary> reports(Authentication authentication) {
        return service.reports(userId(authentication));
    }

    @GetMapping("/reports/{id}")
    public MyPageDtos.RunTripReportDetail report(Authentication authentication, @PathVariable UUID id) {
        return service.report(userId(authentication), id);
    }

    @GetMapping("/reports/preview")
    public MyPageDtos.RunTripReportStatistics reportPreview(
            Authentication authentication,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return service.reportPreview(userId(authentication), startDate, endDate);
    }

    @GetMapping("/reports/statistics")
    public MyPageDtos.RunTripOverallStatistics reportStatistics(Authentication authentication) {
        return service.overallReportStatistics(userId(authentication));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public MyPageDtos.RunTripReportDetail createReport(
            Authentication authentication,
            @Valid @RequestBody MyPageDtos.SaveRunTripReportRequest request
    ) {
        return service.createReport(userId(authentication), request);
    }

    @PutMapping("/reports/{id}")
    public MyPageDtos.RunTripReportDetail updateReport(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody MyPageDtos.SaveRunTripReportRequest request
    ) {
        return service.updateReport(userId(authentication), id, request);
    }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(Authentication authentication, @PathVariable UUID id) {
        service.deleteReport(userId(authentication), id);
    }

    @PostMapping("/reports/image")
    public ImageUploadResponse uploadReportImage(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해주세요.");
        }
        try {
            return new ImageUploadResponse(fileStorageService.store(List.of(file)));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "대표 사진 업로드에 실패했습니다.");
        }
    }
}
