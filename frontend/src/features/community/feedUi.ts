import type { FeedPost } from "./api";

export function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = seconds % 60;
  return `${minutes}:${String(remainSeconds).padStart(2, "0")}`;
}

export function formatPace(distanceKm: number, durationSeconds: number) {
  if (!distanceKm) {
    return "페이스 정보 없음";
  }

  const paceSeconds = Math.round(durationSeconds / distanceKm);
  const paceMinutes = Math.floor(paceSeconds / 60);
  const remainSeconds = paceSeconds % 60;
  return `페이스 ${paceMinutes}'${String(remainSeconds).padStart(2, "0")}"`;
}

function parseApiDate(value: string) {
  const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/.test(value);

  // 백엔드 LocalDateTime은 UTC이지만 시간대 표시가 없으므로 Z를 붙인다.
  return new Date(hasTimezone ? value : `${value}Z`);
}

export function formatRelativeTime(value: string) {
  const date = parseApiDate(value);
  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.max(0, Math.floor(diffMs / 60000));

  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;

  const diffHours = Math.floor(diffMinutes / 60);

  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffHours < 48) return "어제";

  return `${Math.floor(diffHours / 24)}일 전`;
}

export function formatFullDate(value: string) {
  const date = parseApiDate(value);

  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    month: "numeric",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

export function buildImageGridClass(imageCount: number) {
  if (imageCount === 1) return "grid-cols-1";
  if (imageCount === 2) return "grid-cols-2";
  return "grid-cols-3";
}

export function buildAvatarGradient(post: FeedPost) {
  if (post.course?.courseType === "SPOT_COURSE") {
    return "linear-gradient(135deg,#34C759,#86EFAC)";
  }
  if (post.photoTagged) {
    return "linear-gradient(135deg,#3B82F6,#93C5FD)";
  }
  return "linear-gradient(135deg,#FF6F0F,#FF954E)";
}
