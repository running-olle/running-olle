package com.runningolle.domain.meetup.service;

import com.runningolle.domain.chat.entity.ChatRoom;
import com.runningolle.domain.chat.entity.ChatMessage;
import com.runningolle.domain.chat.entity.ChatRoomParticipant;
import com.runningolle.domain.chat.enums.ChatRoomType;
import com.runningolle.domain.chat.repository.ChatMessageRepository;
import com.runningolle.domain.chat.repository.ChatRoomParticipantRepository;
import com.runningolle.domain.chat.repository.ChatRoomRepository;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.meetup.dto.MeetupCourseSummaryResponse;
import com.runningolle.domain.meetup.dto.MeetupCreateRequest;
import com.runningolle.domain.meetup.dto.MeetupParticipantResponse;
import com.runningolle.domain.meetup.dto.MeetupParticipantStatsResponse;
import com.runningolle.domain.meetup.dto.MeetupResponse;
import com.runningolle.domain.meetup.entity.Meetup;
import com.runningolle.domain.meetup.entity.MeetupParticipant;
import com.runningolle.domain.meetup.entity.MeetupTheme;
import com.runningolle.domain.meetup.enums.MeetupStatus;
import com.runningolle.domain.meetup.enums.ParticipantStatus;
import com.runningolle.domain.meetup.repository.MeetupParticipantRepository;
import com.runningolle.domain.meetup.repository.MeetupRepository;
import com.runningolle.domain.meetup.repository.MeetupThemeRepository;
import com.runningolle.domain.notification.enums.NotificationType;
import com.runningolle.domain.notification.service.NotificationService;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MeetupService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final Map<String, String> THEME_LABELS = Map.of(
            "coast", "Coast",
            "forest", "Forest",
            "oreum", "Oreum",
            "photo", "Photo",
            "food", "Food"
    );

    private final MeetupRepository meetupRepository;
    private final MeetupParticipantRepository meetupParticipantRepository;
    private final MeetupThemeRepository meetupThemeRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ThemeRepository themeRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<MeetupResponse> getMeetups(UUID userId) {
        return meetupRepository.findByIsDeletedFalseOrderByMeetupDateAscCreatedAtDesc().stream()
                .map(meetup -> {
                    refreshMeetupStatus(meetup);
                    return toResponse(meetup, userId);
                })
                .toList();
    }

    @Transactional
    public MeetupResponse getMeetup(UUID userId, UUID meetupId) {
        Meetup meetup = getMeetupEntity(meetupId);
        refreshMeetupStatus(meetup);
        return toResponse(meetup, userId);
    }

    @Transactional
    public MeetupResponse createMeetup(UUID userId, MeetupCreateRequest request) {
        User organizer = getUser(userId);
        Course course = getCourse(request.courseId(), userId);
        Point meetingPoint = GEOMETRY_FACTORY.createPoint(
                new Coordinate(request.longitude().doubleValue(), request.latitude().doubleValue())
        );

        Meetup meetup = meetupRepository.save(Meetup.create(
                organizer,
                course,
                request.title().trim(),
                request.description().trim(),
                request.meetupDate(),
                request.maxParticipants(),
                request.targetPace(),
                request.meetingPlace().trim(),
                meetingPoint,
                request.joinMethod()
        ));

        meetupParticipantRepository.save(MeetupParticipant.create(meetup, organizer, ParticipantStatus.ACCEPTED));
        syncTheme(meetup, request.themeCode());
        ensureGroupChat(meetup, organizer);
        refreshMeetupStatus(meetup);
        return toResponse(meetup, userId);
    }

    @Transactional
    public MeetupResponse updateMeetup(UUID userId, UUID meetupId, MeetupCreateRequest request) {
        Meetup meetup = getMeetupEntity(meetupId);
        if (!meetup.getOrganizer().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can edit this meetup.");
        }

        refreshMeetupStatus(meetup);
        if (meetup.getStatus() == com.runningolle.domain.meetup.enums.MeetupStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed meetup cannot be edited.");
        }

        Course course = getCourse(request.courseId(), userId);
        Point meetingPoint = GEOMETRY_FACTORY.createPoint(
                new Coordinate(request.longitude().doubleValue(), request.latitude().doubleValue())
        );

        meetup.update(
                course,
                request.title().trim(),
                request.description().trim(),
                request.meetupDate(),
                request.maxParticipants(),
                request.targetPace(),
                request.meetingPlace().trim(),
                meetingPoint,
                request.joinMethod()
        );
        syncTheme(meetup, request.themeCode());
        meetupParticipantRepository.findByMeetupIdAndStatus(meetupId, ParticipantStatus.ACCEPTED).stream()
                .filter(participant -> !participant.getUser().getId().equals(userId))
                .forEach(participant -> notificationService.createMeetup(
                        participant.getUser(),
                        NotificationType.MEETUP_UPDATED,
                        "참여 중인 번개 일정이 변경됐어요",
                        "‘" + meetup.getTitle() + "’의 일정과 장소를 확인해 주세요.",
                        "MEETUP_UPDATED:" + meetupId + ":" + meetup.getUpdatedAt() + ":" + participant.getUser().getId()
                ));
        refreshMeetupStatus(meetup);
        return toResponse(meetup, userId);
    }

    @Transactional
    public MeetupResponse joinMeetup(UUID userId, UUID meetupId) {
        User user = getUser(userId);
        Meetup meetup = getMeetupEntity(meetupId);
        refreshMeetupStatus(meetup);

        if (meetup.getOrganizer().getId().equals(userId)) {
            return toResponse(meetup, userId);
        }

        if (meetup.getStatus() == com.runningolle.domain.meetup.enums.MeetupStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed meetup cannot be joined.");
        }
        if (meetup.getStatus() == com.runningolle.domain.meetup.enums.MeetupStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled meetup cannot be joined.");
        }

        long acceptedCount = meetupParticipantRepository.countByMeetupIdAndStatus(meetupId, ParticipantStatus.ACCEPTED);
        if (acceptedCount >= meetup.getMaxParticipants()) {
            refreshMeetupStatus(meetup);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is already full.");
        }

        ParticipantStatus status = meetup.getJoinMethod().name().equals("INSTANT")
                ? ParticipantStatus.ACCEPTED
                : ParticipantStatus.PENDING;

        MeetupParticipant participant = meetupParticipantRepository.findByMeetupIdAndUserId(meetupId, userId)
                .map(existing -> {
                    if (existing.getStatus() == ParticipantStatus.REJECTED) {
                        existing.reapply(status);
                        return existing;
                    }
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Already joined or requested this meetup.");
                })
                .orElseGet(() -> meetupParticipantRepository.save(MeetupParticipant.create(meetup, user, status)));

        if (status == ParticipantStatus.ACCEPTED) {
            ensureGroupChat(meetup, user);
        }

        notificationService.createMeetup(
                meetup.getOrganizer(),
                status == ParticipantStatus.PENDING ? NotificationType.MEETUP_JOIN_REQUEST : NotificationType.MEETUP_JOINED,
                status == ParticipantStatus.PENDING ? "새 번개 참여 요청이 있어요" : "새 러너가 번개에 참여했어요",
                user.getNickname() + "님이 ‘" + meetup.getTitle() + "’에 "
                        + (status == ParticipantStatus.PENDING ? "참여를 요청했습니다." : "참여했습니다."),
                "MEETUP_JOIN:" + meetupId + ":" + userId + ":" + participant.getRequestedAt()
        );

        refreshMeetupStatus(meetup);
        return toResponse(meetup, userId);
    }

    @Transactional
    public MeetupResponse respondToParticipant(UUID organizerId, UUID meetupId, UUID participantId, boolean accept) {
        Meetup meetup = getMeetupEntity(meetupId);
        if (!meetup.getOrganizer().getId().equals(organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can manage participants.");
        }
        refreshMeetupStatus(meetup);

        if (meetup.getStatus() == com.runningolle.domain.meetup.enums.MeetupStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed meetup cannot be updated.");
        }
        if (meetup.getStatus() == com.runningolle.domain.meetup.enums.MeetupStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled meetup cannot be updated.");
        }

        MeetupParticipant participant = meetupParticipantRepository.findByMeetupIdAndUserId(meetupId, participantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found."));

        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participant is no longer pending.");
        }

        if (accept) {
            long acceptedCount = meetupParticipantRepository.countByMeetupIdAndStatus(meetupId, ParticipantStatus.ACCEPTED);
            if (acceptedCount >= meetup.getMaxParticipants()) {
                refreshMeetupStatus(meetup);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is already full.");
            }
            participant.accept();
            ensureGroupChat(meetup, participant.getUser());
            appendGroupSystemMessage(
                    meetup,
                    meetup.getOrganizer(),
                    participant.getUser().getNickname() + "님이 번개에 참여했습니다."
            );
        } else {
            participant.reject();
            appendInquirySystemMessage(
                    meetup,
                    participant.getUser(),
                    meetup.getOrganizer(),
                    "번개 참여 요청이 거절되었습니다."
            );
        }

        notificationService.createMeetup(
                participant.getUser(),
                accept ? NotificationType.MEETUP_JOIN_ACCEPTED : NotificationType.MEETUP_JOIN_REJECTED,
                accept ? "번개 참여가 확정됐어요" : "번개 참여 요청 결과가 도착했어요",
                "‘" + meetup.getTitle() + "’ 참여 요청이 " + (accept ? "승인되었습니다." : "거절되었습니다."),
                "MEETUP_RESPONSE:" + meetupId + ":" + participantId + ":" + accept + ":" + participant.getRequestedAt()
        );

        refreshMeetupStatus(meetup);
        return toResponse(meetup, organizerId);
    }

    @Transactional
    public void deleteMeetup(UUID userId, UUID meetupId) {
        Meetup meetup = getMeetupEntity(meetupId);
        if (!meetup.getOrganizer().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can delete this meetup.");
        }
        meetupParticipantRepository.findByMeetupIdAndStatus(meetupId, ParticipantStatus.ACCEPTED).stream()
                .filter(participant -> !participant.getUser().getId().equals(userId))
                .forEach(participant -> notificationService.createMeetup(
                        participant.getUser(),
                        NotificationType.MEETUP_CANCELLED,
                        "참여 중인 번개가 취소됐어요",
                        "‘" + meetup.getTitle() + "’ 번개가 취소되었습니다.",
                        "MEETUP_CANCELLED:" + meetupId + ":" + participant.getUser().getId()
                ));
        appendGroupSystemMessage(meetup, meetup.getOrganizer(), "번개가 취소되었습니다.");
        meetup.delete();
    }

    private MeetupResponse toResponse(Meetup meetup, UUID currentUserId) {
        List<MeetupParticipant> allParticipants = meetupParticipantRepository.findByMeetupId(meetup.getId());
        List<UUID> acceptedParticipantIds = allParticipants.stream()
                .filter(participant -> participant.getStatus() == ParticipantStatus.ACCEPTED)
                .map(participant -> participant.getUser().getId())
                .toList();

        Map<UUID, MeetupParticipantStatsResponse> statsByUserId = buildStats(
                allParticipants.stream()
                        .map(participant -> participant.getUser().getId())
                        .filter(userId -> !userId.equals(meetup.getOrganizer().getId()))
                        .toList()
        );

        List<MeetupParticipantResponse> participantResponses = allParticipants.stream()
                .filter(participant -> !participant.getUser().getId().equals(meetup.getOrganizer().getId()))
                .sorted(Comparator.comparing(MeetupParticipant::getRequestedAt))
                .map(participant -> new MeetupParticipantResponse(
                        participant.getUser().getId(),
                        participant.getUser().getNickname(),
                        participant.getUser().getProfileImageUrl(),
                        participant.getStatus(),
                        statsByUserId.getOrDefault(
                                participant.getUser().getId(),
                                new MeetupParticipantStatsResponse(BigDecimal.ZERO, null, 0)
                        )
                ))
                .toList();

        ParticipantStatus myParticipation = allParticipants.stream()
                .filter(participant -> participant.getUser().getId().equals(currentUserId))
                .map(MeetupParticipant::getStatus)
                .findFirst()
                .orElse(null);

        MeetupCourseSummaryResponse courseResponse = meetup.getCourse() == null
                ? null
                : new MeetupCourseSummaryResponse(
                        meetup.getCourse().getId(),
                        meetup.getCourse().getName(),
                        meetup.getCourse().getDistanceKm(),
                        meetup.getCourse().getEstimatedDurationMinutes(),
                        meetup.getCourse().getDifficulty(),
                        meetup.getCourse().getCourseType()
                );

        MeetupTheme meetupTheme = meetupThemeRepository.findByMeetupId(meetup.getId()).stream().findFirst().orElse(null);
        String themeCode = meetupTheme == null ? null : meetupTheme.getTheme().getCode();
        String themeLabel = meetupTheme == null ? null : meetupTheme.getTheme().getName();

        return new MeetupResponse(
                meetup.getId(),
                meetup.getTitle(),
                meetup.getDescription(),
                meetup.getCreatedAt(),
                meetup.getOrganizer().getId(),
                meetup.getOrganizer().getNickname(),
                meetup.getOrganizer().getProfileImageUrl(),
                themeCode,
                themeLabel,
                meetup.getMeetupDate(),
                meetup.getMeetingPlace(),
                BigDecimal.valueOf(meetup.getMeetingPoint().getY()).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(meetup.getMeetingPoint().getX()).setScale(6, RoundingMode.HALF_UP),
                meetup.getMaxParticipants(),
                meetup.getTargetPace(),
                meetup.getJoinMethod(),
                meetup.getStatus(),
                courseResponse,
                acceptedParticipantIds,
                participantResponses,
                myParticipation,
                meetup.getOrganizer().getId().equals(currentUserId)
        );
    }

    private Map<UUID, MeetupParticipantStatsResponse> buildStats(List<UUID> userIds) {
        List<UUID> distinctUserIds = userIds.stream().distinct().toList();
        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, RunningRecordRepository.UserRunningStatsProjection> runningStatsByUserId =
                runningRecordRepository.aggregateStatsByUserIds(distinctUserIds).stream()
                        .collect(Collectors.toMap(
                                RunningRecordRepository.UserRunningStatsProjection::getUserId,
                                Function.identity()
                        ));

        Map<UUID, Long> meetupCountsByUserId =
                meetupParticipantRepository.countByUserIdsAndStatus(distinctUserIds, ParticipantStatus.ACCEPTED).stream()
                        .collect(Collectors.toMap(
                                MeetupParticipantRepository.UserMeetupCountProjection::getUserId,
                                MeetupParticipantRepository.UserMeetupCountProjection::getMeetupCount
                        ));

        return distinctUserIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userId -> {
                            RunningRecordRepository.UserRunningStatsProjection runningStats =
                                    runningStatsByUserId.get(userId);

                            BigDecimal totalDistance = runningStats == null || runningStats.getTotalDistanceKm() == null
                                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                                    : runningStats.getTotalDistanceKm().setScale(2, RoundingMode.HALF_UP);

                            Integer averagePaceSeconds =
                                    toAveragePaceSeconds(runningStats == null ? null : runningStats.getAveragePaceMinutes());

                            long meetupCount = meetupCountsByUserId.getOrDefault(userId, 0L);

                            return new MeetupParticipantStatsResponse(totalDistance, averagePaceSeconds, meetupCount);
                        }
                ));
    }

    private Integer toAveragePaceSeconds(BigDecimal averagePaceMinutes) {
        if (averagePaceMinutes == null) {
            return null;
        }

        return averagePaceMinutes
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private void syncTheme(Meetup meetup, String themeCode) {
        meetupThemeRepository.deleteByMeetupId(meetup.getId());
        if (themeCode == null || themeCode.isBlank()) {
            return;
        }

        Theme theme = themeRepository.findByCode(themeCode)
                .orElseGet(() -> themeRepository.save(
                        Theme.create(themeCode, THEME_LABELS.getOrDefault(themeCode, themeCode))
                ));
        meetupThemeRepository.save(MeetupTheme.create(meetup, theme));
    }

    private void ensureGroupChat(Meetup meetup, User user) {
        ChatRoom room = chatRoomRepository.findByMeetupIdAndRoomType(meetup.getId(), ChatRoomType.MEETUP_GROUP)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.createMeetupGroup(meetup)));

        chatRoomParticipantRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> chatRoomParticipantRepository.save(ChatRoomParticipant.create(room, user)));
    }

    private void refreshMeetupStatus(Meetup meetup) {
        MeetupStatus previousStatus = meetup.getStatus();
        long acceptedCount = meetupParticipantRepository.countByMeetupIdAndStatus(meetup.getId(), ParticipantStatus.ACCEPTED);
        meetup.refreshStatus(java.time.LocalDateTime.now(), (int) acceptedCount);
        if (previousStatus != meetup.getStatus() && meetup.getStatus() == MeetupStatus.COMPLETED) {
            appendGroupSystemMessage(meetup, meetup.getOrganizer(), "번개가 종료되었습니다.");
        }
    }

    private void appendGroupSystemMessage(Meetup meetup, User sender, String content) {
        chatRoomRepository.findByMeetupIdAndRoomType(meetup.getId(), ChatRoomType.MEETUP_GROUP)
                .ifPresent(room -> chatMessageRepository.save(ChatMessage.createSystem(room, sender, content)));
    }

    private void appendInquirySystemMessage(Meetup meetup, User inquirer, User organizer, String content) {
        ChatRoom room = chatRoomRepository
                .findByMeetupIdAndOrganizerIdAndInquirerIdAndRoomType(
                        meetup.getId(),
                        organizer.getId(),
                        inquirer.getId(),
                        ChatRoomType.DIRECT_INQUIRY
                )
                .orElseGet(() -> {
                    ChatRoom created = chatRoomRepository.save(ChatRoom.createDirectInquiry(meetup, organizer, inquirer));
                    chatRoomParticipantRepository.save(ChatRoomParticipant.create(created, organizer));
                    chatRoomParticipantRepository.save(ChatRoomParticipant.create(created, inquirer));
                    return created;
                });

        chatMessageRepository.save(ChatMessage.createSystem(room, organizer, content));
    }

    private Meetup getMeetupEntity(UUID meetupId) {
        return meetupRepository.findByIdAndIsDeletedFalse(meetupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found."));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private Course getCourse(UUID courseId, UUID userId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course not found."));
        if (!Boolean.TRUE.equals(course.getIsPublic()) && !course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course not found.");
        }
        return course;
    }
}
