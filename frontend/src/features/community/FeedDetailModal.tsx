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
    <div className="fixed inset-0 z-30 bg-[rgba(38,25,18,0.45)]" onClick={onClose}>
      <div
        className="mx-auto flex h-dvh max-w-[430px] flex-col bg-[#FFF8F6]"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center gap-3 border-b border-[#E1BFB1] bg-[#FFF8F6] px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="flex h-[34px] w-[34px] items-center justify-center rounded-[10px] bg-[#F5F5F5] text-[17px] text-[#261912]"
            aria-label="닫기"
          >
            ←
          </button>
          <div className="min-w-0">
            <div className="text-[17px] font-bold text-[#261912]">피드 상세</div>
            <div className="mt-0.5 text-[11px] text-[#8D7164]">
              {post.nickname} · {post.region}
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto bg-[#F5F5F5] px-5 py-4">
          <article className="rounded-[18px] bg-white px-5 py-4 shadow-[0px_2px_12px_rgba(0,0,0,0.06)]">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[linear-gradient(135deg,#FF6F0F,#FF954E)] text-[14px] font-bold text-white">
                  {post.nickname.slice(0, 1)}
                </div>
                <div>
                  <div className="text-[14px] font-bold text-[#261912]">{post.nickname}</div>
                  <div className="mt-1 text-[11px] text-[#8D7164]">{formatFullDate(post.createdAt)}</div>
                </div>
              </div>

              {post.mine ? (
                <div className="flex items-center gap-3 text-[12px] font-bold text-[#8D7164]">
                  <button type="button" onClick={() => onEdit(post)}>
                    수정
                  </button>
                  <button type="button" onClick={handleDeletePost}>
                    삭제
                  </button>
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
                className={`mt-4 flex w-full items-center gap-3 rounded-[12px] px-3 py-3 ${
                  post.course?.courseType === 'SPOT_COURSE' ? 'bg-[#F0FDF4]' : 'bg-[#FFF5EE]'
                }`}
              >
                <div
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-[9px] text-[16px] text-white"
                  style={{
                    background: post.course?.courseType === 'SPOT_COURSE' ? '#34C759' : '#FF6F0F',
                  }}
                >
                  {post.course?.courseType === 'SPOT_COURSE' ? 'S' : 'R'}
                </div>
                <div className="min-w-0 text-left">
                  <div className="truncate text-[13px] font-bold text-[#261912]">{post.course?.name ?? '러닝 기록'}</div>
                  <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-[#594136]">
                    <span>거리 {post.runningRecord.distanceKm.toFixed(2)}km</span>
                    <span>시간 {formatDuration(post.runningRecord.durationSeconds)}</span>
                    <span>{formatPace(post.runningRecord.distanceKm, post.runningRecord.durationSeconds)}</span>
                  </div>
                </div>
                {post.course ? <span className="ml-auto shrink-0 text-[11px] font-bold text-[#FF6F0F]">코스 보기</span> : null}
              </button>
            ) : null}

            {!post.runningRecord && post.course ? (
              <button
                type="button"
                onClick={() => onOpenCourse(post.course!.id)}
                className={`mt-4 flex w-full items-center gap-3 rounded-[12px] px-3 py-3 text-left ${
                  post.course.courseType === 'SPOT_COURSE' ? 'bg-[#F0FDF4]' : 'bg-[#FFF5EE]'
                }`}
              >
                <div
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-[9px] text-[16px] text-white"
                  style={{
                    background: post.course.courseType === 'SPOT_COURSE' ? '#34C759' : '#FF6F0F',
                  }}
                >
                  {post.course.courseType === 'SPOT_COURSE' ? 'S' : 'R'}
                </div>
                <div className="min-w-0">
                  <div className="truncate text-[13px] font-bold text-[#261912]">{post.course.name}</div>
                  <div className="mt-1 text-[11px] font-bold text-[#594136]">
                    {post.course.courseType === 'RUNNING_COURSE' ? '러닝 코스' : '스팟 코스'}
                  </div>
                </div>
                <span className="ml-auto shrink-0 text-[11px] font-bold text-[#FF6F0F]">코스 보기</span>
              </button>
            ) : null}

            <div className="mt-4 whitespace-pre-wrap text-[14px] leading-[1.7] text-[#261912]">{post.content}</div>

            {post.imageUrls.length > 0 ? (
              <div className="mt-4 grid grid-cols-1 gap-2">
                {post.imageUrls.map((imageUrl, index) => (
                  <div
                    key={`${imageUrl}-${index}`}
                    className="aspect-[4/3] rounded-[12px] bg-cover bg-center"
                    style={{ backgroundImage: `url(${imageUrl})` }}
                  />
                ))}
              </div>
            ) : null}

            <div className="mt-4 flex items-center gap-4 border-t border-[#F5E7E1] pt-3 text-[13px] font-medium text-[#8D7164]">
              <button
                type="button"
                onClick={handleLike}
                className={`flex items-center gap-1 ${post.likedByMe ? 'text-[#FF3B30]' : ''}`}
              >
                <span>{post.likedByMe ? '♥' : '♡'}</span>
                <span>{post.likeCount}</span>
              </button>
              <span>댓글 {post.commentCount}</span>
              {post.course ? (
                <button
                  type="button"
                  onClick={() => onOpenCourse(post.course!.id)}
                  className="ml-auto text-[12px] font-bold text-[#FF6F0F]"
                >
                  {post.course.courseType === 'RUNNING_COURSE' ? '러닝 코스' : '스팟 코스'}
                </button>
              ) : null}
            </div>
          </article>

          <section className="mt-3 rounded-[18px] bg-white px-5 py-4 shadow-[0px_2px_12px_rgba(0,0,0,0.06)]">
            <div className="text-[14px] font-bold text-[#261912]">댓글 {post.commentCount}</div>

            <div className="mt-4 flex gap-2">
              <textarea
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                onKeyDown={handleCommentKeyDown}
                placeholder="댓글을 입력해 주세요."
                rows={1}
                className="min-h-11 flex-1 resize-none rounded-[22px] border border-[#E1BFB1] bg-[#FFF8F6] px-4 py-3 text-[13px] leading-5 text-[#261912] outline-none"
              />
              <button
                type="button"
                onClick={handleCommentSubmit}
                disabled={pending || !comment.trim()}
                className="h-11 rounded-full bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] px-4 text-[13px] font-bold text-white disabled:opacity-40"
              >
                등록
              </button>
            </div>

            <div className="mt-3 space-y-3">
              {post.comments.length === 0 ? (
                <div className="rounded-[12px] bg-[#FFF8F6] px-4 py-4 text-[12px] text-[#8D7164]">
                  아직 댓글이 없습니다.
                </div>
              ) : (
                post.comments.map((item) => (
                  <div key={item.id} className="rounded-[12px] bg-[#FFF8F6] px-4 py-3">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="text-[12px] font-bold text-[#261912]">{item.nickname}</div>
                        <div className="mt-0.5 text-[10px] text-[#8D7164]">{formatRelativeTime(item.createdAt)}</div>
                      </div>
                      {item.mine ? (
                        <button
                          type="button"
                          onClick={() => handleDeleteComment(item.id)}
                          className="text-[10px] font-bold text-[#8D7164]"
                        >
                          삭제
                        </button>
                      ) : null}
                    </div>
                    <div className="mt-1 whitespace-pre-wrap text-[12px] leading-5 text-[#594136]">{item.content}</div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

function isDesktopBrowser() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false
  }

  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}
