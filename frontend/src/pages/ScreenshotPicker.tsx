import { useEffect, useMemo, useRef, useState, type ClipboardEvent, type DragEvent } from 'react'

const SCREENSHOT_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
const MAX_SCREENSHOTS = 8
const MAX_BYTES = 5 * 1024 * 1024

interface Props {
  files: File[]
  onChange: (files: File[]) => void
}

export default function ScreenshotPicker({ files, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [rejected, setRejected] = useState<string | null>(null)

  const previews = useMemo(() => files.map((file) => URL.createObjectURL(file)), [files])
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  function add(incoming: File[]) {
    const problems: string[] = []
    const accepted = incoming.filter((file) => {
      if (!SCREENSHOT_TYPES.includes(file.type)) problems.push(`${file.name} is not a PNG, JPEG, WebP or GIF`)
      else if (file.size > MAX_BYTES) problems.push(`${file.name} is larger than 5 MB`)
      else return true
      return false
    })
    const next = [...files, ...accepted]
    if (next.length > MAX_SCREENSHOTS) problems.push(`Only ${MAX_SCREENSHOTS} screenshots are allowed`)
    setRejected(problems.length ? problems.join('. ') : null)
    onChange(next.slice(0, MAX_SCREENSHOTS))
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault()
    setDragging(false)
    add(Array.from(e.dataTransfer.files))
  }

  function handlePaste(e: ClipboardEvent) {
    const pasted = Array.from(e.clipboardData.files)
    if (pasted.length) {
      e.preventDefault()
      add(pasted)
    }
  }

  return (
    <div className="screenshots">
      <div
        className={`dropzone${dragging ? ' dragging' : ''}`}
        tabIndex={0}
        role="button"
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onPaste={handlePaste}
      >
        <strong>Drop screenshots here, click to choose, or paste with Ctrl+V</strong>
        <span className="muted">Up to {MAX_SCREENSHOTS} images, 5 MB each. Several readable shots beat one very tall one.</span>
        <input
          ref={inputRef}
          type="file"
          accept={SCREENSHOT_TYPES.join(',')}
          multiple
          hidden
          onChange={(e) => { add(Array.from(e.target.files ?? [])); e.target.value = '' }}
        />
      </div>

      {rejected && <p className="error">{rejected}</p>}

      {files.length > 0 && (
        <ul className="thumbs">
          {files.map((file, i) => (
            <li key={previews[i]}>
              <img src={previews[i]} alt={file.name} />
              <button
                type="button"
                className="remove"
                aria-label={`Remove ${file.name}`}
                onClick={() => onChange(files.filter((_, j) => j !== i))}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
