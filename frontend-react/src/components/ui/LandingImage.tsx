import { useState } from 'react'

export type LandingImageProps = {
  src?: string | null
  fallbackSrc: string
  alt: string
  className?: string
  loading?: 'lazy' | 'eager'
  fetchPriority?: 'high' | 'low' | 'auto'
  width?: number
  height?: number
  objectFit?: 'cover' | 'contain' | 'fill'
  aspectRatio?: string
}

export function LandingImage({
  src,
  fallbackSrc,
  alt,
  className = '',
  loading = 'lazy',
  fetchPriority = 'auto',
  width,
  height,
  objectFit = 'cover',
  aspectRatio,
}: LandingImageProps) {
  const [imgSrc, setImgSrc] = useState(src ?? fallbackSrc)
  const [failed, setFailed] = useState(false)

  const currentSrc = failed ? fallbackSrc : (imgSrc || fallbackSrc)

  const handleError = () => {
    if (!failed && currentSrc !== fallbackSrc) {
      setImgSrc(fallbackSrc)
      setFailed(true)
    }
  }

  return (
    <img
      src={currentSrc}
      alt={alt}
      className={className}
      loading={loading}
      fetchPriority={fetchPriority}
      width={width}
      height={height}
      style={{
        objectFit,
        aspectRatio: aspectRatio ?? (width && height ? `${width}/${height}` : undefined),
      }}
      onError={handleError}
    />
  )
}
