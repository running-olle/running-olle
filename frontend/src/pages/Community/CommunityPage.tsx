import { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { ChatList } from '../../features/community/ChatList'
import { ChatRoomModal } from '../../features/community/ChatRoomModal'
import { createInquiryRoom, deleteChatMessage, getChatRoom, getChatRooms, sendChatMessage } from '../../features/community/chatApi'
import { connectChatListRealtime } from '../../features/community/chatRealtime'
import { getCommunityErrorMessage } from '../../features/community/communityError'
import type { ChatRoom, CommunityTab, Meetup, MeetupFilter } from '../../features/community/communityTypes'
import { CoursePreviewModal } from '../../features/community/CoursePreviewModal'
import { FeedComposer } from '../../features/community/FeedComposer'
import { FeedDetailModal } from '../../features/community/FeedDetailModal'
import { FeedPostCard } from '../../features/community/FeedPostCard'
import { MeetupApplicantsModal } from '../../features/community/MeetupApplicantsModal'
import { MeetupComposer } from '../../features/community/MeetupComposer'
import { MeetupDetailModal } from '../../features/community/MeetupDetailModal'
import { MeetupJoinRequestModal } from '../../features/community/MeetupJoinRequestModal'
import { MeetupList } from '../../features/community/MeetupList'
import { getFeed, type FeedPost } from '../../features/community/api'
import {
  acceptMeetupParticipant,
  createMeetup,
  deleteMeetup,
  getMeetups,
  joinMeetup,
  rejectMeetupParticipant,
  updateMeetup,
} from '../../features/community/meetupApi'

type FeedFilter = 'all' | 'running' | 'spot' | 'photo'

const tabs: Array<{ key: CommunityTab; label: string }> = [
  { key: 'feed', label: '피드' },
  { key: 'meetup', label: '번개' },
  { key: 'chat', label: '채팅' },
]

const filters: Array<{ key: FeedFilter; label: string }> = [
  { key: 'all', label: '전체' },
  { key: 'running', label: '러닝 코스' },
  { key: 'spot', label: '스팟 코스' },
  { key: 'photo', label: '포토' },
]

export function CommunityPage() {
  const { search } = useLocation()
  const [activeTab, setActiveTab] = useState<CommunityTab>(() => getTabFromSearch(search))
  const [activeFilter, setActiveFilter] = useState<FeedFilter>('all')
  const [meetupFilter, setMeetupFilter] = useState<MeetupFilter>('all')
  const [chatSearchOpen, setChatSearchOpen] = useState(false)
  const [chatSearchValue, setChatSearchValue] = useState('')
  const [posts, setPosts] = useState<FeedPost[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [meetupLoading, setMeetupLoading] = useState(true)
  const [meetupError, setMeetupError] = useState('')
  const [chatLoading, setChatLoading] = useState(true)
  const [chatError, setChatError] = useState('')
  const [composerOpen, setComposerOpen] = useState(false)
  const [editingPost, setEditingPost] = useState<FeedPost | null>(null)
  const [detailPost, setDetailPost] = useState<FeedPost | null>(null)
  const [previewCourseId, setPreviewCourseId] = useState<string | null>(null)
  const [meetups, setMeetups] = useState<Meetup[]>([])
  const [chatRooms, setChatRooms] = useState<ChatRoom[]>([])
  const [selectedMeetup, setSelectedMeetup] = useState<Meetup | null>(null)
  const [editingMeetup, setEditingMeetup] = useState<Meetup | null>(null)
  const [meetupComposerOpen, setMeetupComposerOpen] = useState(false)
  const [applicantsMeetupId, setApplicantsMeetupId] = useState<string | null>(null)
  const [selectedChatRoom, setSelectedChatRoom] = useState<ChatRoom | null>(null)
  const [requestSuccessMeetup, setRequestSuccessMeetup] = useState<Meetup | null>(null)

  useEffect(() => {
    setActiveTab(getTabFromSearch(search))
  }, [search])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    setMeetupLoading(true)
    setMeetupError('')
    setChatLoading(true)
    setChatError('')

    getFeed()
      .then((data) => {
        if (active) setPosts(data)
      })
      .catch(() => {
        if (active) setError('피드 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    getMeetups()
      .then((data) => {
        if (active) setMeetups(data)
      })
      .catch(() => {
        if (active) setMeetupError('번개 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (active) setMeetupLoading(false)
      })

    getChatRooms()
      .then((data) => {
        if (active) setChatRooms(data)
      })
      .catch(() => {
        if (active) {
          setChatRooms([])
          setChatError('채팅 목록을 불러오지 못했습니다.')
        }
      })
      .finally(() => {
        if (active) setChatLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    return connectChatListRealtime(
      (rooms) => {
        setChatRooms((current) => mergeChatRooms(current, rooms))
        setSelectedChatRoom((current) => {
          if (!current) return current
          const incoming = rooms.find((room) => room.id === current.id)
          if (!incoming) return current
          return {
            ...incoming,
            messages: current.messages.length > 0 ? current.messages : incoming.messages,
          }
        })
      },
      (room) => {
        setChatRooms((current) => {
          const exists = current.some((item) => item.id === room.id)
          if (!exists) {
            return [room, ...current]
          }
          return current.map((item) =>
            item.id === room.id
              ? {
                  ...room,
                  messages: item.messages.length > 0 ? item.messages : room.messages,
                }
              : item,
          )
        })
        setSelectedChatRoom((current) => {
          if (!current || current.id !== room.id) {
            return current
          }
          return {
            ...room,
            messages: current.messages.length > 0 ? current.messages : room.messages,
          }
        })
      },
    )
  }, [])

  const filteredPosts = posts.filter((post) => {
    if (activeFilter === 'all') return true
    if (activeFilter === 'photo') return post.photoTagged
    if (activeFilter === 'running') return post.course?.courseType === 'RUNNING_COURSE'
    if (activeFilter === 'spot') return post.course?.courseType === 'SPOT_COURSE'
    return true
  })

  const filteredMeetups = useMemo(() => {
    const todayKey = getDateKey(new Date())
    const thisWeekKeys = getConsecutiveDateKeys(new Date(), 7)

    return meetups.filter((meetup) => {
      if (meetupFilter === 'all') return true
      if (meetupFilter === 'recruiting') return meetup.status === 'recruiting'
      if (meetupFilter === 'today') return meetup.dateKey === todayKey
      if (meetupFilter === 'thisWeek') return thisWeekKeys.includes(meetup.dateKey)
      if (meetupFilter === 'coast') return meetup.theme === 'coast'
      if (meetupFilter === 'oreum') return meetup.theme === 'oreum'
      return true
    })
  }, [meetupFilter, meetups])

  const filteredChatRooms = useMemo(() => {
    const keyword = chatSearchValue.trim().toLowerCase()
    if (!keyword) return chatRooms
    return chatRooms.filter(
      (room) =>
        room.title.toLowerCase().includes(keyword) ||
        room.lastMessage.toLowerCase().includes(keyword) ||
        room.subtitle.toLowerCase().includes(keyword),
    )
  }, [chatRooms, chatSearchValue])

  const applicantsMeetup = applicantsMeetupId
    ? meetups.find((item) => item.id === applicantsMeetupId) ?? null
    : null
  const selectedMeetupForChat = selectedChatRoom
    ? meetups.find((item) => item.id === selectedChatRoom.meetupId)
    : undefined

  const updatePost = (targetId: string, nextPost: FeedPost | null) => {
    setPosts((current) =>
      current.flatMap((post) => {
        if (post.id !== targetId) return [post]
        return nextPost ? [nextPost] : []
      }),
    )

    if (detailPost?.id === targetId) {
      setDetailPost(nextPost)
    }
  }

  const upsertPost = (post: FeedPost) => {
    setPosts((current) => {
      const exists = current.some((item) => item.id === post.id)
      if (!exists) {
        return [post, ...current]
      }
      return current.map((item) => (item.id === post.id ? post : item))
    })

    setComposerOpen(false)
    setEditingPost(null)
    setDetailPost(post)
    setActiveTab('feed')
  }

  const upsertMeetup = (meetup: Meetup) => {
    setMeetups((current) => {
      const exists = current.some((item) => item.id === meetup.id)
      if (!exists) {
        return [meetup, ...current]
      }
      return current.map((item) => (item.id === meetup.id ? meetup : item))
    })
    setMeetupComposerOpen(false)
    setEditingMeetup(null)
    setSelectedMeetup(meetup)
    setActiveTab('meetup')
  }

  const handleJoinMeetup = async (meetup: Meetup) => {
    try {
      const nextMeetup = await joinMeetup(meetup.id)
      patchMeetup(nextMeetup)

      if (nextMeetup.myParticipation === 'accepted') {
        const rooms = await getChatRooms().catch(() => null)
        if (rooms) setChatRooms(rooms)
        setSelectedMeetup(nextMeetup)
        return
      }

      if (nextMeetup.myParticipation === 'pending') {
        setSelectedMeetup(null)
        setRequestSuccessMeetup(nextMeetup)
      }
    } catch (error) {
      window.alert(getCommunityErrorMessage(error, '번개 참여 처리에 실패했습니다.'))
    }
  }

  const handleAcceptApplicant = async (meetupId: string, participantId: string) => {
    try {
      const nextMeetup = await acceptMeetupParticipant(meetupId, participantId)
      patchMeetup(nextMeetup)
      const rooms = await getChatRooms().catch(() => null)
      if (rooms) setChatRooms(rooms)
    } catch (error) {
      window.alert(getCommunityErrorMessage(error, '참여 요청 수락에 실패했습니다.'))
    }
  }

  const handleRejectApplicant = async (meetupId: string, participantId: string) => {
    try {
      const nextMeetup = await rejectMeetupParticipant(meetupId, participantId)
      patchMeetup(nextMeetup)
    } catch (error) {
      window.alert(getCommunityErrorMessage(error, '참여 요청 거절에 실패했습니다.'))
    }
  }

  const handleDeleteMeetup = async (meetup: Meetup) => {
    if (!window.confirm('이 번개를 삭제할까요?')) {
      return
    }

    try {
      await deleteMeetup(meetup.id)
      setMeetups((current) => current.filter((item) => item.id !== meetup.id))
      setSelectedMeetup(null)
    } catch {
      window.alert('번개를 삭제하지 못했습니다.')
    }
  }

  const openInquiryChatForMeetup = (meetup: Meetup) => {
    createInquiryRoom(meetup.id)
      .then((room) => {
        patchChatRoom(room)
        setSelectedChatRoom(room)
        setActiveTab('chat')
        setSelectedMeetup(null)
        setRequestSuccessMeetup(null)
      })
      .catch(() => {
        window.alert('문의 채팅을 열지 못했습니다.')
      })
  }

  const openGroupChatForMeetup = (meetup: Meetup) => {
    const existing = chatRooms.find((room) => room.type === 'group' && room.meetupId === meetup.id)
    if (!existing) {
      getChatRooms()
        .then((rooms) => {
          setChatRooms(rooms)
          const refreshed = rooms.find((room) => room.type === 'group' && room.meetupId === meetup.id)
          if (!refreshed) {
            window.alert('참여 중인 그룹 채팅방을 찾지 못했습니다.')
            return
          }

          getChatRoom(refreshed.id)
            .then((room) => {
              patchChatRoom(room)
              setSelectedChatRoom(room)
              setActiveTab('chat')
              setSelectedMeetup(null)
            })
            .catch(() => {
              setSelectedChatRoom(refreshed)
              setActiveTab('chat')
              setSelectedMeetup(null)
            })
        })
        .catch(() => {
          window.alert('채팅방 정보를 다시 불러오지 못했습니다.')
        })
      return
    }

    getChatRoom(existing.id)
      .then((room) => {
        patchChatRoom(room)
        setSelectedChatRoom(room)
        setActiveTab('chat')
        setSelectedMeetup(null)
      })
      .catch(() => {
        setSelectedChatRoom(existing)
        setActiveTab('chat')
        setSelectedMeetup(null)
      })
  }

  const handleSendMessage = (chatRoomId: string, content: string) => {
    sendChatMessage(chatRoomId, content)
      .then((room) => {
        patchChatRoom(room)
        setSelectedChatRoom(room)
      })
      .catch(() => {
        window.alert('메시지 전송에 실패했습니다.')
      })
  }

  const handleDeleteMessage = (chatRoomId: string, messageId: string) => {
    if (!window.confirm('이 메시지를 삭제할까요?')) {
      return
    }

    deleteChatMessage(chatRoomId, messageId)
      .then((room) => {
        patchChatRoom(room)
        setSelectedChatRoom(room)
      })
      .catch(() => {
        window.alert('메시지를 삭제하지 못했습니다.')
      })
  }

  const handleShare = async (label: string, url: string) => {
    try {
      await navigator.clipboard.writeText(url)
      window.alert(`${label} 링크를 복사했습니다.`)
    } catch {
      window.alert(`${label} 링크 복사에 실패했습니다.`)
    }
  }

  return (
    <>
      <section className="flex items-start justify-between">
        <h1 className="text-[20px] font-black leading-[28px] text-[#261912]">커뮤니티</h1>
        {activeTab === 'chat' ? (
          <button
            type="button"
            onClick={() => setChatSearchOpen((prev) => !prev)}
            className="flex h-9 w-9 items-center justify-center rounded-[10px] bg-[#F5F5F5] text-[16px] text-[#594136] shadow-[0px_4px_12px_rgba(0,0,0,0.04)]"
            aria-label="채팅 검색"
          >
            ⌕
          </button>
        ) : (
          <button
            type="button"
            onClick={() => {
              if (activeTab === 'feed') {
                setEditingPost(null)
                setComposerOpen(true)
                return
              }
              if (activeTab === 'meetup') {
                setEditingMeetup(null)
                setMeetupComposerOpen(true)
              }
            }}
            className="flex h-9 w-9 items-center justify-center rounded-[10px] bg-[#FF6F0F] text-[16px] text-white shadow-[0px_4px_12px_rgba(0,0,0,0.08)]"
            aria-label={activeTab === 'meetup' ? '번개 만들기' : '게시글 작성'}
          >
            +
          </button>
        )}
      </section>

      <div className="mt-4 flex gap-[22px] border-b border-[#E1BFB1]">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => {
              setActiveTab(tab.key)
              if (tab.key !== 'chat') {
                setChatSearchOpen(false)
                setChatSearchValue('')
              }
            }}
            className={`border-b-[2.5px] pb-3 text-[14px] font-bold ${
              activeTab === tab.key ? 'border-[#FF6F0F] text-[#261912]' : 'border-transparent text-[#8D7164]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'feed' ? (
        <>
          <div className="mt-4 flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {filters.map((filter) => (
              <button
                key={filter.key}
                type="button"
                onClick={() => setActiveFilter(filter.key)}
                className={`shrink-0 rounded-full border px-4 py-2 text-[12px] font-bold ${
                  activeFilter === filter.key
                    ? 'border-[#FF6F0F] bg-[#FF6F0F] text-white'
                    : 'border-[#E1BFB1] bg-white text-[#594136]'
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>

          <div className="mt-3 rounded-[12px] border border-[#FFE4CC] bg-[#FFF5EE] px-4 py-2.5 text-[12px] font-bold text-[#A04100]">
            최근 7일 이내 제주 지역 피드만 노출됩니다.
          </div>

          <div className="mt-4 flex flex-col gap-2">
            {loading ? <StateBox message="피드 목록을 불러오는 중입니다." /> : null}
            {!loading && error ? <StateBox message={error} tone="error" /> : null}
            {!loading && !error && filteredPosts.length === 0 ? (
              <StateBox message="조건에 맞는 피드가 없습니다." />
            ) : null}
            {!loading && !error
              ? filteredPosts.map((post) => (
                  <FeedPostCard
                    key={post.id}
                    post={post}
                    onChange={(nextPost) => updatePost(post.id, nextPost)}
                    onEdit={(target) => {
                      setEditingPost(target)
                      setComposerOpen(true)
                    }}
                    onOpenDetail={(target) => setDetailPost(target)}
                    onOpenCourse={setPreviewCourseId}
                  />
                ))
              : null}
          </div>
        </>
      ) : null}

      {activeTab === 'meetup' ? (
        <>
          {meetupLoading ? <StateBox message="번개 목록을 불러오는 중입니다." /> : null}
          {!meetupLoading && meetupError ? <StateBox message={meetupError} tone="error" /> : null}
          {!meetupLoading && !meetupError ? (
            <MeetupList
              meetups={filteredMeetups}
              activeFilter={meetupFilter}
              onFilterChange={setMeetupFilter}
              onOpenDetail={setSelectedMeetup}
            />
          ) : null}
        </>
      ) : null}

      {activeTab === 'chat' ? (
        <>
          {chatLoading ? <StateBox message="채팅 목록을 불러오는 중입니다." /> : null}
          {!chatLoading && chatError ? <StateBox message={chatError} tone="error" /> : null}
          {!chatLoading && !chatError ? (
            <ChatList
              groupChats={filteredChatRooms.filter((room) => room.type === 'group')}
              inquiryChats={filteredChatRooms.filter((room) => room.type === 'inquiry')}
              searchOpen={chatSearchOpen}
              searchValue={chatSearchValue}
              onSearchChange={setChatSearchValue}
              onOpenChat={(room) => {
                getChatRoom(room.id)
                  .then((nextRoom) => {
                    patchChatRoom(nextRoom)
                    setSelectedChatRoom(nextRoom)
                  })
                  .catch(() => setSelectedChatRoom(room))
              }}
            />
          ) : null}
        </>
      ) : null}

      {composerOpen ? (
        <FeedComposer
          editingPost={editingPost}
          onCancel={() => {
            setComposerOpen(false)
            setEditingPost(null)
          }}
          onCreated={upsertPost}
        />
      ) : null}

      {detailPost ? (
        <FeedDetailModal
          feedPostId={detailPost.id}
          initialPost={detailPost}
          onClose={() => setDetailPost(null)}
          onChange={(nextPost) => updatePost(detailPost.id, nextPost)}
          onEdit={(target) => {
            setDetailPost(null)
            setEditingPost(target)
            setComposerOpen(true)
          }}
          onOpenCourse={setPreviewCourseId}
        />
      ) : null}

      {previewCourseId ? (
        <CoursePreviewModal
          courseId={previewCourseId}
          onClose={() => setPreviewCourseId(null)}
        />
      ) : null}

      {meetupComposerOpen ? (
        <MeetupComposer
          editingMeetup={editingMeetup}
          onClose={() => {
            setMeetupComposerOpen(false)
            setEditingMeetup(null)
          }}
          onSubmit={async (payload) => {
            try {
              const meetup = editingMeetup ? await updateMeetup(editingMeetup.id, payload) : await createMeetup(payload)
              upsertMeetup(meetup)
            } catch (error) {
              window.alert(getCommunityErrorMessage(error, editingMeetup ? '번개 수정에 실패했습니다.' : '번개 생성에 실패했습니다.'))
            }
          }}
        />
      ) : null}

      {selectedMeetup ? (
        <MeetupDetailModal
          meetup={selectedMeetup}
          myParticipation={selectedMeetup.myParticipation}
          onClose={() => setSelectedMeetup(null)}
          onJoin={handleJoinMeetup}
          onOpenApplicants={(meetup) => setApplicantsMeetupId(meetup.id)}
          onOpenInquiryChat={openInquiryChatForMeetup}
          onOpenGroupChat={openGroupChatForMeetup}
          onEdit={(meetup) => {
            setSelectedMeetup(null)
            setEditingMeetup(meetup)
            setMeetupComposerOpen(true)
          }}
          onDelete={handleDeleteMeetup}
          onShare={(meetup) => handleShare('번개', `https://runningolle.app/community/meetups/${meetup.id}`)}
          onOpenCourse={setPreviewCourseId}
        />
      ) : null}

      {applicantsMeetup ? (
        <MeetupApplicantsModal
          meetup={applicantsMeetup}
          onClose={() => setApplicantsMeetupId(null)}
          onAccept={handleAcceptApplicant}
          onReject={handleRejectApplicant}
        />
      ) : null}

      {selectedChatRoom ? (
        <ChatRoomModal
          chatRoom={selectedChatRoom}
          meetup={selectedMeetupForChat}
          onClose={() => setSelectedChatRoom(null)}
          onSendMessage={handleSendMessage}
          onDeleteMessage={handleDeleteMessage}
          onRealtimeRoom={(room) => {
            patchChatRoom(room)
            setSelectedChatRoom(room)
          }}
          onShare={(chatRoom) => handleShare('채팅방', `https://runningolle.app/community/chats/${chatRoom.id}`)}
        />
      ) : null}

      {requestSuccessMeetup ? (
        <MeetupJoinRequestModal
          meetup={requestSuccessMeetup}
          onClose={() => {
            setRequestSuccessMeetup(null)
            setActiveTab('meetup')
          }}
          onOpenInquiry={openInquiryChatForMeetup}
        />
      ) : null}
    </>
  )

  function patchMeetup(nextMeetup: Meetup) {
    setMeetups((current) => {
      const exists = current.some((item) => item.id === nextMeetup.id)
      if (!exists) {
        return [nextMeetup, ...current]
      }
      return current.map((item) => (item.id === nextMeetup.id ? nextMeetup : item))
    })
    setSelectedMeetup((current) => (current?.id === nextMeetup.id ? nextMeetup : current))
    setApplicantsMeetupId((current) => (current === nextMeetup.id ? nextMeetup.id : current))
  }

  function patchChatRoom(nextRoom: ChatRoom) {
    setChatRooms((current) => {
      const exists = current.some((room) => room.id === nextRoom.id)
      if (!exists) {
        return [nextRoom, ...current]
      }
      return current.map((room) => (room.id === nextRoom.id ? nextRoom : room))
    })
  }
}

function mergeChatRooms(current: ChatRoom[], incoming: ChatRoom[]) {
  const currentById = new Map(current.map((room) => [room.id, room]))
  return incoming.map((room) => {
    const existing = currentById.get(room.id)
    if (!existing) {
      return room
    }

    return existing.messages.length > room.messages.length ? { ...room, messages: existing.messages } : room
  })
}

function getDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function getTabFromSearch(search: string): CommunityTab {
  const tab = new URLSearchParams(search).get('tab')
  return tab === 'meetup' || tab === 'chat' ? tab : 'feed'
}

function getConsecutiveDateKeys(startDate: Date, days: number) {
  return Array.from({ length: days }, (_, index) => {
    const nextDate = new Date(startDate)
    nextDate.setDate(startDate.getDate() + index)
    return getDateKey(nextDate)
  })
}

function StateBox({ message, tone = 'normal' }: { message: string; tone?: 'normal' | 'error' }) {
  return (
    <div
      className={`rounded-[16px] px-4 py-5 text-[13px] font-bold ${
        tone === 'error'
          ? 'bg-[#FFF1EE] text-[#B91C1C]'
          : 'bg-white text-[#594136] shadow-[0px_4px_12px_rgba(0,0,0,0.05)]'
      }`}
    >
      {message}
    </div>
  )
}
