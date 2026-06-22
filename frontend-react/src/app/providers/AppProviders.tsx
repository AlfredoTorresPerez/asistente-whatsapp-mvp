import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { useState } from 'react'
import { ShellSessionProvider } from '../../lib/ShellSessionProvider'
import { ToastProvider } from '../../lib/ToastProvider'

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <ShellSessionProvider>{children}</ShellSessionProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
