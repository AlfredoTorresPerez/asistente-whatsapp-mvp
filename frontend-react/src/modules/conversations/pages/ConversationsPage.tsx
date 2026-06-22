import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  assignConversationRequest,
  closeConversationRequest,
  getConversationDetailRequest,
  getConversationsRequest,
  markConversationReadRequest,
  previewAiReplyRequest,
  reopenConversationRequest,
  sendConversationMessageRequest,
} from '../../../services/api/conversationsApi'
import type {
  ConversationMessageResponse,
  ConversationSummaryResponse,
} from '../../../services/api/types'
import {
  dedupeById,
} from './conversationPagination'
import {
  CONVERSATION_INBOX_DEFAULT_PAGE_SIZE,
  conversationInboxCategories,
  filterConversations,
  getConversationAssignees,
  getConversationInboxCounts,
  getConversationStatusTone,
  inferConversationCustomerTag,
  paginateConversations,
  translateConversationStatus,
  translateCustomerTag,
} from './conversationInbox'
import type { ConversationCustomerTag, ConversationInboxCategory } from './conversationInbox'

type ConversationTab = ConversationInboxCategory
type SemanticTag = 'NEW_LEAD' | 'FOLLOW_UP' | 'BOOKING' | 'NO_REPLY' | 'ORDER'
type IconName =
  | 'chat'
  | 'clock'
  | 'user-plus'
  | 'cart'
  | 'calendar'
  | 'sparkles'
  | 'send'
  | 'paperclip'
  | 'bookmark'
  | 'bolt'
  | 'filter'
  | 'whatsapp'
  | 'dots'
  | 'smile'
  | 'search'
  | 'check'
  | 'user'

const tabs: Array<{ key: ConversationTab; label: string }> = conversationInboxCategories

const quickReplies = [
  {
    command: '/saludo',
    body: 'Hola, gracias por escribirnos. Te ayudo de inmediato.',
  },
  {
    command: '/gracias',
    body: 'Gracias por confirmar. Quedo atento para ayudarte con el siguiente paso.',
  },
  {
    command: '/precios',
    body: 'Te comparto los valores vigentes. Tambien puedo revisar promociones disponibles para esta semana.',
  },
  {
    command: '/agendar',
    body: 'Perfecto. Puedo ayudarte a agendar. Indicanos si prefieres manana o tarde.',
  },
]

const normalizeComposerText = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim()

const isGenericAiGreetingBody = (value: string) => {
  const normalized = normalizeComposerText(value)
  return normalized === 'hola contacto gracias por escribirnos te ayudo de inmediato'
    || normalized === 'hola gracias por escribirnos te ayudo de inmediato'
    || (normalized.includes('gracias por escribirnos')
      && normalized.includes('te ayudo de inmediato')
      && normalized.length <= 90)
}

const tagConfig: Record<SemanticTag, { label: string; className: string }> = {
  NEW_LEAD: {
    label: 'Nuevo prospecto',
    className: 'border-emerald-100 bg-emerald-50 text-emerald-700',
  },
  FOLLOW_UP: {
    label: 'Seguimiento',
    className: 'border-violet-100 bg-violet-50 text-violet-700',
  },
  BOOKING: {
    label: 'Reserva',
    className: 'border-blue-100 bg-blue-50 text-blue-700',
  },
  NO_REPLY: {
    label: 'Sin responder',
    className: 'border-amber-100 bg-amber-50 text-amber-700',
  },
  ORDER: {
    label: 'Pedido',
    className: 'border-emerald-100 bg-emerald-50 text-emerald-700',
  },
}

function getSemanticTag(conversation: ConversationSummaryResponse): SemanticTag {
  const text = `${conversation.customerName} ${conversation.lastMessagePreview ?? ''}`.toLowerCase()

  if (text.includes('pedido') || text.includes('comprar') || text.includes('masaje')) {
    return 'ORDER'
  }

  if (text.includes('reserva') || text.includes('cita') || text.includes('agendar') || text.includes('reagendar')) {
    return 'BOOKING'
  }

  if (text.includes('depil') || text.includes('lifting') || text.includes('cotizar')) {
    return 'FOLLOW_UP'
  }

  if (conversation.unreadCount > 0 || conversation.status === 'PENDING') {
    return 'NO_REPLY'
  }

  return 'NEW_LEAD'
}

function formatConversationTime(value: string | null) {
  if (!value) {
    return 'Sin actividad'
  }

  const date = dayjs(value)
  if (date.isSame(dayjs(), 'day')) {
    return date.format('HH:mm')
  }

  if (date.isSame(dayjs().subtract(1, 'day'), 'day')) {
    return 'Ayer'
  }

  return date.format('DD/MM')
}

function formatMessageTime(message: ConversationMessageResponse) {
  return dayjs(message.sentAt ?? message.receivedAt ?? message.failedAt ?? message.createdAt).format('HH:mm')
}

function getInitials(name: string) {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function getAvatarGradient(seed: string) {
  const gradients = [
    'from-rose-100 via-amber-50 to-slate-100 text-rose-700',
    'from-blue-100 via-cyan-50 to-slate-100 text-blue-700',
    'from-emerald-100 via-teal-50 to-slate-100 text-emerald-700',
    'from-violet-100 via-fuchsia-50 to-slate-100 text-violet-700',
    'from-orange-100 via-yellow-50 to-slate-100 text-orange-700',
  ]
  const index = seed.split('').reduce((total, char) => total + char.charCodeAt(0), 0) % gradients.length
  return gradients[index]
}

function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  const sizeClass = {
    sm: 'h-10 w-10 text-xs',
    md: 'h-12 w-12 text-sm',
    lg: 'h-14 w-14 text-base',
  }[size]

  return (
    <span
      className={[
        'inline-flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br font-semibold shadow-[0_10px_22px_rgba(15,23,42,0.10)] ring-2 ring-white',
        sizeClass,
        getAvatarGradient(name),
      ]
        .join(' ')
        .trim()}
    >
      {getInitials(name)}
    </span>
  )
}

function Icon({ name, className = 'h-4 w-4' }: { name: IconName; className?: string }) {
  const common = {
    className,
    fill: 'none',
    viewBox: '0 0 24 24',
    xmlns: 'http://www.w3.org/2000/svg',
  }

  switch (name) {
    case 'chat':
      return (
        <svg {...common}>
          <path d="M5 7.5C5 6.12 6.12 5 7.5 5H16.5C17.88 5 19 6.12 19 7.5V13.5C19 14.88 17.88 16 16.5 16H11L7 19V16H7.5C6.12 16 5 14.88 5 13.5V7.5Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'clock':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.8" />
          <path d="M12 7.8V12.4L15.2 14.2" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'user-plus':
      return (
        <svg {...common}>
          <path d="M10 12C12.21 12 14 10.21 14 8C14 5.79 12.21 4 10 4C7.79 4 6 5.79 6 8C6 10.21 7.79 12 10 12Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M3.8 19C3.8 16.6 6.57 14.8 10 14.8C11.23 14.8 12.37 15.03 13.32 15.43" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M18 10V16" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M15 13H21" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case 'cart':
      return (
        <svg {...common}>
          <path d="M7 7H19L17.5 13H9L7 5H5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
          <circle cx="10" cy="18" fill="currentColor" r="1.2" />
          <circle cx="17" cy="18" fill="currentColor" r="1.2" />
        </svg>
      )
    case 'calendar':
      return (
        <svg {...common}>
          <rect height="14" rx="3" stroke="currentColor" strokeWidth="1.8" width="14" x="5" y="6" />
          <path d="M8 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M16 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case 'sparkles':
      return (
        <svg {...common}>
          <path d="M12 3L13.4 7.2L17.5 8.5L13.4 9.8L12 14L10.6 9.8L6.5 8.5L10.6 7.2L12 3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" />
          <path d="M18 14L18.8 16.2L21 17L18.8 17.8L18 20L17.2 17.8L15 17L17.2 16.2L18 14Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" />
        </svg>
      )
    case 'send':
      return (
        <svg {...common}>
          <path d="M20 4L10.5 13.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
          <path d="M20 4L14 20L10.5 13.5L4 10L20 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'paperclip':
      return (
        <svg {...common}>
          <path d="M8 12.5L13.8 6.7C15.2 5.3 17.4 5.3 18.8 6.7C20.2 8.1 20.2 10.3 18.8 11.7L11 19.5C8.9 21.6 5.5 21.6 3.4 19.5C1.3 17.4 1.3 14 3.4 11.9L11.2 4.1" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'bookmark':
      return (
        <svg {...common}>
          <path d="M7 5H17V19L12 16.5L7 19V5Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'bolt':
      return (
        <svg {...common}>
          <path d="M13 3L5.5 13H11L10 21L18.5 10H13L13 3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'filter':
      return (
        <svg {...common}>
          <path d="M5 7H19" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M8 12H16" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M10 17H14" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case 'whatsapp':
      return (
        <svg {...common}>
          <path d="M7.2 18.2L4.8 19L5.6 16.7C4.6 15.4 4 13.8 4 12C4 7.6 7.6 4 12 4C16.4 4 20 7.6 20 12C20 16.4 16.4 20 12 20C10.2 20 8.5 19.3 7.2 18.2Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
          <path d="M9 8.8C9.4 11.4 11.6 13.6 14.2 14L15.2 12.8L13.4 11.8L12.6 12.5C11.6 12 10.9 11.3 10.5 10.4L11.2 9.6L10.2 7.8L9 8.8Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4" />
        </svg>
      )
    case 'dots':
      return (
        <svg {...common}>
          <circle cx="12" cy="5" fill="currentColor" r="1.4" />
          <circle cx="12" cy="12" fill="currentColor" r="1.4" />
          <circle cx="12" cy="19" fill="currentColor" r="1.4" />
        </svg>
      )
    case 'smile':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.8" />
          <circle cx="9" cy="10" fill="currentColor" r="1" />
          <circle cx="15" cy="10" fill="currentColor" r="1" />
          <path d="M8.8 14.2C9.7 15.4 10.8 16 12 16C13.2 16 14.3 15.4 15.2 14.2" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case 'search':
      return (
        <svg {...common}>
          <circle cx="11" cy="11" r="5.5" stroke="currentColor" strokeWidth="1.8" />
          <path d="M15.2 15.2L20 20" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case 'check':
      return (
        <svg {...common}>
          <path d="M5 12.5L9 16.5L19 6.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      )
    case 'user':
      return (
        <svg {...common}>
          <path d="M12 12C14.21 12 16 10.21 16 8C16 5.79 14.21 4 12 4C9.79 4 8 5.79 8 8C8 10.21 9.79 12 12 12Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M5 19C5 16.79 8.13 15 12 15C15.87 15 19 16.79 19 19" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    default:
      return null
  }
}

function CustomerTagBadge({ tag }: { tag: ConversationCustomerTag }) {
  const className: Record<ConversationCustomerTag, string> = {
    NEW: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    CUSTOMER: 'border-blue-100 bg-blue-50 text-blue-700',
    VIP: 'border-violet-100 bg-violet-50 text-violet-700',
    LEAD: 'border-slate-200 bg-slate-100 text-slate-700',
    UNKNOWN: 'border-slate-200 bg-white text-slate-500',
  }

  return (
    <span className={["rounded-lg border px-2.5 py-1 text-[11px] font-semibold", className[tag]].join(' ')}>
      {translateCustomerTag(tag)}
    </span>
  )
}

function ConversationStatusPill({ status, unreadCount }: { status: string; unreadCount: number }) {
  const tone = getConversationStatusTone(status, unreadCount)
  const className = {
    unread: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    progress: 'border-blue-100 bg-blue-50 text-blue-700',
    pending: 'border-orange-100 bg-orange-50 text-orange-700',
    resolved: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    archived: 'border-slate-200 bg-slate-100 text-slate-600',
    neutral: 'border-slate-200 bg-white text-slate-600',
  }[tone]

  const label = unreadCount > 0 ? 'No leido' : translateConversationStatus(status)

  return (
    <span className={["inline-flex rounded-lg border px-3 py-1 text-xs font-bold", className].join(' ')}>
      {label}
    </span>
  )
}

type AssigneeOption = ReturnType<typeof getConversationAssignees>[number]
type LocationOption = { id: string; name: string }

type InboxViewProps = {
  conversations: ConversationSummaryResponse[]
  visibleConversations: ConversationSummaryResponse[]
  paginatedConversations: ConversationSummaryResponse[]
  counts: Record<ConversationTab, number>
  activeTab: ConversationTab
  setActiveTab: (tab: ConversationTab) => void
  search: string
  setSearch: (value: string) => void
  statusFilter: string
  setStatusFilter: (value: string) => void
  assigneeFilter: string
  setAssigneeFilter: (value: string) => void
  locationFilter: string
  setLocationFilter: (value: string) => void
  locationOptions: LocationOption[]
  tagFilter: string
  setTagFilter: (value: string) => void
  assignees: AssigneeOption[]
  selectedIds: string[]
  setSelectedIds: (ids: string[]) => void
  page: number
  setPage: (value: number) => void
  totalPages: number
  pageStart: number
  pageEnd: number
  rowsPerPage: number
  setRowsPerPage: (value: number) => void
  isLoading: boolean
  isError: boolean
  isFetching: boolean
  onRetry: () => void
  onOpenConversation: (conversationId: string) => void
}

function InboxView({
  conversations,
  visibleConversations,
  paginatedConversations,
  counts,
  activeTab,
  setActiveTab,
  search,
  setSearch,
  statusFilter,
  setStatusFilter,
  assigneeFilter,
  setAssigneeFilter,
  locationFilter,
  setLocationFilter,
  locationOptions,
  tagFilter,
  setTagFilter,
  assignees,
  selectedIds,
  setSelectedIds,
  page,
  setPage,
  totalPages,
  pageStart,
  pageEnd,
  rowsPerPage,
  setRowsPerPage,
  isLoading,
  isError,
  isFetching,
  onRetry,
  onOpenConversation,
}: InboxViewProps) {
  const allVisibleSelected = paginatedConversations.length > 0
    && paginatedConversations.every((conversation) => selectedIds.includes(conversation.id))
  const statusOptions = ['UNREAD', 'IN_PROGRESS', 'PENDING', 'RESOLVED', 'ARCHIVED', 'OPEN', 'CLOSED', 'ASSIGNED']
  const tagOptions: ConversationCustomerTag[] = ['NEW', 'CUSTOMER', 'VIP', 'LEAD', 'UNKNOWN']

  const toggleRow = (conversationId: string) => {
    setSelectedIds(
      selectedIds.includes(conversationId)
        ? selectedIds.filter((id) => id !== conversationId)
        : [...selectedIds, conversationId],
    )
  }

  const toggleVisibleRows = () => {
    if (allVisibleSelected) {
      setSelectedIds(selectedIds.filter((id) => !paginatedConversations.some((conversation) => conversation.id === id)))
      return
    }

    setSelectedIds(Array.from(new Set([...selectedIds, ...paginatedConversations.map((conversation) => conversation.id)])))
  }

  return (
    <section className="flex min-h-[calc(100dvh-64px)] flex-col gap-4 overflow-hidden">
      <div className="shrink-0">
        <h1 className="text-[clamp(1.45rem,2.6vw,2.15rem)] font-semibold tracking-[-0.03em] text-[var(--color-text)]">
          Conversaciones
        </h1>
        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
          Gestiona y responde tus conversaciones por WhatsApp
        </p>
      </div>

      <div className="grid shrink-0 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {tabs.map((tab) => {
          const active = activeTab === tab.key
          return (
            <button
              key={tab.key}
              className={[
                'flex items-center justify-between rounded-[16px] border px-4 py-3 text-left text-sm font-bold transition',
                active
                  ? 'border-emerald-100 bg-emerald-50 text-emerald-700 shadow-[0_12px_30px_rgba(16,185,129,0.14)]'
                  : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-emerald-100 hover:bg-emerald-50/40',
              ].join(' ')}
              onClick={() => setActiveTab(tab.key)}
              type="button"
            >
              <span>{tab.label}</span>
              <span className={[
                'inline-flex h-7 min-w-7 items-center justify-center rounded-full px-2 text-xs font-extrabold',
                active ? 'bg-emerald-500 text-white' : 'bg-slate-100 text-slate-600',
              ].join(' ')}>
                {counts[tab.key]}
              </span>
            </button>
          )
        })}
      </div>

      <Card className="flex min-h-0 flex-1 flex-col overflow-hidden p-0">
        <div className="shrink-0 border-b border-[var(--color-border)] px-4 py-4 lg:px-5">
          <div className="grid gap-3 xl:grid-cols-[minmax(240px,1fr)_170px_170px_190px_180px_120px]">
            <label className="relative block min-w-0">
              <span className="sr-only">Buscar conversaciones</span>
              <Icon className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" name="search" />
              <input
                className="h-11 w-full rounded-[14px] border border-[var(--color-border)] bg-white pl-11 pr-4 text-sm text-[var(--color-text)] outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Buscar conversaciones..."
                type="search"
                value={search}
              />
            </label>

            <select
              className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none"
              onChange={(event) => setLocationFilter(event.target.value)}
              value={locationFilter}
            >
              <option value="ALL">Sucursal: Todas</option>
              <option value="NO_LOCATION">Sin sucursal</option>
              {locationOptions.map((location) => (
                <option key={location.id} value={location.id}>{location.name}</option>
              ))}
            </select>

            <select
              className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none"
              onChange={(event) => setStatusFilter(event.target.value)}
              value={statusFilter}
            >
              <option value="ALL">Estado: Todos</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>{translateConversationStatus(status)}</option>
              ))}
            </select>

            <select
              className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none"
              onChange={(event) => setAssigneeFilter(event.target.value)}
              value={assigneeFilter}
            >
              <option value="ALL">Asignado a: Todos</option>
              <option value="UNASSIGNED">Sin asignar</option>
              {assignees.map((assignee) => (
                <option key={assignee.id} value={assignee.id}>{assignee.name}</option>
              ))}
            </select>

            <select
              className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none"
              onChange={(event) => setTagFilter(event.target.value)}
              value={tagFilter}
            >
              <option value="ALL">Etiquetas: Todas</option>
              {tagOptions.map((tag) => (
                <option key={tag} value={tag}>{translateCustomerTag(tag)}</option>
              ))}
            </select>

            <button
              className="inline-flex h-11 items-center justify-center gap-2 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50"
              type="button"
            >
              <Icon name="filter" />
              Filtros
            </button>
          </div>
        </div>

        {isLoading ? (
          <div className="p-6">
            <LoadingState message="Cargando bandeja de conversaciones WhatsApp." variant="table" />
          </div>
        ) : null}

        {isError ? (
          <div className="p-6">
            <ErrorState
              description="No se pudieron cargar las conversaciones. Reintenta para sincronizar la bandeja."
              onRetry={onRetry}
              title="No se pudieron cargar las conversaciones"
              variant="card"
            />
          </div>
        ) : null}

        {!isLoading && !isError && visibleConversations.length === 0 ? (
          <div className="p-6">
            <EmptyState
              description={conversations.length === 0 ? 'Todavia no hay conversaciones recibidas por WhatsApp.' : 'No hay conversaciones para los filtros seleccionados.'}
              title="Sin conversaciones"
              variant="card"
            />
          </div>
        ) : null}

        {visibleConversations.length > 0 ? (
          <>
            <div className="hidden min-h-0 flex-1 overflow-y-auto lg:block">
              <div className="grid grid-cols-[44px_minmax(220px,1.25fr)_minmax(260px,1.45fr)_minmax(150px,0.85fr)_minmax(120px,0.7fr)_90px] items-center border-b border-[var(--color-border)] bg-white px-4 py-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">
                <label className="flex items-center justify-center">
                  <input
                    aria-label="Seleccionar conversaciones visibles"
                    checked={allVisibleSelected}
                    className="h-4 w-4 rounded border-slate-300"
                    onChange={toggleVisibleRows}
                    type="checkbox"
                  />
                </label>
                <span>Contacto</span>
                <span>Ultimo mensaje</span>
                <span>Asignado a</span>
                <span>Estado</span>
                <span>Hora</span>
              </div>

              {paginatedConversations.map((conversation) => {
                const customerTag = inferConversationCustomerTag(conversation)
                const selected = selectedIds.includes(conversation.id)
                return (
                  <div
                    key={conversation.id}
                    className={[
                      'grid cursor-pointer grid-cols-[44px_minmax(220px,1.25fr)_minmax(260px,1.45fr)_minmax(150px,0.85fr)_minmax(120px,0.7fr)_90px] items-center border-b border-[var(--color-border)] px-4 py-3.5 transition hover:bg-slate-50',
                      conversation.unreadCount > 0 ? 'bg-emerald-50/35' : 'bg-white',
                    ].join(' ')}
                    onClick={() => onOpenConversation(conversation.id)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        onOpenConversation(conversation.id)
                      }
                    }}
                  >
                    <label className="flex items-center justify-center" onClick={(event) => event.stopPropagation()}>
                      <input
                        aria-label={`Seleccionar ${conversation.customerName}`}
                        checked={selected}
                        className="h-4 w-4 rounded border-slate-300"
                        onChange={() => toggleRow(conversation.id)}
                        type="checkbox"
                      />
                    </label>

                    <div className="flex min-w-0 items-center gap-3">
                      <div className="relative">
                        <Avatar name={conversation.customerName} size="sm" />
                        <span className="absolute -bottom-1 -right-1 inline-flex h-4 w-4 items-center justify-center rounded-full bg-emerald-500 text-white ring-2 ring-white">
                          <Icon className="h-2.5 w-2.5" name="whatsapp" />
                        </span>
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="truncate text-sm font-bold text-slate-900">{conversation.customerName}</p>
                          <CustomerTagBadge tag={customerTag} />
                        </div>
                        <p className="mt-1 truncate text-xs text-slate-500">{conversation.customerPhone}</p>
                        {conversation.locationName ? (
                          <p className="mt-1 truncate text-xs font-semibold text-blue-600">Sede: {conversation.locationName}</p>
                        ) : null}
                      </div>
                    </div>

                    <div className="min-w-0 pr-4">
                      <div className="flex items-center gap-2">
                        <span className={conversation.unreadCount > 0 ? 'h-2.5 w-2.5 rounded-full bg-emerald-500' : 'text-emerald-500'}>
                          {conversation.unreadCount > 0 ? null : <Icon className="h-4 w-4" name="check" />}
                        </span>
                        <p className="truncate text-sm font-medium text-slate-700">{conversation.lastMessagePreview ?? 'Sin mensajes todavia.'}</p>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">{formatConversationTime(conversation.lastMessageAt)}</p>
                    </div>

                    <div className="flex min-w-0 items-center gap-2">
                      {conversation.assignedUserName ? <Avatar name={conversation.assignedUserName} size="sm" /> : null}
                      <span className="truncate text-sm text-slate-700">{conversation.assignedUserName ?? 'Sin asignar'}</span>
                    </div>

                    <ConversationStatusPill status={conversation.status} unreadCount={conversation.unreadCount} />

                    <div className="flex items-center justify-between gap-2 text-sm text-slate-600">
                      <span>{formatConversationTime(conversation.lastMessageAt)}</span>
                      {conversation.unreadCount > 0 ? (
                        <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-emerald-500 px-1.5 text-xs font-extrabold text-white">
                          {conversation.unreadCount}
                        </span>
                      ) : null}
                    </div>
                  </div>
                )
              })}
            </div>

            <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4 lg:hidden">
              {paginatedConversations.map((conversation) => {
                const customerTag = inferConversationCustomerTag(conversation)
                return (
                  <button
                    key={conversation.id}
                    className="w-full rounded-[18px] border border-[var(--color-border)] bg-white p-4 text-left shadow-[0_12px_30px_rgba(15,23,42,0.05)]"
                    onClick={() => onOpenConversation(conversation.id)}
                    type="button"
                  >
                    <div className="flex items-start gap-3">
                      <Avatar name={conversation.customerName} size="sm" />
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="font-bold text-slate-900">{conversation.customerName}</p>
                          <CustomerTagBadge tag={customerTag} />
                        </div>
                        <p className="mt-1 text-xs text-slate-500">{conversation.assignedUserName ?? 'Sin asignar'} - {formatConversationTime(conversation.lastMessageAt)}</p>
                        {conversation.locationName ? (
                          <p className="mt-1 text-xs font-semibold text-blue-600">Sede: {conversation.locationName}</p>
                        ) : null}
                      </div>
                      {conversation.unreadCount > 0 ? (
                        <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-emerald-500 px-1.5 text-xs font-extrabold text-white">
                          {conversation.unreadCount}
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-3 line-clamp-2 text-sm text-slate-600">{conversation.lastMessagePreview ?? 'Sin mensajes todavia.'}</p>
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                      <ConversationStatusPill status={conversation.status} unreadCount={conversation.unreadCount} />
                      <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-600">
                        <Icon className="h-3.5 w-3.5" name="whatsapp" />
                        WhatsApp
                      </span>
                    </div>
                  </button>
                )
              })}
            </div>

            <div className="flex shrink-0 flex-col gap-3 border-t border-[var(--color-border)] bg-slate-50 px-4 py-3 md:flex-row md:items-center md:justify-between">
              <p className="text-sm text-slate-600">
                Mostrando {visibleConversations.length === 0 ? 0 : pageStart + 1} a {pageEnd} de {visibleConversations.length} conversaciones
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <div className="inline-flex items-center gap-2">
                  <Button
                    disabled={page === 0 || isFetching}
                    onClick={() => setPage(Math.max(page - 1, 0))}
                    size="sm"
                    variant="secondary"
                  >
                    Anterior
                  </Button>
                  <span className="inline-flex h-9 min-w-9 items-center justify-center rounded-[12px] bg-[var(--color-primary)] px-3 text-sm font-bold text-white">
                    {page + 1}
                  </span>
                  <Button
                    disabled={page >= totalPages - 1 || isFetching}
                    onClick={() => setPage(Math.min(page + 1, totalPages - 1))}
                    size="sm"
                    variant="secondary"
                  >
                    Siguiente
                  </Button>
                </div>
                <label className="flex items-center gap-2 text-sm text-slate-600">
                  Filas por pagina:
                  <select
                    className="h-9 rounded-[12px] border border-[var(--color-border)] bg-white px-3 text-sm font-semibold"
                    onChange={(event) => setRowsPerPage(Number(event.target.value))}
                    value={rowsPerPage}
                  >
                    {[7, 10, 15, 20].map((size) => (
                      <option key={size} value={size}>{size}</option>
                    ))}
                  </select>
                </label>
              </div>
            </div>
          </>
        ) : null}
      </Card>
    </section>
  )
}

function MessageBubble({ message }: { message: ConversationMessageResponse }) {
  const outbound = message.direction === 'OUTBOUND'

  return (
    <div className={["flex items-end gap-2", outbound ? 'justify-end' : 'justify-start'].join(' ')}>
      <div
        className={[
          'max-w-[72%] rounded-2xl px-4 py-3 text-sm leading-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]',
          outbound
            ? 'rounded-br-md border border-blue-100 bg-blue-50 text-[var(--color-text)]'
            : 'rounded-bl-md border border-[var(--color-border)] bg-white text-[var(--color-text)]',
        ]
          .join(' ')
          .trim()}
      >
        <p>{message.body}</p>
        <div className="mt-2 flex items-center justify-end gap-2 text-[11px] text-slate-500">
          <span>{formatMessageTime(message)}</span>
          {outbound ? <Icon className="h-3.5 w-3.5 text-[var(--color-primary)]" name="check" /> : null}
        </div>
      </div>
    </div>
  )
}

export function ConversationsPage() {
  const { conversationId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const { user } = useShellSession()
  const isOnline = useOnlineStatus()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [activeTab, setActiveTab] = useState<ConversationTab>('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [assigneeFilter, setAssigneeFilter] = useState('ALL')
  const [locationFilter, setLocationFilter] = useState('ALL')
  const [tagFilter, setTagFilter] = useState('ALL')
  const [rowsPerPage, setRowsPerPage] = useState(CONVERSATION_INBOX_DEFAULT_PAGE_SIZE)
  const [selectedConversationIds, setSelectedConversationIds] = useState<string[]>([])
  const [messageBody, setMessageBody] = useState('')
  const [composerConversationId, setComposerConversationId] = useState<string | null>(conversationId ?? null)
  const [closeDialogOpen, setCloseDialogOpen] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setPage(0)
      setDebouncedSearch(search.trim())
    }, 250)
    return () => window.clearTimeout(timeoutId)
  }, [search])

  const conversationsQuery = useQuery({
    queryKey: ['conversations', 'list', debouncedSearch],
    queryFn: () =>
      getConversationsRequest({
        page: 0,
        size: 100,
        search: debouncedSearch || undefined,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 15_000 : false,
  })

  const detailQuery = useQuery({
    queryKey: ['conversations', 'detail', conversationId],
    queryFn: () => getConversationDetailRequest(conversationId ?? ''),
    enabled: Boolean(conversationId),
    refetchInterval: isOnline ? 15_000 : false,
  })

  if ((conversationId ?? null) !== composerConversationId) {
    setComposerConversationId(conversationId ?? null)
    setMessageBody('')
  }

  const markReadMutation = useMutation({
    mutationFn: markConversationReadRequest,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['conversations', 'list'] }),
        queryClient.invalidateQueries({ queryKey: ['conversations', 'metrics'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      ])
    },
  })

  useEffect(() => {
    if (conversationId && detailQuery.data?.unreadCount && detailQuery.data.unreadCount > 0) {
      markReadMutation.mutate(conversationId)
    }
  }, [conversationId, detailQuery.data?.unreadCount, markReadMutation])

  const sendMessageMutation = useMutation({
    mutationFn: async (payload: { body: string; aiSource?: string }) => {
      if (!conversationId) {
        throw new Error('No hay conversacion seleccionada.')
      }

      return sendConversationMessageRequest(conversationId, {
        body: payload.body.trim(),
        aiSource: payload.aiSource,
      })
    },
    onSuccess: async () => {
      setMessageBody('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['conversations'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      ])
      showToast({
        title: 'Mensaje enviado',
        description: 'La respuesta quedo registrada en el hilo de atencion.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos enviar el mensaje',
        description: 'Revisa la conexion del canal y vuelve a intentarlo.',
        tone: 'error',
      })
    },
  })

  const assignMutation = useMutation({
    mutationFn: async () => {
      if (!conversationId || !user) {
        throw new Error('No hay usuario autenticado.')
      }
      return assignConversationRequest(conversationId, { userId: user.id })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        title: 'Conversacion asignada',
        description: 'Ahora la conversacion aparece bajo tu responsabilidad.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo asignar la conversacion',
        description: 'Intenta nuevamente en unos segundos.',
        tone: 'error',
      })
    },
  })

  const aiReplyMutation = useMutation({
    mutationFn: async () => {
      if (!conversationId) {
        throw new Error('No hay conversacion seleccionada.')
      }
      return previewAiReplyRequest(conversationId)
    },
    onSuccess: (response) => {
      setMessageBody(response.suggestedBody)
      showToast({
        title: 'Respuesta IA preparada',
        description: response.source.includes('BOOKING_PREVIEW') ? 'Al enviarla se creara la reserva temporal real y el enlace de confirmacion.' : 'Revisa la sugerencia antes de enviarla por WhatsApp.',
        tone: 'success',
      })
    },
    onError: () => {
      const fallback = detailQuery.data?.customer.displayName
        ? `Hola ${detailQuery.data.customer.displayName.split(' ')[0]}, gracias por escribirnos. Te ayudo de inmediato.`
        : 'Hola, gracias por escribirnos. Te ayudo de inmediato.'
      setMessageBody(fallback)
      showToast({
        title: 'Respuesta IA local aplicada',
        description: 'No pudimos consultar el servidor, pero se cargo una respuesta sugerida base.',
        tone: 'warning',
      })
    },
  })

  const closeMutation = useMutation({
    mutationFn: async () => {
      if (!conversationId) {
        throw new Error('No hay conversacion seleccionada.')
      }
      return closeConversationRequest(conversationId)
    },
    onSuccess: async () => {
      setCloseDialogOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        title: 'Conversacion cerrada',
        description: 'El hilo quedo marcado como cerrado.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo cerrar la conversacion',
        description: 'La accion no pudo completarse. Reintenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const reopenMutation = useMutation({
    mutationFn: async () => {
      if (!conversationId) {
        throw new Error('No hay conversacion seleccionada.')
      }
      return reopenConversationRequest(conversationId)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        title: 'Conversacion reabierta',
        description: 'Ya puedes continuar la atencion desde el hilo.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo reabrir la conversacion',
        description: 'La accion no pudo completarse. Reintenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const conversations = useMemo(
    () => dedupeById(conversationsQuery.data?.items ?? []),
    [conversationsQuery.data?.items],
  )
  const inboxCounts = useMemo(() => getConversationInboxCounts(conversations), [conversations])
  const assigneeOptions = useMemo(() => getConversationAssignees(conversations), [conversations])
  const locationOptions = useMemo<LocationOption[]>(() => {
    const items = new Map<string, LocationOption>()
    conversations.forEach((conversation) => {
      if (!conversation.locationName) {
        return
      }
      const id = conversation.locationId ?? conversation.locationName
      items.set(id, { id, name: conversation.locationName })
    })
    return Array.from(items.values()).sort((left, right) => left.name.localeCompare(right.name))
  }, [conversations])

  const visibleConversations = useMemo(() => {
    const baseConversations = filterConversations(conversations, {
      category: activeTab,
      status: statusFilter,
      assignee: assigneeFilter,
      tag: tagFilter,
      search: debouncedSearch,
    })

    if (locationFilter === 'ALL') {
      return baseConversations
    }

    if (locationFilter === 'NO_LOCATION') {
      return baseConversations.filter((conversation) => !conversation.locationId && !conversation.locationName)
    }

    return baseConversations.filter((conversation) => (conversation.locationId ?? conversation.locationName) === locationFilter)
  }, [activeTab, assigneeFilter, conversations, debouncedSearch, locationFilter, statusFilter, tagFilter])

  const handleActiveTabChange = useCallback((tab: ConversationTab) => {
    setActiveTab(tab)
    setPage(0)
  }, [])

  const handleStatusFilterChange = useCallback((value: string) => {
    setStatusFilter(value)
    setPage(0)
  }, [])

  const handleAssigneeFilterChange = useCallback((value: string) => {
    setAssigneeFilter(value)
    setPage(0)
  }, [])

  const handleLocationFilterChange = useCallback((value: string) => {
    setLocationFilter(value)
    setPage(0)
  }, [])

  const handleTagFilterChange = useCallback((value: string) => {
    setTagFilter(value)
    setPage(0)
  }, [])

  const handleRowsPerPageChange = useCallback((value: number) => {
    setRowsPerPage(value)
    setPage(0)
  }, [])

  const inboxPage = paginateConversations(visibleConversations, page, rowsPerPage)
  const selectedConversation = detailQuery.data
  const selectedAssignedToCurrentUser = Boolean(user && selectedConversation?.assignedUserId === user.id)
  const messageScrollSignature = selectedConversation?.messages.map((message) => message.id).join('|') ?? ''

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' })
  }, [conversationId, messageScrollSignature])

  const sendMessage = async () => {
    if (!messageBody.trim()) {
      showToast({
        title: 'Mensaje vacio',
        description: 'Escribe una respuesta o usa un atajo antes de enviar.',
        tone: 'warning',
      })
      return
    }

    if (!isOnline) {
      showToast({
        title: 'Sin conexion',
        description: 'Reconecta tu navegador antes de enviar el mensaje.',
        tone: 'warning',
      })
      return
    }

    const preparedAiReply = aiReplyMutation.data
    const preparedBody = preparedAiReply?.suggestedBody.trim()
    const currentBody = messageBody.trim()
    const preparedIsBookingPreview = preparedAiReply?.source.includes('BOOKING_PREVIEW') ?? false
    const shouldSendPreparedBookingPreview = Boolean(
      preparedAiReply
        && preparedIsBookingPreview
        && preparedBody
        && (currentBody === preparedBody || isGenericAiGreetingBody(currentBody)),
    )
    const bodyToSend = shouldSendPreparedBookingPreview ? preparedBody! : messageBody
    const aiSource = preparedAiReply && (bodyToSend.trim() === preparedBody || shouldSendPreparedBookingPreview)
      ? preparedAiReply.source
      : undefined

    await sendMessageMutation.mutateAsync({ body: bodyToSend, aiSource })
  }

  if (!conversationId) {
    return (
      <InboxView
        activeTab={activeTab}
        assigneeFilter={assigneeFilter}
        assignees={assigneeOptions}
        conversations={conversations}
        counts={inboxCounts}
        isError={conversationsQuery.isError && !conversationsQuery.data}
        isFetching={conversationsQuery.isFetching}
        isLoading={conversationsQuery.isPending && !conversationsQuery.data}
        locationFilter={locationFilter}
        locationOptions={locationOptions}
        onOpenConversation={(id) => navigate(`/conversations/${id}`)}
        onRetry={() => void conversationsQuery.refetch()}
        page={inboxPage.currentPage}
        pageEnd={inboxPage.end}
        pageStart={inboxPage.start}
        paginatedConversations={inboxPage.items}
        rowsPerPage={rowsPerPage}
        search={search}
        selectedIds={selectedConversationIds}
        setActiveTab={handleActiveTabChange}
        setAssigneeFilter={handleAssigneeFilterChange}
        setLocationFilter={handleLocationFilterChange}
        setPage={setPage}
        setRowsPerPage={handleRowsPerPageChange}
        setSearch={setSearch}
        setSelectedIds={setSelectedConversationIds}
        setStatusFilter={handleStatusFilterChange}
        setTagFilter={handleTagFilterChange}
        statusFilter={statusFilter}
        tagFilter={tagFilter}
        totalPages={inboxPage.totalPages}
        visibleConversations={visibleConversations}
      />
    )
  }

  return (
    <section className="flex min-h-[calc(100dvh-64px)] flex-col gap-4 overflow-hidden">
      <div className="shrink-0">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <Button
              onClick={() => navigate('/conversations')}
              size="sm"
              variant="secondary"
            >
              Volver a bandeja
            </Button>
            <h1 className="mt-3 text-[clamp(1.45rem,2.6vw,2.15rem)] font-semibold tracking-[-0.03em] text-[var(--color-text)]">
              Detalle de conversacion
            </h1>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              Responde y gestiona el hilo seleccionado desde WhatsApp.
            </p>
          </div>
          <Button
            leadingIcon={<span className="text-lg leading-none">+</span>}
            onClick={() => navigate('/conversations/new')}
            variant="secondary"
          >
            Nueva conversacion
          </Button>
        </div>
      </div>

      {!isOnline ? (
        <Card className="shrink-0 border-amber-200 bg-amber-50 p-4">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexion</p>
          <p className="mt-1 text-sm leading-6 text-amber-800">
            El modulo sigue visible con los ultimos datos cacheados, pero no podra enviar mensajes ni sincronizar cambios hasta recuperar internet.
          </p>
        </Card>
      ) : null}

      <div className="min-h-0 flex-1 overflow-hidden">
        {detailQuery.isPending && !selectedConversation ? (
          <LoadingState message="Preparando el hilo de la conversacion." variant="detail" />
        ) : detailQuery.isError && !selectedConversation ? (
          <ErrorState
            description="No pudimos abrir el detalle de esta conversacion. Reintenta desde la bandeja."
            onRetry={() => void detailQuery.refetch()}
            title="No fue posible abrir la conversacion"
          />
        ) : selectedConversation && user ? (
          <Card className="flex h-full min-h-0 flex-col overflow-hidden p-0">
            <div className="shrink-0 border-b border-[var(--color-border)] px-6 py-5">
              <div className="flex flex-col gap-4 2xl:flex-row 2xl:items-center 2xl:justify-between">
              <div className="flex items-center gap-4">
                <Avatar name={selectedConversation.customer.displayName} size="lg" />
                <div>
                  <h2 className="text-xl font-semibold text-[var(--color-text)]">{selectedConversation.customer.displayName}</h2>
                  <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-secondary)]">
                    <span className="inline-flex items-center gap-1 text-emerald-600">
                      <Icon className="h-4 w-4" name="whatsapp" />
                      WhatsApp
                    </span>
                    <span>{selectedConversation.customer.phone}</span>
                    {selectedConversation.locationName ? (
                      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-700">
                        Sucursal: {selectedConversation.locationName}
                      </span>
                    ) : (
                      <span className="inline-flex rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-700">
                        Sin sucursal
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                {!selectedAssignedToCurrentUser ? (
                  <Button
                    disabled={!isOnline}
                    leadingIcon={<Icon name="user" />}
                    loading={assignMutation.isPending}
                    onClick={() => void assignMutation.mutateAsync()}
                    size="sm"
                    variant="secondary"
                  >
                    Asignarme
                  </Button>
                ) : null}
                {selectedConversation.prospectId ? (
                  <Button
                    leadingIcon={<Icon name="user-plus" />}
                    onClick={() => navigate(`/prospects/${selectedConversation.prospectId}`)}
                    size="sm"
                    variant="secondary"
                  >
                    Ver prospecto
                  </Button>
                ) : (
                  <Button
                    leadingIcon={<Icon name="user-plus" />}
                    onClick={() => navigate(`/conversations/${selectedConversation.id}/prospects/new`)}
                    size="sm"
                    variant="secondary"
                  >
                    Crear prospecto
                  </Button>
                )}
                <Button
                  leadingIcon={<Icon name="calendar" />}
                  onClick={() => navigate(`/conversations/${selectedConversation.id}/appointments/new`)}
                  size="sm"
                  variant="secondary"
                >
                  Agendar cita
                </Button>
                <Button
                  leadingIcon={<Icon name="cart" />}
                  onClick={() => navigate(`/conversations/${selectedConversation.id}/orders/new`)}
                  size="sm"
                  variant="secondary"
                >
                  Crear pedido
                </Button>
                <Button
                  leadingIcon={<Icon name="sparkles" />}
                  loading={aiReplyMutation.isPending}
                  onClick={() => void aiReplyMutation.mutateAsync()}
                  size="sm"
                  variant="secondary"
                >
                  Responder con IA
                </Button>
                {selectedConversation.status === 'CLOSED' ? (
                  <Button
                    disabled={!isOnline}
                    loading={reopenMutation.isPending}
                    onClick={() => void reopenMutation.mutateAsync()}
                    size="sm"
                    variant="secondary"
                  >
                    Reabrir
                  </Button>
                ) : (
                  <button
                    aria-label="Cerrar conversacion"
                    className="inline-flex h-10 w-10 items-center justify-center rounded-[14px] text-slate-500 transition hover:bg-slate-100"
                    onClick={() => setCloseDialogOpen(true)}
                    type="button"
                  >
                    <Icon name="dots" />
                  </button>
                )}
              </div>
              </div>
            </div>

            <div className="shrink-0 border-b border-amber-100 bg-amber-50/70 px-6 py-3">
              <div className="flex items-center justify-between gap-3 rounded-[14px] border border-amber-100 bg-white/70 px-4 py-2 text-sm text-amber-800">
                <span className="inline-flex items-center gap-2">
                  <Icon className="h-4 w-4" name="user-plus" />
                  {tagConfig[getSemanticTag({
                    id: selectedConversation.id,
                    customerName: selectedConversation.customer.displayName,
                    customerPhone: selectedConversation.customer.phone,
                    status: selectedConversation.status,
                    unreadCount: selectedConversation.unreadCount,
                    lastMessagePreview: selectedConversation.lastMessagePreview,
                    lastMessageAt: selectedConversation.lastMessageAt,
                    channelType: selectedConversation.channelType,
                    assignedUserId: selectedConversation.assignedUserId,
                    assignedUserName: selectedConversation.assignedUserName,
                    prospectId: selectedConversation.prospectId,
                    locationId: selectedConversation.locationId,
                    locationName: selectedConversation.locationName,
                  })].label} - Primera interaccion
                </span>
                <span>{selectedConversation.lastMessageAt ? formatConversationTime(selectedConversation.lastMessageAt) : '--'}</span>
              </div>
            </div>

            <div className="flex min-h-0 flex-1 flex-col overflow-hidden bg-gradient-to-b from-white to-slate-50">
              <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-5">
                {selectedConversation.messages.length > 0 ? (
                  <>
                    {selectedConversation.messages.map((message) => (
                      <MessageBubble key={message.id} message={message} />
                    ))}
                    <div className="h-px scroll-mb-5" ref={messagesEndRef} />
                  </>
                ) : (
                  <div className="rounded-2xl border border-dashed border-[var(--color-border)] bg-white p-6 text-center text-sm text-[var(--color-text-secondary)]">
                    Todavia no hay mensajes en este hilo.
                  </div>
                )}
              </div>

              <div className="shrink-0 border-t border-[var(--color-border)] bg-white px-6 py-4">
                {aiReplyMutation.data ? (
                  <div className="mb-3 rounded-[16px] border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                    Sugerencia IA aplicada. Confianza estimada: {Math.round(aiReplyMutation.data.confidence * 100)}%.
                  </div>
                ) : null}

                <div className="flex items-end gap-3 rounded-[18px] border border-[var(--color-border)] bg-white px-4 py-3 shadow-[0_15px_40px_rgba(15,23,42,0.06)]">
                  <button className="mb-1 text-slate-500 transition hover:text-[var(--color-primary)]" type="button">
                    <Icon className="h-5 w-5" name="smile" />
                  </button>
                  <textarea
                    className="min-h-[44px] flex-1 resize-none border-0 bg-transparent py-2 text-sm text-[var(--color-text)] outline-none placeholder:text-slate-400"
                    onChange={(event) => setMessageBody(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' && !event.shiftKey) {
                        event.preventDefault()
                        void sendMessage()
                      }
                    }}
                    placeholder="Escribe una respuesta..."
                    rows={1}
                    value={messageBody}
                  />
                  <div className="mb-1 flex items-center gap-2 text-slate-500">
                    <button className="transition hover:text-[var(--color-primary)]" type="button"><Icon name="paperclip" /></button>
                    <button className="transition hover:text-[var(--color-primary)]" type="button"><Icon name="bookmark" /></button>
                    <button
                      className="transition hover:text-[var(--color-primary)]"
                      onClick={() => void aiReplyMutation.mutateAsync()}
                      type="button"
                    >
                      <Icon name="bolt" />
                    </button>
                  </div>
                  <button
                    className="inline-flex h-12 w-12 items-center justify-center rounded-[16px] bg-[var(--color-primary)] text-white shadow-[0_14px_28px_rgba(36,83,255,0.30)] transition hover:bg-[var(--color-primary-strong)] disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={!isOnline || sendMessageMutation.isPending}
                    onClick={() => void sendMessage()}
                    type="button"
                  >
                    <Icon className="h-5 w-5" name="send" />
                  </button>
                </div>

                <div className="mt-3 flex flex-col gap-2 text-xs text-[var(--color-text-secondary)] 2xl:flex-row 2xl:items-center 2xl:justify-between">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold">Atajos:</span>
                    {quickReplies.map((reply) => (
                      <button
                        key={reply.command}
                        className="font-semibold text-slate-600 transition hover:text-[var(--color-primary)]"
                        onClick={() => setMessageBody(reply.body)}
                        type="button"
                      >
                        {reply.command}
                      </button>
                    ))}
                  </div>
                  <span>Enter para enviar - Shift + Enter para nueva linea</span>
                </div>
              </div>
            </div>
          </Card>
        ) : null}
      </div>

      <ConfirmDialog
        confirmLabel="Cerrar conversacion"
        confirmLoading={closeMutation.isPending}
        description="La conversacion quedara fuera de la bandeja activa hasta que decidas reabrirla."
        onCancel={() => setCloseDialogOpen(false)}
        onConfirm={() => void closeMutation.mutateAsync()}
        open={closeDialogOpen}
        title="Cerrar conversacion"
        tone="danger"
      />
    </section>
  )
}
