import type { HTMLAttributes } from 'react'

type AppLogoProps = HTMLAttributes<HTMLDivElement> & {
  inverted?: boolean
  compact?: boolean
}

export function AppLogo({
  className,
  compact = false,
  inverted = false,
  ...props
}: AppLogoProps) {
  return (
    <div
      className={[
        'inline-flex items-center gap-3',
        inverted ? 'text-white' : 'text-[#102247]',
        className ?? '',
      ]
        .join(' ')
        .trim()}
      {...props}
    >
      <span
        aria-hidden="true"
        className={[
          'relative inline-flex items-center justify-center rounded-2xl',
          compact ? 'h-11 w-11' : 'h-12 w-12',
          inverted
            ? 'bg-white/10 ring-1 ring-white/18'
            : 'bg-white shadow-[0_10px_30px_rgba(15,23,42,0.12)] ring-1 ring-[#E6EBF5]',
        ]
          .join(' ')
          .trim()}
      >
        <svg
          className={compact ? 'h-7 w-7' : 'h-8 w-8'}
          fill="none"
          viewBox="0 0 32 32"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M15.999 4C9.373 4 4 9.148 4 15.5C4 18.227 5.001 20.735 6.674 22.693L5.565 27.5L10.595 26.445C12.195 27.145 14.001 27.5 15.999 27.5C22.625 27.5 28 22.352 28 16C28 9.648 22.625 4 15.999 4Z"
            className={inverted ? 'stroke-[#4ADE80]' : 'stroke-[#16A34A]'}
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2.2"
          />
          <path
            d="M12.1 12.6C12.35 12.1 12.63 12.07 12.99 12.09C13.28 12.1 13.61 12.09 13.94 12.09C14.27 12.09 14.81 11.96 15.05 12.53C15.29 13.1 15.88 14.52 15.94 14.64C16 14.77 16.05 14.93 15.95 15.11C15.85 15.29 15.8 15.4 15.61 15.61C15.42 15.82 15.21 16.08 15.04 16.25C14.87 16.42 14.69 16.61 14.9 16.95C15.11 17.29 15.84 18.47 16.93 19.47C18.32 20.74 19.49 21.12 19.86 21.29C20.23 21.46 20.44 21.43 20.62 21.22C20.8 21.01 21.37 20.39 21.57 20.08C21.77 19.77 21.98 19.82 22.29 19.95C22.6 20.08 24.25 20.87 24.59 21.03C24.93 21.19 25.15 21.27 25.23 21.4C25.31 21.53 25.31 22.17 25.09 22.79C24.87 23.41 23.83 24.01 23.36 24.1C22.89 24.19 22.29 24.39 18.87 22.97C15.45 21.55 12.6 18.8 11.22 15.88C9.84 12.96 11.85 13.1 12.1 12.6Z"
            className={inverted ? 'fill-white' : 'fill-[#2453FF]'}
          />
        </svg>
      </span>

      <span className="leading-tight">
        <span className={compact ? 'block text-base font-semibold' : 'block text-lg font-semibold'}>
          Asistente
        </span>
        <span className={compact ? 'block text-base font-semibold' : 'block text-lg font-semibold'}>
          WhatsApp
        </span>
      </span>
    </div>
  )
}
