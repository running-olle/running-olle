import { Card, Icon, Button } from '../../components/ui'
import { useMemo } from 'react'
import type { FeedPost } from './api'
import { deletePostById, toggleLikeWithOptimistic } from './feedPostMutations'
import {
  buildAvatarGradient,
  buildImageGridClass,
  formatDuration,
  formatPace,
  formatRelativeTime,
} from './feedUi'

type FeedPostCardProps = {
  post: FeedPost
  onChange: (post: FeedPost | null) => void
  onEdit: (post: FeedPost) => void
  onOpenDetail: (post: FeedPost) => void
  onOpenCourse: (courseId: string) => void
}

export function FeedPostCard({ post, onChange, onEdit, onOpenDetail, onOpenCourse }: FeedPostCardProps) {
  const createdLabel = useMemo(() => formatRelativeTime(post.createdAt), [post.createdAt])

  const handleLike = async () => {
    const current = { ...post }

    try {
      const { optimistic, confirmed } = await toggleLikeWithOptimistic(current)
      onChange(optimistic)
      onChange(confirmed)
    } catch {
      onChange(current)
    }
  }

  const handleDeletePost = async () => {
    if (!window.confirm('이 게시글을 삭제할까요?')) {
      return
    }

    try {
      await deletePostById(post.id)
      onChange(null)
    } catch {
      window.alert('게시글을 삭제하지 못했습니다.')
    }
  }

  return (
    <Card as="article" shadow="none" className="community-card">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <div
            className="flex h-10 w-10 items-center justify-center rounded-full text-body-sm font-bold text-surface"
            style={{ backgroundImage: buildAvatarGradient(post) }}
          >
            {post.nickname.slice(0, 1)}
          </div>
          <div className="min-w-0">
            <div className="truncate text-body-sm font-bold text-ink">{post.nickname}</div>
            <div className="mt-0.5 text-caption text-ink-secondary">
              {createdLabel} · {post.region}
            </div>
          </div>
        </div>

        {post.mine ? (
          <div className="flex items-center gap-3 text-caption font-bold text-ink-secondary">
            <Button variant="ghost" size="sm" type="button" onClick={() => onEdit(post)}>
              수정
            </Button>
            <Button variant="danger" size="sm" type="button" onClick={handleDeletePost}>
              삭제
            </Button>
          </div>
        ) : (
          <span className="text-app-title leading-none text-ink-tertiary">⋯</span>
        )}
      </div>

      {post.runningRecord ? (
        <button
          type="button"
          onClick={() => {
            if (post.course) {
              onOpenCourse(post.course.id)
              return
            }
            onOpenDetail(post)
          }}
          className={`mt-4 flex w-full items-center gap-3 rounded-control px-3 py-3 text-left ${
            post.course?.courseType === 'SPOT_COURSE' ? 'bg-surface-subtle' : 'bg-surface-subtle'
          }`}
        >
          <div
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-control text-card-title text-surface"
            style={{
              background: post.course?.courseType === 'SPOT_COURSE' ? 'var(--color-success)' : 'var(--color-brand-500)',
            }}
          >
            {post.course?.courseType === 'SPOT_COURSE' ? 'S' : 'R'}
          </div>
          <div className="min-w-0">
            <div className="truncate text-label font-bold text-ink">{post.course?.name ?? '러닝 기록'}</div>
            <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-caption text-ink-secondary">
              <span>거리 {post.runningRecord.distanceKm.toFixed(2)}km</span>
              <span>시간 {formatDuration(post.runningRecord.durationSeconds)}</span>
              <span>{formatPace(post.runningRecord.distanceKm, post.runningRecord.durationSeconds)}</span>
            </div>
          </div>
          {post.course ? <span className="ml-auto shrink-0 text-caption font-bold text-brand-500">코스 보기</span> : null}
        </button>
      ) : null}

      {!post.runningRecord && post.course ? (
        <button
          type="button"
          onClick={() => onOpenCourse(post.course!.id)}
          className={`mt-4 flex w-full items-center gap-3 rounded-control px-3 py-3 text-left ${
            post.course.courseType === 'SPOT_COURSE' ? 'bg-surface-subtle' : 'bg-surface-subtle'
          }`}
        >
          <div
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-control text-card-title text-surface"
            style={{
              background: post.course.courseType === 'SPOT_COURSE' ? 'var(--color-success)' : 'var(--color-brand-500)',
            }}
          >
            {post.course.courseType === 'SPOT_COURSE' ? 'S' : 'R'}
          </div>
          <div className="min-w-0">
            <div className="truncate text-label font-bold text-ink">{post.course.name}</div>
            <div className="mt-1 text-caption font-bold text-ink-secondary">
              {post.course.courseType === 'RUNNING_COURSE' ? '러닝 코스' : '스팟 코스'}
            </div>
          </div>
          <span className="ml-auto shrink-0 text-caption font-bold text-brand-500">코스 보기</span>
        </button>
      ) : null}

      <button type="button" onClick={() => onOpenDetail(post)} className="mt-4 block w-full text-left">
        <p className="whitespace-pre-wrap text-body-sm leading-relaxed text-ink">{post.content}</p>
      </button>

      {post.imageUrls.length > 0 ? (
        <button type="button" onClick={() => onOpenDetail(post)} className="mt-3 block w-full">
          <div className={`grid gap-1 overflow-hidden rounded-control ${buildImageGridClass(post.imageUrls.length)}`}>
            {post.imageUrls.slice(0, 3).map((imageUrl, index) => (
              <div
                key={`${imageUrl}-${index}`}
                className={`relative bg-cover bg-center ${post.imageUrls.length === 1 ? 'aspect-[4/3]' : 'aspect-square'}`}
                style={{ backgroundImage: `url(${imageUrl})` }}
              >
                {index === 2 && post.imageUrls.length > 3 ? (
                  <div className="absolute inset-0 flex items-center justify-center bg-(--color-overlay) text-app-title font-bold text-surface">
                    +{post.imageUrls.length - 3}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </button>
      ) : null}

      <div className="community-feed-actions">
        <Button variant="ghost" size="sm"
          type="button"
          onClick={handleLike}
          aria-label="좋아요" aria-pressed={post.likedByMe}
          className={`flex items-center gap-1 ${post.likedByMe ? 'text-danger' : ''}`}
        >
          <Icon name="heart" fill={post.likedByMe ? 'currentColor' : 'none'} />
          <span>{post.likeCount}</span>
        </Button>
        <Button variant="ghost" size="sm" type="button" onClick={() => onOpenDetail(post)} className="flex items-center gap-1">
          <span>댓글</span>
          <span>{post.commentCount}</span>
        </Button>
        {post.course ? (
          <Button variant="ghost" size="sm" type="button" onClick={() => onOpenCourse(post.course!.id)} className="ml-auto text-caption font-bold text-brand-500">
            태그 코스 보기
          </Button>
        ) : null}
      </div>
    </Card>
  )
}
