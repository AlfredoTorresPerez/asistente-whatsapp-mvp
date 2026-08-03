import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import {
  listAestheticRules,
  listAestheticServiceCategories,
  listAestheticServices,
} from '../../../services/api/aestheticApi'
import type { AestheticIntentLogResponse } from '../../../services/api/types'
import { PAGE_SIZE } from '../lib/constants'
import { matchesSearch } from '../lib/businessAiHelpers'

type KnowledgeTab = 'services' | 'rules' | 'policies'

type KnowledgeRow = {
  id: string
  title: string
  category: string
  status: string
  updatedAt: string
  description: string
  type: 'service' | 'rule' | 'audit'
}

function serviceToRow(service: { id: string; name: string; categoryCode: string; active: boolean; updatedAt: string; description: string }): KnowledgeRow {
  return {
    id: service.id,
    title: service.name,
    category: service.categoryCode,
    status: service.active ? 'active' : 'inactive',
    updatedAt: service.updatedAt,
    description: service.description,
    type: 'service',
  }
}

function ruleToRow(rule: { id: string; name: string; ruleType: string; active: boolean; updatedAt: string; description: string }): KnowledgeRow {
  return {
    id: rule.id,
    title: rule.name,
    category: rule.ruleType,
    status: rule.active ? 'active' : 'inactive',
    updatedAt: rule.updatedAt,
    description: rule.description,
    type: 'rule',
  }
}

export function useBusinessKnowledgeHealth(logs: AestheticIntentLogResponse[], userPermissions: string[] = []) {
  const [activeTab, setActiveTab] = useState<KnowledgeTab>('services')
  const [knowledgePage, setKnowledgePage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [showBaseModal, setShowBaseModal] = useState(false)

  const hasCatView = userPermissions.includes('CATALOG_VIEW') || userPermissions.includes('ALL')
  const hasAutoManage = userPermissions.includes('AUTOMATION_MANAGE') || userPermissions.includes('ALL')

  const servicesQuery = useQuery({
    queryKey: ['business-ai', 'services'],
    queryFn: () => listAestheticServices({ size: PAGE_SIZE }),
    enabled: hasCatView,
    placeholderData: keepPreviousData,
  })

  const rulesQuery = useQuery({
    queryKey: ['business-ai', 'rules'],
    queryFn: () => listAestheticRules({ size: PAGE_SIZE }),
    enabled: hasAutoManage,
    placeholderData: keepPreviousData,
  })

  const serviceCategoriesQuery = useQuery({
    queryKey: ['business-ai', 'service-categories'],
    queryFn: () => listAestheticServiceCategories({ active: true, size: PAGE_SIZE }),
    enabled: hasCatView,
  })

  const services = useMemo(() => servicesQuery.data?.items ?? [], [servicesQuery.data?.items])
  const rules = useMemo(() => rulesQuery.data?.items ?? [], [rulesQuery.data?.items])
  const serviceCategories = useMemo(() => serviceCategoriesQuery.data?.items ?? [], [serviceCategoriesQuery.data?.items])

  const rows = useMemo(() => {
    const serviceRows = services.map(serviceToRow)
    const ruleRows = rules.map(ruleToRow)
    const policyRows = rules
      .filter((rule) => ['SAFETY', 'AVAILABILITY', 'PAYMENT', 'COMMERCIAL'].includes(rule.ruleType))
      .map(ruleToRow)

    const tabRows = {
      policies: policyRows,
      rules: ruleRows,
      services: serviceRows,
    }[activeTab]

    return tabRows.filter((row) => {
      const matchesStatus = statusFilter === 'all' || row.status === statusFilter
      return matchesStatus && matchesSearch(row, search)
    })
  }, [activeTab, rules, search, services, statusFilter])

  const totalPages = Math.max(1, Math.ceil(rows.length / 10))
  const resolvedPage = Math.min(knowledgePage, totalPages - 1)
  const paginatedRows = rows.slice(
    resolvedPage * 10,
    resolvedPage * 10 + 10,
  )

  const tabs = [
    { label: 'Servicios', value: 'services' },
    { label: 'Reglas IA', value: 'rules' },
    { label: 'Políticas', value: 'policies' },
  ] as const

  return {
    activeTab,
    setActiveTab,
    knowledgePage,
    setKnowledgePage,
    search,
    setSearch,
    statusFilter,
    setStatusFilter,
    showBaseModal,
    setShowBaseModal,
    services,
    rules,
    serviceCategories,
    rows,
    paginatedRows,
    totalPages,
    tabs,
    isKnowledgeLoading: servicesQuery.isLoading || rulesQuery.isLoading,
  }
}
