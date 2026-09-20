import { useState, type CSSProperties } from 'react'

type ProfileAvatarProps = {
  name: string
  imageUrl?: string | null
  className?: string
  fallbackStyle?: CSSProperties
}

function resolveAssetUrl(url: string) {
  if (!url.startsWith('/')) return url

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined
  return apiBaseUrl?.startsWith('http') ? `${new URL(apiBaseUrl).origin}${url}` : url
}

export function ProfileAvatar({ name, imageUrl, className = '', fallbackStyle }: ProfileAvatarProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = Boolean(imageUrl && failedUrl !== imageUrl)

  return (
    <div
      className={`relative overflow-hidden ${className}`}
      style={fallbackStyle}
      aria-label={`${name} 프로필`}
    >
      <span aria-hidden="true">{name.trim().slice(0, 1).toUpperCase() || '?'}</span>
      {showImage ? (
        <img
          src={resolveAssetUrl(imageUrl!)}
          alt=""
          aria-hidden="true"
          className="absolute inset-0 h-full w-full object-cover"
          onError={() => setFailedUrl(imageUrl!)}
        />
      ) : null}
    </div>
  )
}
