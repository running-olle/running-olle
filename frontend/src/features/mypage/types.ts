export type ThemeOption = { id: string; code: string; name: string }
export type Profile = { nickname: string; profileImageUrl: string | null; bio: string | null; userTypes: string[]; preferredDistance: string | null; preferredDifficulty: string | null; themeIds?: string[]; createdAt: string; accountStatus: string }
export type Dashboard = { profile: Profile; totalDistanceKm: number; completionCount: number; uniqueCourseCount: number }
export type RunRecord = { id: string; courseId: string | null; courseName: string | null; courseType: 'RUNNING_COURSE' | 'SPOT_COURSE' | null; thumbnailImageUrl: string | null; distanceKm: number; durationSeconds: number; averagePace: number | null; startedAt: string }
export type Bookmark = { bookmarkId: string; courseId: string; name: string; courseType: 'RUNNING_COURSE' | 'SPOT_COURSE'; distanceKm: number; difficulty: 'LOW' | 'MID' | 'HIGH'; thumbnailImageUrl: string | null; mine: boolean }
export type Trip = { id: string; name: string; region: string | null; startDate: string; endDate: string; thumbnailImageUrl: string | null; completedCourses: number; totalDistanceKm: number; visitedPlaces: number; totalDurationSeconds: number }
export type NotificationSettings = { recommendedCourse: boolean; weather: boolean; savedCourseUpdate: boolean; meetupInvite: boolean; commentLike: boolean; tierChange: boolean; eventChallenge: boolean }
