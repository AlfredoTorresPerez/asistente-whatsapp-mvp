import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

export type DataTableShellRow = {
  id: string
  cells: ReactNode[]
  href?: string
}

type DataTableShellProps = {
  caption: string
  columns: string[]
  emptyMessage?: string
  rows: DataTableShellRow[]
}

export function DataTableShell({
  caption,
  columns,
  emptyMessage = 'No hay registros para mostrar.',
  rows,
}: DataTableShellProps) {
  const navigate = useNavigate()

  const handleRowNavigation = (href?: string) => {
    if (!href) {
      return
    }

    navigate(href)
  }

  return (
    <div className="overflow-hidden rounded-lg border border-[var(--color-border)]">
      <div className="border-b border-[var(--color-border)] bg-slate-50 px-5 py-4">
        <p className="text-sm font-medium text-slate-600">{caption}</p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full border-separate border-spacing-0">
          <thead>
            <tr className="bg-white">
              {columns.map((column) => (
                <th
                  key={column}
                  className="max-w-56 border-b border-[var(--color-border)] px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500"
                  scope="col"
                >
                  {column}
                </th>
              ))}
            </tr>
          </thead>

          <tbody className="bg-white">
            {rows.length === 0 ? (
              <tr>
                <td
                  className="border-b border-[var(--color-border)] px-5 py-8 text-center text-sm text-slate-500"
                  colSpan={columns.length}
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={row.id}
                  className={row.href ? 'cursor-pointer transition hover:bg-slate-50 focus-within:bg-slate-50' : ''}
                  onClick={() => handleRowNavigation(row.href)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      handleRowNavigation(row.href)
                    }
                  }}
                  role={row.href ? 'link' : undefined}
                  tabIndex={row.href ? 0 : undefined}
                >
                  {row.cells.map((cell, index) => (
                    <td
                      key={`${row.id}-${index}`}
                      className="max-w-72 break-words border-b border-[var(--color-border)] px-4 py-3 text-sm text-slate-700 last:text-right"
                    >
                      {cell}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
