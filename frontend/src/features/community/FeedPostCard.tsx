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
    <article className="rounded-[18px] bg-white px-5 py-4 shadow-[0px_2px_12px_rgba(0,0,0,0.06)]">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <div
            className="flex h-[38px] w-[38px] items-center justify-center rounded-full text-[14px] font-bold text-white"
            style={{ backgroundImage: buildAvatarGradient(post) }}
          >
            {post.nickname.slice(0, 1)}
          </div>
          <div className="min-w-0">
            <div className="truncate text-[14px] font-bold text-[#261912]">{post.nickname}</div>
            <div className="mt-0.5 text-[11px] text-[#8D7164]">
              {createdLabel} · {post.region}
            </div>
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
        ) : (
          <span className="text-[18px] leading-none text-[#C8A99A]">⋯</span>
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
          className={`mt-4 flex w-full items-center gap-3 rounded-[12px] px-3 py-3 text-left ${
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
          <div className="min-w-0">
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

      <button type="button" onClick={() => onOpenDetail(post)} className="mt-4 block w-full text-left">
        <p className="whitespace-pre-wrap text-[14px] leading-[1.65] text-[#261912]">{post.content}</p>
      </button>

      {post.imageUrls.length > 0 ? (
        <button type="button" onClick={() => onOpenDetail(post)} className="mt-3 block w-full">
          <div className={`grid gap-[3px] overflow-hidden rounded-[12px] ${buildImageGridClass(post.imageUrls.length)}`}>
            {post.imageUrls.slice(0, 3).map((imageUrl, index) => (
              <div
                key={`${imageUrl}-${index}`}
                className={`relative bg-cover bg-center ${post.imageUrls.length === 1 ? 'aspect-[4/3]' : 'aspect-square'}`}
                style={{ backgroundImage: `url(${imageUrl})` }}
              >
                {index === 2 && post.imageUrls.length > 3 ? (
                  <div className="absolute inset-0 flex items-center justify-center bg-[rgba(38,25,18,0.38)] text-[18px] font-bold text-white">
                    +{post.imageUrls.length - 3}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </button>
      ) : null}

      <div className="mt-3 flex items-center gap-4 border-t border-[#F5E7E1] pt-3 text-[13px] font-medium text-[#8D7164]">
        <button
          type="button"
          onClick={handleLike}
          className={`flex items-center gap-1 ${post.likedByMe ? 'text-[#FF3B30]' : ''}`}
        >
          <span>{post.likedByMe ? '♥' : '♡'}</span>
          <span>{post.likeCount}</span>
        </button>
        <button type="button" onClick={() => onOpenDetail(post)} className="flex items-center gap-1">
          <span>댓글</span>
          <span>{post.commentCount}</span>
        </button>
        {post.course ? (
          <button type="button" onClick={() => onOpenCourse(post.course!.id)} className="ml-auto text-[12px] font-bold text-[#FF6F0F]">
            태그 코스 보기
          </button>
        ) : null}
      </div>
    </article>
  )
}
