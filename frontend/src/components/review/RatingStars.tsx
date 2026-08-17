type RatingStarsProps = {
  interactive?: boolean
  onChange?: (rating: number) => void
  rating: number
  size?: 'sm' | 'lg'
}

const STAR_COUNT = 5

export function RatingStars({
  interactive = false,
  onChange,
  rating,
  size = 'lg',
}: RatingStarsProps) {
  const normalizedRating = Math.max(0, Math.min(STAR_COUNT, rating))
  const starSizeClass = size === 'sm' ? 'text-base' : 'text-[22px]'

  return (
    <div className="inline-flex items-center gap-1">
      {Array.from({ length: STAR_COUNT }, (_, index) => {
        const starValue = index + 1
        const fillPercent = getFillPercent(normalizedRating, starValue)

        if (interactive) {
          return (
            <div key={starValue} className="relative">
              <button
                type="button"
                onClick={() => onChange?.(starValue - 0.5)}
                className="absolute inset-y-0 left-0 z-10 w-1/2 cursor-pointer"
                aria-label={`${starValue - 0.5}점 선택`}
              />
              <button
                type="button"
                onClick={() => onChange?.(starValue)}
                className="absolute inset-y-0 right-0 z-10 w-1/2 cursor-pointer"
                aria-label={`${starValue}점 선택`}
              />
              <span
                className={`relative inline-block ${starSizeClass} leading-none text-[#e8d7ca] transition`}
                aria-hidden="true"
              >
                ★
                {fillPercent > 0 ? (
                  <span
                    className="absolute inset-y-0 left-0 overflow-hidden text-[#ff8b3d]"
                    style={{ width: `${fillPercent}%` }}
                  >
                    ★
                  </span>
                ) : null}
              </span>
            </div>
          )
        }

        return (
          <span
            key={starValue}
            className={`relative inline-block ${starSizeClass} leading-none text-[#e8d7ca]`}
            aria-hidden="true"
          >
            ★
            {fillPercent > 0 ? (
              <span
                className="absolute inset-y-0 left-0 overflow-hidden text-[#ff8b3d]"
                style={{ width: `${fillPercent}%` }}
              >
                ★
              </span>
            ) : null}
          </span>
        )
      })}
    </div>
  )
}

function getFillPercent(rating: number, starValue: number) {
  const remaining = rating - (starValue - 1)

  if (remaining >= 1) {
    return 100
  }

  if (remaining >= 0.5) {
    return 50
  }

  if (remaining > 0) {
    return remaining * 100
  }

  return 0
}
