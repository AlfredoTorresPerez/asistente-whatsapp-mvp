import { useCallback, useRef, useState, type ChangeEvent } from 'react'
import { Button } from '../ui/Button'
import { useToast } from '../../lib/toast'

type ImageUploadProps = {
  value: string | null
  onChange: (file: File | null) => void
  onRemove: () => void
  disabled?: boolean
  maxSizeMB?: number
  acceptedTypes?: string[]
}

const DEFAULT_MAX_SIZE_MB = 5
const DEFAULT_ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/webp']

export function ImageUpload({
  value,
  onChange,
  onRemove,
  disabled = false,
  maxSizeMB = DEFAULT_MAX_SIZE_MB,
  acceptedTypes = DEFAULT_ACCEPTED_TYPES,
}: ImageUploadProps) {
  const { showToast } = useToast()
  const [preview, setPreview] = useState<string | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const validateFile = useCallback((file: File): boolean => {
    if (file.size > maxSizeMB * 1024 * 1024) {
      showToast({
        title: 'Archivo demasiado grande',
        description: `El archivo no debe superar ${maxSizeMB} MB`,
        tone: 'error',
      })
      return false
    }
    if (!acceptedTypes.includes(file.type)) {
      showToast({
        title: 'Tipo de archivo no permitido',
        description: `Formatos aceptados: ${acceptedTypes.map(t => t.split('/')[1].toUpperCase()).join(', ')}`,
        tone: 'error',
      })
      return false
    }
    return true
  }, [maxSizeMB, acceptedTypes, showToast])

  const handleFileSelect = useCallback((file: File | null) => {
    if (!file) {
      onChange(null)
      setPreview(null)
      return
    }
    if (!validateFile(file)) return
    onChange(file)
    const objectUrl = URL.createObjectURL(file)
    setPreview(objectUrl)
  }, [onChange, validateFile])

  const handleRemove = useCallback(() => {
    onRemove()
    onChange(null)
    setPreview(null)
  }, [onRemove, onChange])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!disabled) setIsDragging(true)
  }, [disabled])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
    if (disabled) return
    const file = e.dataTransfer.files[0]
    if (file) handleFileSelect(file)
  }, [disabled, handleFileSelect])

  const handleInputChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null
    handleFileSelect(file)
  }, [handleFileSelect])

  const openFileDialog = useCallback(() => {
    fileInputRef.current?.click()
  }, [])

  const displayImage = value ? `/api/v1/content-items/media/${value}` : null

  return (
    <div className="space-y-3">
      <div
        className={`relative border-2 border-dashed rounded-2xl p-6 text-center transition-colors ${
          isDragging
            ? 'border-blue-400 bg-blue-50'
            : value
              ? 'border-emerald-300 bg-emerald-50'
              : 'border-slate-200 hover:border-slate-300'
        }`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={!disabled ? openFileDialog : undefined}
        role={!disabled ? 'button' : undefined}
        tabIndex={!disabled ? 0 : undefined}
        onKeyDown={!disabled ? (e) => (e.key === 'Enter' || e.key === ' ') && openFileDialog() : undefined}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept={acceptedTypes.join(',')}
          onChange={handleInputChange}
          className="sr-only"
          disabled={disabled}
        />
        {(value || preview) ? (
          <div className="relative max-w-xs mx-auto">
            <img
              src={preview || displayImage!}
              alt="Vista previa"
              className="max-h-48 w-auto object-contain rounded-lg shadow-sm"
            />
            {!disabled && (
              <div className="absolute top-2 right-2 flex gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 rounded-full bg-white/90 shadow"
                  onClick={(e) => {
                    e.stopPropagation()
                    openFileDialog()
                  }}
                  aria-label="Cambiar imagen"
                >
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 rounded-full bg-white/90 shadow"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleRemove()
                  }}
                  aria-label="Eliminar imagen"
                >
                  <svg className="h-4 w-4 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            <svg
              className="mx-auto h-10 w-10 text-slate-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
              />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <p className="text-sm font-medium text-slate-700">
              {disabled ? 'Imagen actual' : 'Arrastra una imagen o haz clic para seleccionar'}
            </p>
            <p className="text-xs text-slate-500">
              PNG, JPG, WebP &middot; M&aacute;x. {maxSizeMB} MB
            </p>
          </div>
        )}
      </div>
      {value && !disabled && (
        <p className="text-xs text-slate-500 text-center">
          Haz clic en la imagen para cambiarla
        </p>
      )}
    </div>
  )
}