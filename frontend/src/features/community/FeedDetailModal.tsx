import { Card, Icon, Button } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
import { useEffect, useState } from 'react'
import { getFeedPost, type FeedPost } from './api'
import {
  createCommentWithPost,
  deleteCommentWithPost,
  deletePostById,
  toggleLikeWithOptimistic,
} from './feedPostMutations'
import { formatDuration, formatFullDate, formatPace, formatRelativeTime } from './feedUi'

export function FeedDetailModal({
  feedPostId,
  initialPost,
  onClose,
  onChange,
  onEdit,
  onOpenCourse,
}: {
  feedPostId: string
  initialPost: FeedPost
  onClose: () => void
  onChange: (post: FeedPost | null) => void
  onEdit: (post: FeedPost) => void
  onOpenCourse: (courseId: string) => void
}) {
  const [post, setPost] = useState<FeedPost>(initialPost)
  const [comment, setComment] = useState('')
  const [pending, setPending] = useState(false)

  useEffect(() => {
    setPost(initialPost)
  }, [initialPost])

  useEffect(() => {
    let active = true

    getFeedPost(feedPostId)
      .then((data) => {
        if (active) {
          setPost(data)
          onChange(data)
        }
      })
      .catch(() => {})

    return () => {
      active = false
    }
  }, [feedPostId])

  const syncPost = (nextPost: FeedPost | null) => {
    if (nextPost) {
      setPost(nextPost)
    }
    onChange(nextPost)
  }

  const handleLike = async () => {
    const current = { ...post }

    try {
      const { optimistic, confirmed } = await toggleLikeWithOptimistic(current)
      syncPost(optimistic)
      syncPost(confirmed)
    } catch {
      syncPost(current)
    }
  }

  const handleCommentSubmit = async () => {
    if (!comment.trim() || pending) {
      return
    }

    setPending(true)

    try {
      const nextPost = await createCommentWithPost(post, comment.trim())
      syncPost(nextPost)
      setComment('')
    } finally {
      setPending(false)
    }
  }

  const handleCommentKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey || !isDesktopBrowser()) {
      return
    }

    event.preventDefault()
    void handleCommentSubmit()
  }

  const handleDeleteComment = async (commentId: string) => {
    const current = { ...post }
    const optimistic = {
      ...post,
      commentCount: Math.max(0, post.commentCount - 1),
      comments: post.comments.filter((item) => item.id !== commentId),
    }

    syncPost(optimistic)

    try {
      const nextPost = await deleteCommentWithPost(current, commentId)
      syncPost(nextPost)
    } catch {
      syncPost(current)
    }
  }

  const handleDeletePost = async () => {
    if (!window.confirm('이 게시글을 삭제할까요?')) {
      return
    }

    try {
      await deletePostById(post.id)
      syncPost(null)
      onClose()
    } catch {
      window.alert('게시글을 삭제하지 못했습니다.')
    }
  }

  return (
    <div className="community-backdrop" onClick={onClose}>
      <FullScreenPage
        scroll={false} role="dialog" aria-modal="true" aria-label="피드 상세" className="community-dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="community-dialog-header">
          <Button variant="ghost" size="sm"
            type="button"
            onClick={onClose}
            className="flex h-11 w-11 items-center justify-center rounded-control bg-surface-subtle text-app-title text-ink"
            aria-label="닫기"
          >
            <Icon name="arrowLeft" />
          </Button>
          <div className="min-w-0">
            <div className="text-app-title font-bold text-ink">피드 상세</div>
            <div className="mt-0.5 text-caption text-ink-secondary">
              {post.nickname} · {post.region}
            </div>
          </div>
        </div>

        <div className="community-dialog-body flex-1 overflow-y-auto bg-surface-subtle px-5 py-4">
          <Card as="article" shadow="none" className="community-card">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-500 text-body-sm font-bold text-surface">
                  {post.nickname.slice(0, 1)}
                </div>
                <div>
                  <div className="text-body-sm font-bold text-ink">{post.nickname}</div>
                  <div className="mt-1 text-caption text-ink-secondary">{formatFullDate(post.createdAt)}</div>
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
              ) : null}
            </div>

            {post.runningRecord ? (
              <button
                type="button"
                onClick={() => {
                  if (post.course) {
                    onOpenCourse(post.course.id)
                  }
                }}
                disabled={!post.course}
                className={`mt-4 flex w-full items-center gap-3 rounded-control px-3 py-3 ${
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
                <div className="min-w-0 text-left">
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

            <div className="mt-4 whitespace-pre-wrap text-body-sm leading-relaxed text-ink">{post.content}</div>

            {post.imageUrls.length > 0 ? (
              <div className="mt-4 grid grid-cols-1 gap-2">
                {post.imageUrls.map((imageUrl, index) => (
                  <div
                    key={`${imageUrl}-${index}`}
                    className="aspect-[4/3] rounded-control bg-cover bg-center"
                    style={{ backgroundImage: `url(${imageUrl})` }}
                  />
                ))}
              </div>
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
              <span>댓글 {post.commentCount}</span>
              {post.course ? (
                <Button variant="ghost" size="sm"
                  type="button"
                  onClick={() => onOpenCourse(post.course!.id)}
                  className="ml-auto text-caption font-bold text-brand-500"
                >
                  {post.course.courseType === 'RUNNING_COURSE' ? '러닝 코스' : '스팟 코스'}
                </Button>
              ) : null}
            </div>
          </Card>

          <section className="mt-3 rounded-md bg-surface px-5 py-4 shadow-none">
            <div className="text-body-sm font-bold text-ink">댓글 {post.commentCount}</div>

            <div className="mt-4 flex gap-2">
              <textarea aria-label="댓글을 입력해 주세요."
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                onKeyDown={handleCommentKeyDown}
                placeholder="댓글을 입력해 주세요."
                rows={1}
                className="ui-textarea min-h-11 flex-1 resize-none rounded-lg border border-border-default bg-surface px-4 py-3 text-label leading-5 text-ink outline-none"
              />
              <Button variant="primary" size="sm"
                type="button"
                onClick={handleCommentSubmit}
                disabled={pending || !comment.trim()}
                className="h-11 rounded-full bg-brand-500 px-4 text-label font-bold text-surface disabled:opacity-40"
              >
                등록
              </Button>
            </div>

            <div className="mt-3 space-y-3">
              {post.comments.length === 0 ? (
                <div className="rounded-control bg-surface-subtle px-4 py-4 text-caption text-ink-tertiary">
                  아직 댓글이 없습니다.
                </div>
              ) : (
                post.comments.map((item) => (
                  <div key={item.id} className="rounded-control bg-surface-subtle px-4 py-3">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="text-caption font-bold text-ink">{item.nickname}</div>
                        <div className="mt-0.5 text-caption text-ink-secondary">{formatRelativeTime(item.createdAt)}</div>
                      </div>
                      {item.mine ? (
                        <Button variant="danger" size="sm"
                          type="button"
                          onClick={() => handleDeleteComment(item.id)}
                          className="text-caption font-bold text-ink-secondary"
                        >
                          삭제
                        </Button>
                      ) : null}
                    </div>
                    <div className="mt-1 whitespace-pre-wrap text-caption leading-5 text-ink-secondary">{item.content}</div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </FullScreenPage>
    </div>
  )
}

function isDesktopBrowser() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false
  }

  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}
