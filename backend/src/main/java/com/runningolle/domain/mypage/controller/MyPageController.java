package com.runningolle.domain.mypage.controller;

import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.mypage.service.MyPageService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
    @GetMapping public MyPageDtos.Dashboard dashboard(Authentication authentication) { return service.dashboard(userId(authentication)); }
    @GetMapping("/runs") public List<MyPageDtos.Run> runs(Authentication authentication) { return service.runs(userId(authentication)); }
    @GetMapping("/visits") public List<MyPageDtos.Visit> visits(Authentication authentication) { return service.visits(userId(authentication)); }
    @GetMapping("/runs/{id}") public MyPageDtos.RunDetail runDetail(Authentication authentication, @PathVariable UUID id) { return service.runDetail(userId(authentication), id); }
    @GetMapping("/bookmarks") public List<MyPageDtos.Bookmark> bookmarks(Authentication authentication) { return service.bookmarks(userId(authentication)); }
    @DeleteMapping("/bookmarks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(Authentication authentication, @PathVariable UUID id) { service.deleteBookmark(userId(authentication), id); }
    @GetMapping("/trips") public List<MyPageDtos.TripResponse> trips(Authentication authentication) { return service.trips(userId(authentication)); }
    @PostMapping("/trips") @ResponseStatus(HttpStatus.CREATED)
    public MyPageDtos.TripResponse createTrip(Authentication authentication, @RequestBody MyPageDtos.CreateTripRequest request) { return service.createTrip(userId(authentication), request); }
}
