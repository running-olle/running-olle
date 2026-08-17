export type CourseReview = {
  content: string
  createdAt: string
  isMine: boolean
  nickname: string
  profileImageUrl: string | null
  rating: number
  reviewId: string
  runningRecordId: string
  updatedAt: string
  userId: string
}

export type CourseReviewListResponse = {
  ratingAvg: number | string
  reviewCount: number
  reviews: CourseReview[]
}

export type CourseReviewCreateRequest = {
  content: string
  rating: number
  runningRecordId: string
}

export type CourseReviewUpdateRequest = {
  content: string
  rating: number
}

export type TestTokenRequest = {
  kakaoId: string
}

export type TestTokenResponse = {
  accessToken: string
  provider: string
  providerUserId: string
  tokenType: string
}
