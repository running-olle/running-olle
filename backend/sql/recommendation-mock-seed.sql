-- Running Olle 추천 mock 시드
-- Target date context: 2026-08-31
--
-- 목적:
-- 1. 추천 정렬을 점검할 수 있도록 공개 제주 코스를 충분히 넣는다.
-- 2. 인증 기반 추천 검증을 위해 성향이 다른 온보딩 완료 사용자 3명을 넣는다.
-- 3. 코스 설명 기반 fallback / rerank 흐름을 확인할 수 있도록 추천 문서를 넣는다.
--
-- 주의:
-- - 이 스크립트는 재실행 가능하다.
-- - themes, user_types 같은 공용 lookup 테이블은 고정 ID를 가정하지 않고 code 기준 upsert로 처리한다.
-- - 테스트 사용자는 kakao_id 기준으로 다시 만들고, 매핑/문서/코스도 함께 다시 넣는다.
-- - pgvector embedding 데이터는 넣지 않는다. 앱 플로우로 embedding까지 확인하려면 시드 적재 후
--   수동 recommendation sync endpoint를 실행하면 된다.
BEGIN;

-- 1) 공용 lookup row 보정
INSERT INTO themes (id, code, name)
VALUES
    ('11000000-0000-0000-0000-000000000001', 'COAST', '해안'),
    ('11000000-0000-0000-0000-000000000002', 'FOREST', '숲길'),
    ('11000000-0000-0000-0000-000000000003', 'OREUM', '오름'),
    ('11000000-0000-0000-0000-000000000004', 'FOOD', '맛집'),
    ('11000000-0000-0000-0000-000000000005', 'PHOTO', '포토'),
    ('11000000-0000-0000-0000-000000000006', 'TRADITION', '전통'),
    ('11000000-0000-0000-0000-000000000007', 'URBAN', '도심')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO user_types (id, code, name)
VALUES
    ('12000000-0000-0000-0000-000000000001', 'ACTIVE_RUNNER', '활동형 러너'),
    ('12000000-0000-0000-0000-000000000002', 'RELAXED_TRAVELER', '여유형 여행자'),
    ('12000000-0000-0000-0000-000000000003', 'JEJU_RESIDENT', '제주 거주자')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name;

-- 2) 기존 테스트 사용자 매핑 삭제
DELETE FROM user_themes
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE kakao_id IN (
        'mock-kakao-active-runner',
        'mock-kakao-relaxed-traveler',
        'mock-kakao-jeju-resident'
    )
);

DELETE FROM user_user_types
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE kakao_id IN (
        'mock-kakao-active-runner',
        'mock-kakao-relaxed-traveler',
        'mock-kakao-jeju-resident'
    )
);

-- 3) 기존 테스트 코스 관련 데이터 삭제
DELETE FROM course_reviews
WHERE id IN (
    '70000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000002',
    '70000000-0000-0000-0000-000000000003',
    '70000000-0000-0000-0000-000000000004',
    '70000000-0000-0000-0000-000000000005',
    '70000000-0000-0000-0000-000000000006'
)
OR course_id IN (
    SELECT id
    FROM courses
    WHERE name IN (
        '애월 해안 노을 런',
        '함덕 포토 바람 루프',
        '성산 오름 임계 코스',
        '사려니 숲 회복 런',
        '제주시 야간 순환 코스',
        '한라산 자락 지구력 런',
        '올레시장 미식 산책 런',
        '모슬포 바람 템포 런',
        '구좌 일출 드리프트',
        '도심 인터벌 그리드',
        '오라 오름 파워 클라임',
        '서귀포 마을 전통 런'
    )
);

DELETE FROM running_records
WHERE id IN (
    '60000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000002',
    '60000000-0000-0000-0000-000000000003',
    '60000000-0000-0000-0000-000000000004',
    '60000000-0000-0000-0000-000000000005',
    '60000000-0000-0000-0000-000000000006'
)
OR course_id IN (
    SELECT id
    FROM courses
    WHERE name IN (
        '애월 해안 노을 런',
        '함덕 포토 바람 루프',
        '성산 오름 임계 코스',
        '사려니 숲 회복 런',
        '제주시 야간 순환 코스',
        '한라산 자락 지구력 런',
        '올레시장 미식 산책 런',
        '모슬포 바람 템포 런',
        '구좌 일출 드리프트',
        '도심 인터벌 그리드',
        '오라 오름 파워 클라임',
        '서귀포 마을 전통 런'
    )
);

DELETE FROM course_recommendation_documents
WHERE id IN (
    '50000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000006',
    '50000000-0000-0000-0000-000000000007',
    '50000000-0000-0000-0000-000000000008',
    '50000000-0000-0000-0000-000000000009',
    '50000000-0000-0000-0000-000000000010',
    '50000000-0000-0000-0000-000000000011',
    '50000000-0000-0000-0000-000000000012'
)
OR course_id IN (
    SELECT id
    FROM courses
    WHERE name IN (
        '애월 해안 노을 런',
        '함덕 포토 바람 루프',
        '성산 오름 임계 코스',
        '사려니 숲 회복 런',
        '제주시 야간 순환 코스',
        '한라산 자락 지구력 런',
        '올레시장 미식 산책 런',
        '모슬포 바람 템포 런',
        '구좌 일출 드리프트',
        '도심 인터벌 그리드',
        '오라 오름 파워 클라임',
        '서귀포 마을 전통 런'
    )
)
OR title IN (
    '애월 해안 노을 런',
    '함덕 포토 바람 루프',
    '성산 오름 임계 코스',
    '사려니 숲 회복 런',
    '제주시 야간 순환 코스',
    '한라산 자락 지구력 런',
    '올레시장 미식 산책 런',
    '모슬포 바람 템포 런',
    '구좌 일출 드리프트',
    '도심 인터벌 그리드',
    '오라 오름 파워 클라임',
    '서귀포 마을 전통 런'
);

DELETE FROM course_themes
WHERE id IN (
    '40000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000005',
    '40000000-0000-0000-0000-000000000006',
    '40000000-0000-0000-0000-000000000007',
    '40000000-0000-0000-0000-000000000008',
    '40000000-0000-0000-0000-000000000009',
    '40000000-0000-0000-0000-000000000010',
    '40000000-0000-0000-0000-000000000011',
    '40000000-0000-0000-0000-000000000012',
    '40000000-0000-0000-0000-000000000013',
    '40000000-0000-0000-0000-000000000014',
    '40000000-0000-0000-0000-000000000015',
    '40000000-0000-0000-0000-000000000016',
    '40000000-0000-0000-0000-000000000017',
    '40000000-0000-0000-0000-000000000018',
    '40000000-0000-0000-0000-000000000019',
    '40000000-0000-0000-0000-000000000020',
    '40000000-0000-0000-0000-000000000021',
    '40000000-0000-0000-0000-000000000022',
    '40000000-0000-0000-0000-000000000023',
    '40000000-0000-0000-0000-000000000024'
)
OR course_id IN (
    SELECT id
    FROM courses
    WHERE name IN (
        '애월 해안 노을 런',
        '함덕 포토 바람 루프',
        '성산 오름 임계 코스',
        '사려니 숲 회복 런',
        '제주시 야간 순환 코스',
        '한라산 자락 지구력 런',
        '올레시장 미식 산책 런',
        '모슬포 바람 템포 런',
        '구좌 일출 드리프트',
        '도심 인터벌 그리드',
        '오라 오름 파워 클라임',
        '서귀포 마을 전통 런'
    )
);

DELETE FROM courses
WHERE id IN (
    '30000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000005',
    '30000000-0000-0000-0000-000000000006',
    '30000000-0000-0000-0000-000000000007',
    '30000000-0000-0000-0000-000000000008',
    '30000000-0000-0000-0000-000000000009',
    '30000000-0000-0000-0000-000000000010',
    '30000000-0000-0000-0000-000000000011',
    '30000000-0000-0000-0000-000000000012'
)
OR name IN (
    '애월 해안 노을 런',
    '함덕 포토 바람 루프',
    '성산 오름 임계 코스',
    '사려니 숲 회복 런',
    '제주시 야간 순환 코스',
    '한라산 자락 지구력 런',
    '올레시장 미식 산책 런',
    '모슬포 바람 템포 런',
    '구좌 일출 드리프트',
    '도심 인터벌 그리드',
    '오라 오름 파워 클라임',
    '서귀포 마을 전통 런'
);

-- 4) 테스트 사용자 재생성
DELETE FROM users
WHERE kakao_id IN (
    'mock-kakao-active-runner',
    'mock-kakao-relaxed-traveler',
    'mock-kakao-jeju-resident'
);

INSERT INTO users (
    id,
    kakao_id,
    nickname,
    profile_image_url,
    bio,
    preferred_distance,
    preferred_difficulty,
    terms_service_agreed,
    terms_privacy_agreed,
    terms_location_agreed,
    terms_marketing_agreed_at,
    role,
    account_status,
    withdrawn_at,
    is_deleted,
    deleted_at,
    onboarding_completed,
    created_at,
    updated_at
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'mock-kakao-active-runner',
        '활동형러너',
        null,
        '장거리와 오름 구간을 선호하는 러너.',
        'OVER_10KM',
        'HARD',
        true,
        true,
        true,
        '2026-08-01 09:00:00',
        'USER',
        'ACTIVE',
        null,
        false,
        null,
        true,
        '2026-08-01 09:00:00',
        '2026-08-31 10:00:00'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'mock-kakao-relaxed-traveler',
        '여유형여행자',
        null,
        '풍경과 사진, 무리 없는 제주 러닝을 선호한다.',
        'FROM_5_TO_10KM',
        'EASY',
        true,
        true,
        true,
        '2026-08-02 09:00:00',
        'USER',
        'ACTIVE',
        null,
        false,
        null,
        true,
        '2026-08-02 09:00:00',
        '2026-08-31 10:00:00'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'mock-kakao-jeju-resident',
        '제주생활러너',
        null,
        '도심 근처에서 반복 가능한 평일 러닝이 필요하다.',
        'FROM_5_TO_10KM',
        'NORMAL',
        true,
        true,
        true,
        '2026-08-03 09:00:00',
        'USER',
        'ACTIVE',
        null,
        false,
        null,
        true,
        '2026-08-03 09:00:00',
        '2026-08-31 10:00:00'
    );

INSERT INTO users (
    id,
    kakao_id,
    nickname,
    profile_image_url,
    bio,
    preferred_distance,
    preferred_difficulty,
    terms_service_agreed,
    terms_privacy_agreed,
    terms_location_agreed,
    terms_marketing_agreed_at,
    role,
    account_status,
    withdrawn_at,
    is_deleted,
    deleted_at,
    onboarding_completed,
    created_at,
    updated_at
)
VALUES (
    '10000000-0000-0000-0000-000000000010',
    'mock-kakao-course-creator',
    '코스시더',
    null,
    '추천용 코스 데이터를 넣는 계정.',
    'FROM_5_TO_10KM',
    'NORMAL',
    true,
    true,
    true,
    null,
    'USER',
    'ACTIVE',
    null,
    false,
    null,
    true,
    '2026-07-01 09:00:00',
    '2026-08-31 10:00:00'
)
ON CONFLICT (kakao_id) DO UPDATE
SET
    nickname = EXCLUDED.nickname,
    profile_image_url = EXCLUDED.profile_image_url,
    bio = EXCLUDED.bio,
    preferred_distance = EXCLUDED.preferred_distance,
    preferred_difficulty = EXCLUDED.preferred_difficulty,
    terms_service_agreed = EXCLUDED.terms_service_agreed,
    terms_privacy_agreed = EXCLUDED.terms_privacy_agreed,
    terms_location_agreed = EXCLUDED.terms_location_agreed,
    terms_marketing_agreed_at = EXCLUDED.terms_marketing_agreed_at,
    role = EXCLUDED.role,
    account_status = EXCLUDED.account_status,
    withdrawn_at = EXCLUDED.withdrawn_at,
    is_deleted = EXCLUDED.is_deleted,
    deleted_at = EXCLUDED.deleted_at,
    onboarding_completed = EXCLUDED.onboarding_completed,
    updated_at = EXCLUDED.updated_at;

-- 5) 사용자 선호 매핑 재생성
INSERT INTO user_user_types (id, user_id, user_type_id)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        (SELECT id FROM user_types WHERE code = 'ACTIVE_RUNNER')
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        (SELECT id FROM user_types WHERE code = 'RELAXED_TRAVELER')
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000003',
        (SELECT id FROM user_types WHERE code = 'JEJU_RESIDENT')
    );

INSERT INTO user_themes (id, user_id, theme_id)
VALUES
    (
        '21000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        (SELECT id FROM themes WHERE code = 'OREUM')
    ),
    (
        '21000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        (SELECT id FROM themes WHERE code = 'FOREST')
    ),
    (
        '21000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000002',
        (SELECT id FROM themes WHERE code = 'COAST')
    ),
    (
        '21000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000002',
        (SELECT id FROM themes WHERE code = 'PHOTO')
    ),
    (
        '21000000-0000-0000-0000-000000000005',
        '10000000-0000-0000-0000-000000000003',
        (SELECT id FROM themes WHERE code = 'URBAN')
    ),
    (
        '21000000-0000-0000-0000-000000000006',
        '10000000-0000-0000-0000-000000000003',
        (SELECT id FROM themes WHERE code = 'FOREST')
    );

-- 6) 코스 시드 데이터
INSERT INTO courses (
    id,
    creator_id,
    name,
    description,
    course_type,
    distance_km,
    estimated_duration_minutes,
    elevation_gain_m,
    difficulty,
    surface_asphalt_pct,
    surface_dirt_pct,
    surface_stairs_pct,
    route,
    start_point,
    gpx_file_url,
    thumbnail_image_url,
    is_public,
    rating_avg,
    completion_count,
    is_deleted,
    deleted_at,
    created_at,
    updated_at
)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '애월 해안 노을 런',
        '탁 트인 바다 전망과 노을, 카페 동선이 어우러진 평탄한 해안 코스. 여행하듯 가볍게 달리기 좋다.',
        'RUNNING_COURSE',
        6.40,
        42,
        18.00,
        'LOW',
        88.00,
        10.00,
        2.00,
        ST_GeomFromText('LINESTRING(126.3080 33.4620, 126.3155 33.4645)', 4326),
        ST_GeomFromText('POINT(126.3080 33.4620)', 4326),
        null,
        null,
        true,
        4.82,
        148,
        false,
        null,
        '2026-08-01 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '함덕 포토 바람 루프',
        '맑은 바다와 아침빛, 사진 포인트가 많은 짧은 해변 루프 코스. 여유롭게 즐기기 좋다.',
        'RUNNING_COURSE',
        5.10,
        33,
        12.00,
        'LOW',
        90.00,
        8.00,
        2.00,
        ST_GeomFromText('LINESTRING(126.6670 33.5430, 126.6735 33.5455)', 4326),
        ST_GeomFromText('POINT(126.6670 33.5430)', 4326),
        null,
        null,
        true,
        4.76,
        121,
        false,
        null,
        '2026-08-02 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '성산 오름 임계 코스',
        '짧지 않은 거리와 반복 오르막, 오름 조망, 바람 노출이 있는 코스로 강한 러너에게 맞다.',
        'RUNNING_COURSE',
        12.30,
        82,
        168.00,
        'HIGH',
        58.00,
        30.00,
        12.00,
        ST_GeomFromText('LINESTRING(126.9310 33.4580, 126.9420 33.4620)', 4326),
        ST_GeomFromText('POINT(126.9310 33.4580)', 4326),
        null,
        null,
        true,
        4.68,
        93,
        false,
        null,
        '2026-08-03 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000004',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '사려니 숲 회복 런',
        '그늘이 많은 숲길과 한적한 분위기로 회복주나 반복 러닝에 적합한 코스다.',
        'RUNNING_COURSE',
        8.40,
        55,
        54.00,
        'MID',
        42.00,
        56.00,
        2.00,
        ST_GeomFromText('LINESTRING(126.6680 33.4220, 126.6765 33.4255)', 4326),
        ST_GeomFromText('POINT(126.6680 33.4220)', 4326),
        null,
        null,
        true,
        4.74,
        135,
        false,
        null,
        '2026-08-04 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000005',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '제주시 야간 순환 코스',
        '도심 야간 조명과 편의시설 접근성이 좋고 반복 방문이 쉬운 저녁 러닝 코스다.',
        'RUNNING_COURSE',
        7.20,
        46,
        22.00,
        'MID',
        95.00,
        3.00,
        2.00,
        ST_GeomFromText('LINESTRING(126.5312 33.4996, 126.5390 33.5035)', 4326),
        ST_GeomFromText('POINT(126.5312 33.4996)', 4326),
        null,
        null,
        true,
        4.51,
        164,
        false,
        null,
        '2026-08-05 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000006',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '한라산 자락 지구력 런',
        '오르막 비중이 높고 상승 고도가 커서 강도 있는 훈련을 준비하는 러너에게 적합하다.',
        'RUNNING_COURSE',
        14.80,
        104,
        242.00,
        'HIGH',
        50.00,
        35.00,
        15.00,
        ST_GeomFromText('LINESTRING(126.4910 33.3610, 126.5000 33.3670)', 4326),
        ST_GeomFromText('POINT(126.4910 33.3610)', 4326),
        null,
        null,
        true,
        4.62,
        89,
        false,
        null,
        '2026-08-06 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000007',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '올레시장 미식 산책 런',
        '시장 먹거리와 골목 동선을 가볍게 잇는 짧은 도심 코스. 러닝과 관광을 함께 즐기기 좋다.',
        'SPOT_COURSE',
        3.10,
        24,
        8.00,
        'LOW',
        98.00,
        1.00,
        1.00,
        ST_GeomFromText('LINESTRING(126.5205 33.5125, 126.5245 33.5145)', 4326),
        ST_GeomFromText('POINT(126.5205 33.5125)', 4326),
        null,
        null,
        true,
        4.33,
        57,
        false,
        null,
        '2026-08-07 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000008',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '모슬포 바람 템포 런',
        '바람을 정면으로 받는 해안 구간과 일정한 페이스 유지에 적합한 중거리 코스다.',
        'RUNNING_COURSE',
        9.60,
        64,
        34.00,
        'MID',
        84.00,
        12.00,
        4.00,
        ST_GeomFromText('LINESTRING(126.2490 33.2140, 126.2580 33.2180)', 4326),
        ST_GeomFromText('POINT(126.2490 33.2140)', 4326),
        null,
        null,
        true,
        4.21,
        41,
        false,
        null,
        '2026-08-08 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000009',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '구좌 일출 드리프트',
        '조용한 해안 로컬 도로를 따라 일출과 사진을 즐기기 좋은 쉬운 아침 러닝 코스다.',
        'RUNNING_COURSE',
        6.80,
        44,
        16.00,
        'LOW',
        86.00,
        10.00,
        4.00,
        ST_GeomFromText('LINESTRING(126.8380 33.5560, 126.8460 33.5590)', 4326),
        ST_GeomFromText('POINT(126.8380 33.5560)', 4326),
        null,
        null,
        true,
        4.71,
        118,
        false,
        null,
        '2026-08-09 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000010',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '도심 인터벌 그리드',
        '직장과 주거 지역 가까운 도심 블록 중심 코스로 짧은 인터벌과 평일 반복 러닝에 적합하다.',
        'RUNNING_COURSE',
        4.60,
        29,
        14.00,
        'MID',
        99.00,
        1.00,
        0.00,
        ST_GeomFromText('LINESTRING(126.5230 33.5005, 126.5285 33.5030)', 4326),
        ST_GeomFromText('POINT(126.5230 33.5005)', 4326),
        null,
        null,
        true,
        4.48,
        132,
        false,
        null,
        '2026-08-10 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000011',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '오라 오름 파워 클라임',
        '가파른 오름 구간과 반복 상승이 포함된 코스로 하체 힘과 지구력을 요구한다.',
        'RUNNING_COURSE',
        10.90,
        78,
        196.00,
        'HIGH',
        46.00,
        41.00,
        13.00,
        ST_GeomFromText('LINESTRING(126.5150 33.4550, 126.5225 33.4600)', 4326),
        ST_GeomFromText('POINT(126.5150 33.4550)', 4326),
        null,
        null,
        true,
        4.57,
        76,
        false,
        null,
        '2026-08-11 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '30000000-0000-0000-0000-000000000012',
        (SELECT id FROM users WHERE kakao_id = 'mock-kakao-course-creator'),
        '서귀포 마을 전통 런',
        '돌담길과 마을 풍경을 따라가는 완만한 코스로 적당한 거리의 제주 분위기를 느끼기 좋다.',
        'RUNNING_COURSE',
        7.90,
        52,
        38.00,
        'LOW',
        80.00,
        16.00,
        4.00,
        ST_GeomFromText('LINESTRING(126.5600 33.2490, 126.5670 33.2520)', 4326),
        ST_GeomFromText('POINT(126.5600 33.2490)', 4326),
        null,
        null,
        true,
        4.44,
        52,
        false,
        null,
        '2026-08-12 08:00:00',
        '2026-08-31 08:00:00'
    );

-- 7) 코스 테마 연결
INSERT INTO course_themes (id, course_id, theme_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', (SELECT id FROM themes WHERE code = 'COAST')),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', (SELECT id FROM themes WHERE code = 'PHOTO')),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', (SELECT id FROM themes WHERE code = 'COAST')),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', (SELECT id FROM themes WHERE code = 'PHOTO')),
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000003', (SELECT id FROM themes WHERE code = 'OREUM')),
    ('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000003', (SELECT id FROM themes WHERE code = 'PHOTO')),
    ('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000004', (SELECT id FROM themes WHERE code = 'FOREST')),
    ('40000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000004', (SELECT id FROM themes WHERE code = 'PHOTO')),
    ('40000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000005', (SELECT id FROM themes WHERE code = 'URBAN')),
    ('40000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000006', (SELECT id FROM themes WHERE code = 'OREUM')),
    ('40000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000006', (SELECT id FROM themes WHERE code = 'FOREST')),
    ('40000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000007', (SELECT id FROM themes WHERE code = 'FOOD')),
    ('40000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000007', (SELECT id FROM themes WHERE code = 'URBAN')),
    ('40000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000008', (SELECT id FROM themes WHERE code = 'COAST')),
    ('40000000-0000-0000-0000-000000000015', '30000000-0000-0000-0000-000000000009', (SELECT id FROM themes WHERE code = 'COAST')),
    ('40000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000009', (SELECT id FROM themes WHERE code = 'PHOTO')),
    ('40000000-0000-0000-0000-000000000017', '30000000-0000-0000-0000-000000000010', (SELECT id FROM themes WHERE code = 'URBAN')),
    ('40000000-0000-0000-0000-000000000018', '30000000-0000-0000-0000-000000000010', (SELECT id FROM themes WHERE code = 'FOREST')),
    ('40000000-0000-0000-0000-000000000019', '30000000-0000-0000-0000-000000000011', (SELECT id FROM themes WHERE code = 'OREUM')),
    ('40000000-0000-0000-0000-000000000020', '30000000-0000-0000-0000-000000000011', (SELECT id FROM themes WHERE code = 'FOREST')),
    ('40000000-0000-0000-0000-000000000021', '30000000-0000-0000-0000-000000000012', (SELECT id FROM themes WHERE code = 'TRADITION')),
    ('40000000-0000-0000-0000-000000000022', '30000000-0000-0000-0000-000000000012', (SELECT id FROM themes WHERE code = 'URBAN')),
    ('40000000-0000-0000-0000-000000000023', '30000000-0000-0000-0000-000000000005', (SELECT id FROM themes WHERE code = 'FOREST')),
    ('40000000-0000-0000-0000-000000000024', '30000000-0000-0000-0000-000000000008', (SELECT id FROM themes WHERE code = 'URBAN'));

-- 8) 후기 RAG 검증용 러닝 기록/후기 시드
INSERT INTO running_records (
    id,
    user_id,
    course_id,
    trip_id,
    running_mode,
    route,
    total_distance_km,
    total_duration_seconds,
    avg_pace,
    calories,
    elevation_gain_m,
    started_at,
    ended_at,
    created_at
)
VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000001',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.3080 33.4620, 126.3155 33.4645)', 4326),
        6.40,
        2460,
        6.40,
        390.00,
        18.00,
        '2026-08-21 18:20:00',
        '2026-08-21 19:01:00',
        '2026-08-21 19:05:00'
    ),
    (
        '60000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.6670 33.5430, 126.6735 33.5455)', 4326),
        5.10,
        1980,
        6.47,
        310.00,
        12.00,
        '2026-08-22 07:30:00',
        '2026-08-22 08:03:00',
        '2026-08-22 08:05:00'
    ),
    (
        '60000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000003',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.9310 33.4580, 126.9420 33.4620)', 4326),
        12.30,
        4920,
        6.67,
        760.00,
        168.00,
        '2026-08-23 06:10:00',
        '2026-08-23 07:32:00',
        '2026-08-23 07:40:00'
    ),
    (
        '60000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000004',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.6680 33.4220, 126.6765 33.4255)', 4326),
        8.40,
        3300,
        6.55,
        510.00,
        54.00,
        '2026-08-24 09:00:00',
        '2026-08-24 09:55:00',
        '2026-08-24 10:00:00'
    ),
    (
        '60000000-0000-0000-0000-000000000005',
        '10000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000005',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.5312 33.4996, 126.5390 33.5035)', 4326),
        7.20,
        2760,
        6.39,
        430.00,
        22.00,
        '2026-08-25 20:10:00',
        '2026-08-25 20:56:00',
        '2026-08-25 21:00:00'
    ),
    (
        '60000000-0000-0000-0000-000000000006',
        '10000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000006',
        null,
        'COURSE_SELECT',
        ST_GeomFromText('LINESTRING(126.4910 33.3610, 126.5000 33.3670)', 4326),
        14.80,
        6240,
        7.03,
        910.00,
        242.00,
        '2026-08-26 06:00:00',
        '2026-08-26 07:44:00',
        '2026-08-26 07:50:00'
    );

INSERT INTO course_reviews (
    id,
    user_id,
    course_id,
    running_record_id,
    rating,
    content,
    created_at,
    updated_at
)
VALUES
    (
        '70000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000001',
        '60000000-0000-0000-0000-000000000001',
        5,
        '노을 시간대 바다 전망이 좋고 사진 찍기 좋은 포인트가 많았다. 평탄해서 여행 중 가볍게 뛰기 좋았다.',
        '2026-08-21 19:10:00',
        '2026-08-21 19:10:00'
    ),
    (
        '70000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        '60000000-0000-0000-0000-000000000002',
        5,
        '아침 바람은 조금 있었지만 해변 루프가 짧고 선명해서 초행자도 부담이 적었다. 포토 스팟 만족도가 높다.',
        '2026-08-22 08:10:00',
        '2026-08-22 08:10:00'
    ),
    (
        '70000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000003',
        '60000000-0000-0000-0000-000000000003',
        4,
        '반복 오르막이 꽤 강하고 바람 노출이 있다. 장거리 훈련 목적이면 만족스럽지만 쉬운 여행 러닝으로는 부담스럽다.',
        '2026-08-23 07:45:00',
        '2026-08-23 07:45:00'
    ),
    (
        '70000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000004',
        '60000000-0000-0000-0000-000000000004',
        5,
        '그늘이 많고 숲길 리듬이 좋아서 회복주로 만족스러웠다. 반복해서 뛰어도 질리지 않는 생활형 코스다.',
        '2026-08-24 10:05:00',
        '2026-08-24 10:05:00'
    ),
    (
        '70000000-0000-0000-0000-000000000005',
        '10000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000005',
        '60000000-0000-0000-0000-000000000005',
        4,
        '도심 접근성과 편의시설이 좋아 평일 저녁에 반복하기 편했다. 일부 교차로는 신호 대기가 있어 페이스가 끊긴다.',
        '2026-08-25 21:05:00',
        '2026-08-25 21:05:00'
    ),
    (
        '70000000-0000-0000-0000-000000000006',
        '10000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000006',
        '60000000-0000-0000-0000-000000000006',
        4,
        '상승 고도가 커서 지구력 훈련에는 좋지만 초반부터 강도가 높다. 더운 날에는 보급과 페이스 조절이 필요하다.',
        '2026-08-26 07:55:00',
        '2026-08-26 07:55:00'
    );

-- 9) 추천 문서 시드
INSERT INTO course_recommendation_documents (
    id,
    course_id,
    source_type,
    source_key,
    title,
    content,
    metadata,
    embedding_model,
    embedding_status,
    embedded_at,
    embedding_failure_reason,
    is_deleted,
    deleted_at,
    created_at,
    updated_at
)
VALUES
    (
        '50000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        'COURSE_DESCRIPTION',
        'course-description',
        '애월 해안 노을 런',
        '탁 트인 바다 전망과 노을, 카페 동선이 어우러진 평탄한 해안 코스. 여행하듯 가볍게 달리기 좋다.',
        '{"courseId":"30000000-0000-0000-0000-000000000001","courseName":"애월 해안 노을 런","type":"RUNNING_COURSE","themeCodes":["COAST","PHOTO"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        'COURSE_DESCRIPTION',
        'course-description',
        '함덕 포토 바람 루프',
        '맑은 바다와 아침빛, 사진 포인트가 많은 짧은 해변 루프 코스. 여유롭게 즐기기 좋다.',
        '{"courseId":"30000000-0000-0000-0000-000000000002","courseName":"함덕 포토 바람 루프","type":"RUNNING_COURSE","themeCodes":["COAST","PHOTO"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000003',
        'COURSE_DESCRIPTION',
        'course-description',
        '성산 오름 임계 코스',
        '짧지 않은 거리와 반복 오르막, 오름 조망, 바람 노출이 있는 코스로 강한 러너에게 맞다.',
        '{"courseId":"30000000-0000-0000-0000-000000000003","courseName":"성산 오름 임계 코스","type":"RUNNING_COURSE","themeCodes":["OREUM","PHOTO"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000004',
        '30000000-0000-0000-0000-000000000004',
        'COURSE_DESCRIPTION',
        'course-description',
        '사려니 숲 회복 런',
        '그늘이 많은 숲길과 한적한 분위기로 회복주나 반복 러닝에 적합한 코스다.',
        '{"courseId":"30000000-0000-0000-0000-000000000004","courseName":"사려니 숲 회복 런","type":"RUNNING_COURSE","themeCodes":["FOREST","PHOTO"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000005',
        '30000000-0000-0000-0000-000000000005',
        'COURSE_DESCRIPTION',
        'course-description',
        '제주시 야간 순환 코스',
        '도심 야간 조명과 편의시설 접근성이 좋고 반복 방문이 쉬운 저녁 러닝 코스다.',
        '{"courseId":"30000000-0000-0000-0000-000000000005","courseName":"제주시 야간 순환 코스","type":"RUNNING_COURSE","themeCodes":["URBAN","FOREST"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000006',
        '30000000-0000-0000-0000-000000000006',
        'COURSE_DESCRIPTION',
        'course-description',
        '한라산 자락 지구력 런',
        '오르막 비중이 높고 상승 고도가 커서 강도 있는 훈련을 준비하는 러너에게 적합하다.',
        '{"courseId":"30000000-0000-0000-0000-000000000006","courseName":"한라산 자락 지구력 런","type":"RUNNING_COURSE","themeCodes":["OREUM","FOREST"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000007',
        '30000000-0000-0000-0000-000000000007',
        'COURSE_DESCRIPTION',
        'course-description',
        '올레시장 미식 산책 런',
        '시장 먹거리와 골목 동선을 가볍게 잇는 짧은 도심 코스. 러닝과 관광을 함께 즐기기 좋다.',
        '{"courseId":"30000000-0000-0000-0000-000000000007","courseName":"올레시장 미식 산책 런","type":"SPOT_COURSE","themeCodes":["FOOD","URBAN"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000008',
        '30000000-0000-0000-0000-000000000008',
        'COURSE_DESCRIPTION',
        'course-description',
        '모슬포 바람 템포 런',
        '바람을 정면으로 받는 해안 구간과 일정한 페이스 유지에 적합한 중거리 코스다.',
        '{"courseId":"30000000-0000-0000-0000-000000000008","courseName":"모슬포 바람 템포 런","type":"RUNNING_COURSE","themeCodes":["COAST","URBAN"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000009',
        '30000000-0000-0000-0000-000000000009',
        'COURSE_DESCRIPTION',
        'course-description',
        '구좌 일출 드리프트',
        '조용한 해안 로컬 도로를 따라 일출과 사진을 즐기기 좋은 쉬운 아침 러닝 코스다.',
        '{"courseId":"30000000-0000-0000-0000-000000000009","courseName":"구좌 일출 드리프트","type":"RUNNING_COURSE","themeCodes":["COAST","PHOTO"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000010',
        '30000000-0000-0000-0000-000000000010',
        'COURSE_DESCRIPTION',
        'course-description',
        '도심 인터벌 그리드',
        '직장과 주거 지역 가까운 도심 블록 중심 코스로 짧은 인터벌과 평일 반복 러닝에 적합하다.',
        '{"courseId":"30000000-0000-0000-0000-000000000010","courseName":"도심 인터벌 그리드","type":"RUNNING_COURSE","themeCodes":["URBAN","FOREST"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000011',
        '30000000-0000-0000-0000-000000000011',
        'COURSE_DESCRIPTION',
        'course-description',
        '오라 오름 파워 클라임',
        '가파른 오름 구간과 반복 상승이 포함된 코스로 하체 힘과 지구력을 요구한다.',
        '{"courseId":"30000000-0000-0000-0000-000000000011","courseName":"오라 오름 파워 클라임","type":"RUNNING_COURSE","themeCodes":["OREUM","FOREST"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    ),
    (
        '50000000-0000-0000-0000-000000000012',
        '30000000-0000-0000-0000-000000000012',
        'COURSE_DESCRIPTION',
        'course-description',
        '서귀포 마을 전통 런',
        '돌담길과 마을 풍경을 따라가는 완만한 코스로 적당한 거리의 제주 분위기를 느끼기 좋다.',
        '{"courseId":"30000000-0000-0000-0000-000000000012","courseName":"서귀포 마을 전통 런","type":"RUNNING_COURSE","themeCodes":["TRADITION","URBAN"]}'::jsonb,
        null,
        'PENDING',
        null,
        null,
        false,
        null,
        '2026-08-20 08:00:00',
        '2026-08-31 08:00:00'
    );

COMMIT;
