package com.runningolle.domain.running.controller;

import com.runningolle.domain.course.dto.CourseCreateResponse;
import com.runningolle.domain.running.dto.CreateRunningRecordRequest;
import com.runningolle.domain.running.dto.CreateRunningRecordResponse;
import com.runningolle.domain.running.dto.SaveRunningCourseRequest;
import com.runningolle.domain.running.service.RunningCourseSaveService;
import com.runningolle.domain.running.service.RunningRecordService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/running-records")
@RequiredArgsConstructor
public class RunningRecordController {

    private final RunningRecordService runningRecordService;
    private final RunningCourseSaveService runningCourseSaveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRunningRecordResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateRunningRecordRequest request
    ) {
        UUID id = runningRecordService.createRecord(UUID.fromString(authentication.getName()), request);
        return new CreateRunningRecordResponse(id);
    }

    @PostMapping("/{recordId}/course")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseCreateResponse saveAsCourse(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody SaveRunningCourseRequest request
    ) {
        return runningCourseSaveService.saveAsCourse(
                UUID.fromString(authentication.getName()),
                recordId,
                request
        );
    }
}
