import { forwardRef, useId, type InputHTMLAttributes, type TextareaHTMLAttributes } from 'react'

type FieldState = 'default' | 'error' | 'success'

type SharedFieldProps = {
  label?: string
  message?: string
  state?: FieldState
  count?: string
}

export type InputProps = InputHTMLAttributes<HTMLInputElement> & SharedFieldProps
export type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & SharedFieldProps

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, message, state = 'default', count, className = '', id: providedId, ...inputProps },
  ref,
) {
  const generatedId = useId()
  const id = providedId ?? generatedId
  const messageId = message ? `${id}-message` : undefined

  return (
    <label className="ui-field" htmlFor={id}>
      {label && <span className="ui-field__label">{label}</span>}
      <span className="ui-field__control-wrap">
        <input
          ref={ref}
          id={id}
          className={`ui-input ${className}`}
          aria-invalid={state === 'error' || undefined}
          aria-describedby={messageId}
          {...inputProps}
        />
      </span>
      {(message || count) && (
        <span className="ui-field__meta">
          {message && <span id={messageId} className={`ui-field__message--${state}`}>{message}</span>}
          {count && <span className="ui-field__count">{count}</span>}
        </span>
      )}
    </label>
  )
})

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, message, state = 'default', count, className = '', id: providedId, ...textareaProps },
  ref,
) {
  const generatedId = useId()
  const id = providedId ?? generatedId
  const messageId = message ? `${id}-message` : undefined

  return (
    <label className="ui-field" htmlFor={id}>
      {label && <span className="ui-field__label">{label}</span>}
      <span className="ui-field__control-wrap">
        <textarea
          ref={ref}
          id={id}
          className={`ui-textarea ${className}`}
          aria-invalid={state === 'error' || undefined}
          aria-describedby={messageId}
          {...textareaProps}
        />
      </span>
      {(message || count) && (
        <span className="ui-field__meta">
          {message && <span id={messageId} className={`ui-field__message--${state}`}>{message}</span>}
          {count && <span className="ui-field__count">{count}</span>}
        </span>
      )}
    </label>
  )
})
