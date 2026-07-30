import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useToast } from '../../../lib/toast'
import {
  createAestheticProduct,
  createAestheticRule,
  createAestheticService,
  listAestheticProductCategories,
  listAestheticProducts,
  listAestheticRules,
  listAestheticServiceCategories,
  listAestheticServices,
  updateAestheticProduct,
  updateAestheticRule,
  updateAestheticService,
} from '../../../services/api/aestheticApi'
import type {
  AestheticBusinessRuleResponse,
  AestheticIntentLogResponse,
  AestheticProductResponse,
  AestheticServiceResponse,
  UpsertAestheticBusinessRuleRequest,
  UpsertAestheticProductRequest,
  UpsertAestheticServiceRequest,
} from '../../../services/api/types'
import { PAGE_SIZE } from '../lib/constants'
import { matchesSearch } from '../lib/businessAiHelpers'

type KnowledgeTab = 'services' | 'products' | 'rules' | 'policies' | 'audit'

type KnowledgeRow = {
  id: string
  title: string
  category: string
  status: string
  updatedAt: string
  description: string
  type: 'service' | 'product' | 'rule' | 'audit'
  service?: AestheticServiceResponse
  product?: AestheticProductResponse
  rule?: AestheticBusinessRuleResponse
  log?: AestheticIntentLogResponse
}

type EditorState = {
  open: boolean
  mode: 'create' | 'edit'
  type: 'service' | 'product' | 'rule'
  id?: string
  title: string
  description: string
  categoryCode: string
  ruleType: string
  price: string
  durationMinutes: string
  stock: string
  priority: string
  active: boolean
  source?: KnowledgeRow
}

const emptyEditor: EditorState = {
  open: false,
  mode: 'create',
  type: 'service',
  title: '',
  description: '',
  categoryCode: '',
  ruleType: '',
  price: '',
  durationMinutes: '',
  stock: '',
  priority: '',
  active: true,
}

function serviceToRow(service: AestheticServiceResponse): KnowledgeRow {
  return {
    id: service.id,
    title: service.name,
    category: service.categoryCode,
    status: service.active ? 'active' : 'inactive',
    updatedAt: service.updatedAt,
    description: service.description,
    type: 'service',
    service,
  }
}

function productToRow(product: AestheticProductResponse): KnowledgeRow {
  return {
    id: product.id,
    title: product.name,
    category: product.categoryCode,
    status: product.active ? 'active' : 'inactive',
    updatedAt: product.updatedAt,
    description: product.description,
    type: 'product',
    product,
  }
}

function ruleToRow(rule: AestheticBusinessRuleResponse): KnowledgeRow {
  return {
    id: rule.id,
    title: rule.name,
    category: rule.ruleType,
    status: rule.active ? 'active' : 'inactive',
    updatedAt: rule.updatedAt,
    description: rule.description,
    type: 'rule',
    rule,
  }
}

function logToRow(log: AestheticIntentLogResponse): KnowledgeRow {
  return {
    id: log.id,
    title: `Intención: ${log.intencion}`,
    category: log.intencion,
    status: log.requiresHumanHandoff ? 'requires handoff' : 'synced',
    updatedAt: log.createdAt,
    description: log.mensajeUsuario,
    type: 'audit',
    log,
  }
}

function buildServiceStatusRequest(service: AestheticServiceResponse, active: boolean): UpsertAestheticServiceRequest {
  return {
    active,
    aftercareRecommendations: service.aftercareRecommendations,
    availabilityRules: service.availabilityRules,
    bookingRules: service.bookingRules,
    cancellationRules: service.cancellationRules,
    categoryCode: service.categoryCode,
    code: service.code,
    contraindications: service.contraindications,
    description: service.description,
    durationMinutes: service.durationMinutes,
    name: service.name,
    priceBase: service.priceBase,
    professionalRequired: service.professionalRequired,
    requiresInformedConsent: service.requiresInformedConsent,
    requiresPriorEvaluation: service.requiresPriorEvaluation,
    supplies: service.supplies,
    professionalIds: service.professionalIds,
    roomIds: service.roomIds,
  }
}

function buildProductStatusRequest(product: AestheticProductResponse, active: boolean): UpsertAestheticProductRequest {
  return {
    active,
    categoryCode: product.categoryCode,
    code: product.code,
    compatibleServices: product.compatibleServices,
    crossSellRules: product.crossSellRules,
    description: product.description,
    expirationDate: product.expirationDate,
    name: product.name,
    price: product.price,
    recommendationRules: product.recommendationRules,
    stock: product.stock,
    stockMinimum: product.stockMinimum,
    supplier: product.supplier,
    usageRestrictions: product.usageRestrictions,
  }
}

function buildRuleStatusRequest(rule: AestheticBusinessRuleResponse, active: boolean): UpsertAestheticBusinessRuleRequest {
  return {
    active,
    code: rule.code,
    description: rule.description,
    name: rule.name,
    priority: rule.priority,
    rulePayload: rule.rulePayload,
    ruleType: rule.ruleType,
  }
}

export function useBusinessKnowledgeHealth(logs: AestheticIntentLogResponse[]) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const [activeTab, setActiveTab] = useState<KnowledgeTab>('services')
  const [knowledgePage, setKnowledgePage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [editor, setEditor] = useState<EditorState>(emptyEditor)
  const [showBaseModal, setShowBaseModal] = useState(false)

  const servicesQuery = useQuery({
    queryKey: ['business-ai', 'services'],
    queryFn: () => listAestheticServices({ size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })

  const productsQuery = useQuery({
    queryKey: ['business-ai', 'products'],
    queryFn: () => listAestheticProducts({ size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })

  const rulesQuery = useQuery({
    queryKey: ['business-ai', 'rules'],
    queryFn: () => listAestheticRules({ size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })

  const serviceCategoriesQuery = useQuery({
    queryKey: ['business-ai', 'service-categories'],
    queryFn: () => listAestheticServiceCategories({ active: true, size: PAGE_SIZE }),
  })

  const productCategoriesQuery = useQuery({
    queryKey: ['business-ai', 'product-categories'],
    queryFn: () => listAestheticProductCategories({ active: true, size: PAGE_SIZE }),
  })

  const services = useMemo(() => servicesQuery.data?.items ?? [], [servicesQuery.data?.items])
  const products = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data?.items])
  const rules = useMemo(() => rulesQuery.data?.items ?? [], [rulesQuery.data?.items])
  const serviceCategories = useMemo(() => serviceCategoriesQuery.data?.items ?? [], [serviceCategoriesQuery.data?.items])
  const productCategories = useMemo(() => productCategoriesQuery.data?.items ?? [], [productCategoriesQuery.data?.items])

  const rows = useMemo(() => {
    const serviceRows = services.map(serviceToRow)
    const productRows = products.map(productToRow)
    const ruleRows = rules.map(ruleToRow)
    const policyRows = rules
      .filter((rule) => ['SAFETY', 'AVAILABILITY', 'PAYMENT', 'COMMERCIAL'].includes(rule.ruleType))
      .map(ruleToRow)
    const auditRows = logs.map(logToRow)

    const tabRows = {
      audit: auditRows,
      policies: policyRows,
      products: productRows,
      rules: ruleRows,
      services: serviceRows,
    }[activeTab]

    return tabRows.filter((row) => {
      const matchesStatus = statusFilter === 'all' || row.status === statusFilter
      return matchesStatus && matchesSearch(row, search)
    })
  }, [activeTab, logs, products, rules, search, services, statusFilter])

  const totalPages = Math.max(1, Math.ceil(rows.length / 10))
  const resolvedPage = Math.min(knowledgePage, totalPages - 1)
  const paginatedRows = rows.slice(
    resolvedPage * 10,
    resolvedPage * 10 + 10,
  )

  const tabs = [
    { label: 'Servicios', value: 'services' },
    { label: 'Productos', value: 'products' },
    { label: 'Reglas IA', value: 'rules' },
    { label: 'Políticas', value: 'policies' },
    { label: 'Auditoría', value: 'audit' },
  ] as const

  const statusMutation = useMutation({
    mutationFn: async ({ active, row }: { active: boolean; row: KnowledgeRow }) => {
      if (row.type === 'service' && row.service) {
        return updateAestheticService(row.service.id, buildServiceStatusRequest(row.service, active))
      }
      if (row.type === 'product' && row.product) {
        return updateAestheticProduct(row.product.id, buildProductStatusRequest(row.product, active))
      }
      if (row.type === 'rule' && row.rule) {
        return updateAestheticRule(row.rule.id, buildRuleStatusRequest(row.rule, active))
      }
      throw new Error('Este registro no permite cambios de estado.')
    },
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo actualizar el estado.',
        title: 'No se pudo cambiar el estado',
        tone: 'error',
      })
    },
    onSuccess: (_result, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai'] })
      showToast({
        description: `El contenido quedó ${variables.active ? 'activo' : 'desactivado'}.`,
        title: variables.active ? 'Contenido activado' : 'Contenido desactivado',
        tone: 'success',
      })
    },
  })

  const saveEditorMutation = useMutation({
    mutationFn: async (state: EditorState) => {
      if (state.type === 'service') {
        const source = state.source?.service
        const categoryCode = state.categoryCode || source?.categoryCode || serviceCategories[0]?.code || 'DEPILACION'
        const request: UpsertAestheticServiceRequest = {
          active: state.active,
          aftercareRecommendations: source?.aftercareRecommendations ?? null,
          availabilityRules: source?.availabilityRules ?? 'Validar disponibilidad en agenda antes de confirmar.',
          bookingRules: source?.bookingRules ?? 'Confirmar servicio, fecha y hora antes de reservar.',
          cancellationRules: source?.cancellationRules ?? 'Avisar con anticipación para reagendar.',
          categoryCode,
          code: source?.code ?? null,
          contraindications: source?.contraindications ?? null,
          description: state.description,
          durationMinutes: Number(state.durationMinutes) || 30,
          name: state.title,
          priceBase: Number(state.price) || 0,
          professionalRequired: source?.professionalRequired ?? 'Profesional estética',
          requiresInformedConsent: source?.requiresInformedConsent ?? false,
          requiresPriorEvaluation: source?.requiresPriorEvaluation ?? false,
          supplies: source?.supplies ?? null,
          professionalIds: source?.professionalIds ?? null,
          roomIds: source?.roomIds ?? null,
        }
        if (state.mode === 'edit' && state.id) {
          return updateAestheticService(state.id, request)
        }
        return createAestheticService(request)
      }

      if (state.type === 'product') {
        const source = state.source?.product
        const categoryCode = state.categoryCode || source?.categoryCode || productCategories[0]?.code || 'POST_TRATAMIENTO'
        const request: UpsertAestheticProductRequest = {
          active: state.active,
          categoryCode,
          code: source?.code ?? null,
          compatibleServices: source?.compatibleServices ?? null,
          crossSellRules: source?.crossSellRules ?? null,
          description: state.description,
          expirationDate: source?.expirationDate ?? null,
          name: state.title,
          price: Number(state.price) || 0,
          recommendationRules: source?.recommendationRules ?? 'Recomendar solo si aporta al tratamiento consultado.',
          stock: Number(state.stock) || 0,
          stockMinimum: source?.stockMinimum ?? 1,
          supplier: source?.supplier ?? null,
          usageRestrictions: source?.usageRestrictions ?? null,
        }
        if (state.mode === 'edit' && state.id) {
          return updateAestheticProduct(state.id, request)
        }
        return createAestheticProduct(request)
      }

      const source = state.source?.rule
      const request: UpsertAestheticBusinessRuleRequest = {
        active: state.active,
        code: source?.code ?? null,
        description: state.description,
        name: state.title,
        priority: Number(state.priority) || 50,
        rulePayload: source?.rulePayload ?? JSON.stringify({ source: 'business-ai-page' }),
        ruleType: state.ruleType,
      }
      if (state.mode === 'edit' && state.id) {
        return updateAestheticRule(state.id, request)
      }
      return createAestheticRule(request)
    },
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo guardar el contenido.',
        title: 'Error al guardar contenido',
        tone: 'error',
      })
    },
    onSuccess: () => {
      setEditor(emptyEditor)
      void queryClient.invalidateQueries({ queryKey: ['business-ai'] })
      showToast({
        description: 'La base de conocimiento quedó actualizada.',
        title: 'Contenido guardado',
        tone: 'success',
      })
    },
  })

  return {
    activeTab,
    setActiveTab,
    knowledgePage,
    setKnowledgePage,
    search,
    setSearch,
    statusFilter,
    setStatusFilter,
    editor,
    setEditor,
    showBaseModal,
    setShowBaseModal,
    services,
    products,
    rules,
    serviceCategories,
    productCategories,
    rows,
    paginatedRows,
    totalPages,
    tabs,
    statusMutation,
    saveEditorMutation,
    isKnowledgeLoading: servicesQuery.isLoading || productsQuery.isLoading || rulesQuery.isLoading,
  }
}
