import { Icon } from './Icon'

type MapZoomControlsProps = {
  onZoomIn: () => void
  onZoomOut: () => void
  className?: string
}

export function MapZoomControls({ onZoomIn, onZoomOut, className = '' }: MapZoomControlsProps) {
  return (
    <div className={`ui-map-controls ${className}`} aria-label="지도 확대/축소">
      <button type="button" aria-label="지도 확대" onClick={onZoomIn}>
        <Icon name="plus" size={20} />
      </button>
      <button type="button" aria-label="지도 축소" onClick={onZoomOut}>
        <Icon name="minus" size={20} />
      </button>
    </div>
  )
}
