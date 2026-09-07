import { useEffect, useId, useRef, type MouseEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type OverlayProps = {
  open: boolean
  title: ReactNode
  description?: ReactNode
  children?: ReactNode
  footer?: ReactNode
  onClose: () => void
  closeLabel?: string
  closeOnBackdrop?: boolean
  closeOnEscape?: boolean
  className?: string
}

function useOverlayLifecycle(open: boolean, onClose: () => void, closeOnEscape: boolean) {
  const panelRef = useRef<HTMLElement>(null)
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!open) return
    const previousFocus = document.activeElement as HTMLElement | null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    panelRef.current?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && closeOnEscape) {
        onCloseRef.current()
        return
      }
      if (event.key !== 'Tab' || !panelRef.current) return

      const focusable = Array.from(panelRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
      )).filter((element) => !element.hasAttribute('hidden'))
      if (focusable.length === 0) {
        event.preventDefault()
        panelRef.current.focus()
        return
      }

      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
      previousFocus?.focus()
    }
  }, [closeOnEscape, open])

  return panelRef
}

export function Modal({
  open,
  title,
  description,
  children,
  footer,
  onClose,
  closeLabel = '닫기',
  closeOnBackdrop = true,
  closeOnEscape = true,
  className = '',
}: OverlayProps) {
  const titleId = useId()
  const descriptionId = useId()
  const panelRef = useOverlayLifecycle(open, onClose, closeOnEscape)
  if (!open) return null

  const handleBackdrop = (event: MouseEvent<HTMLDivElement>) => {
    if (closeOnBackdrop && event.target === event.currentTarget) onClose()
  }

  return createPortal(
    <div className="ui-overlay ui-overlay--center" onMouseDown={handleBackdrop}>
      <section
        ref={panelRef}
        className={`ui-dialog ${className}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
      >
        <header className="ui-dialog__header">
          <div>
            <h2 className="ui-dialog__title" id={titleId}>{title}</h2>
            {description && <p className="ui-dialog__description" id={descriptionId}>{description}</p>}
          </div>
          <button type="button" className="ui-dialog__close" aria-label={closeLabel} onClick={onClose}>×</button>
        </header>
        {children && <div className="ui-dialog__body">{children}</div>}
        {footer && <footer className="ui-dialog__footer">{footer}</footer>}
      </section>
    </div>,
    document.body,
  )
}

type BottomSheetProps = OverlayProps & {
  showHandle?: boolean
}

export function BottomSheet({
  open,
  title,
  description,
  children,
  footer,
  onClose,
  closeLabel = '닫기',
  closeOnBackdrop = true,
  closeOnEscape = true,
  showHandle = true,
  className = '',
}: BottomSheetProps) {
  const titleId = useId()
  const descriptionId = useId()
  const panelRef = useOverlayLifecycle(open, onClose, closeOnEscape)
  if (!open) return null

  const handleBackdrop = (event: MouseEvent<HTMLDivElement>) => {
    if (closeOnBackdrop && event.target === event.currentTarget) onClose()
  }

  return createPortal(
    <div className="ui-overlay ui-overlay--bottom" onMouseDown={handleBackdrop}>
      <section
        ref={panelRef}
        className={`ui-sheet ${className}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
      >
        {showHandle && <div className="ui-sheet__handle" aria-hidden="true" />}
        <header className="ui-sheet__header">
          <div>
            <h2 className="ui-dialog__title" id={titleId}>{title}</h2>
            {description && <p className="ui-dialog__description" id={descriptionId}>{description}</p>}
          </div>
          <button type="button" className="ui-dialog__close" aria-label={closeLabel} onClick={onClose}>×</button>
        </header>
        {children && <div className="ui-sheet__body">{children}</div>}
        {footer && <footer className="ui-sheet__footer">{footer}</footer>}
      </section>
    </div>,
    document.body,
  )
}
