-- Canonicalize Running Olle themes and merge legacy lowercase meetup themes.
-- Run once against an existing database before deploying the unified ThemeCode release.
BEGIN;

INSERT INTO themes (id, code, name)
VALUES
    (gen_random_uuid(), 'COAST', '해안'),
    (gen_random_uuid(), 'FOREST', '숲길'),
    (gen_random_uuid(), 'OREUM', '오름'),
    (gen_random_uuid(), 'FOOD', '맛집'),
    (gen_random_uuid(), 'PHOTO', '포토'),
    (gen_random_uuid(), 'TRADITION', '전통'),
    (gen_random_uuid(), 'URBAN', '도심')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name;

DELETE FROM user_themes legacy_link
USING themes legacy_theme, themes canonical_theme, user_themes canonical_link
WHERE legacy_link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code)
  AND canonical_link.user_id = legacy_link.user_id
  AND canonical_link.theme_id = canonical_theme.id;

UPDATE user_themes link
SET theme_id = canonical_theme.id
FROM themes legacy_theme, themes canonical_theme
WHERE link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code);

DELETE FROM course_themes legacy_link
USING themes legacy_theme, themes canonical_theme, course_themes canonical_link
WHERE legacy_link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code)
  AND canonical_link.course_id = legacy_link.course_id
  AND canonical_link.theme_id = canonical_theme.id;

UPDATE course_themes link
SET theme_id = canonical_theme.id
FROM themes legacy_theme, themes canonical_theme
WHERE link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code);

DELETE FROM meetup_themes legacy_link
USING themes legacy_theme, themes canonical_theme, meetup_themes canonical_link
WHERE legacy_link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code)
  AND canonical_link.meetup_id = legacy_link.meetup_id
  AND canonical_link.theme_id = canonical_theme.id;

UPDATE meetup_themes link
SET theme_id = canonical_theme.id
FROM themes legacy_theme, themes canonical_theme
WHERE link.theme_id = legacy_theme.id
  AND legacy_theme.code <> UPPER(legacy_theme.code)
  AND canonical_theme.code = UPPER(legacy_theme.code);

DELETE FROM themes legacy_theme
WHERE legacy_theme.code <> UPPER(legacy_theme.code)
  AND UPPER(legacy_theme.code) IN ('COAST', 'FOREST', 'OREUM', 'FOOD', 'PHOTO', 'TRADITION', 'URBAN');

COMMIT;
