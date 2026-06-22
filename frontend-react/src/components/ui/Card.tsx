import type { HTMLAttributes } from 'react'

type CardTone = 'default' | 'muted' | 'accent'

type CardProps = HTMLAttributes<HTMLElement> & {
  as?: 'article' | 'div' | 'section'
  tone?: CardTone
}

export function Card({
  as = 'article',
  children,
  className,
  tone = 'default',
  ...props
}: CardProps) {
  const Component = as
  const toneClass = {
    default: 'bg-[var(--color-surface)]',
    muted: 'bg-[var(--color-muted-surface)]',
    accent: 'bg-[var(--color-primary-soft)]',
  }[tone]

  return (
    <Component
      className={[
        'rounded-[24px] border border-[var(--color-border)] p-6 shadow-[var(--shadow-card)]',
        toneClass,
        className ?? '',
      ]
        .join(' ')
        .trim()}
      {...props}
    >
      {children}
    </Component>
  )
}
