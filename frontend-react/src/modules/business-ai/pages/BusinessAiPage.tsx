import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useReducer, useState } from 'react'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Modal } from '../../../components/overlay/Modal'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import {
  formatEstadoRegistro,
  getRegistroTone,
  isRegistroActivo,
} from '../../../lib/statusFormatters'
import { useToast } from '../../../lib/toast'
import {
  BUSINESS_AI_AUDIT_PAGE_SIZE,
  getAuditTotalPages,
  isBusinessHourRangeValid,
  paginateAuditLogs,
  sortAuditLogsDescending,
} from '../lib/businessAiHelpers'
import type { BusinessHoursDay } from '../lib/businessAiHelpers'
import { formatRuleType } from '../../rules/lib/ruleTypeLabels'
import {
  analyzeAestheticIntent,
  createAestheticProduct,
  createAestheticRule,
  createAestheticService,
  listAestheticIntentLogs,
  listAestheticProductCategories,
  listAestheticProducts,
  listAestheticRules,
  listAestheticServiceCategories,
  listAestheticServices,
  updateAestheticProduct,
  updateAestheticRule,
  updateAestheticService,
} from '../../../services/api/aestheticApi'
import {
  getConversationsRequest,
  sendConversationMessageRequest,
} from '../../../services/api/conversationsApi'
import {
  getBusinessHoursRequest,
  saveBusinessHoursRequest,
} from '../../../services/api/completeAgendaApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import type {
  AestheticBusinessRuleResponse,
  AestheticIntentLogResponse,
  AestheticProductResponse,
  AestheticServiceResponse,
  BusinessHoursResponse,
  BusinessLocationResponse,
  ConversationSummaryResponse,
  IntentAnalysisResponse,
  UpsertAestheticBusinessRuleRequest,
  UpsertAestheticProductRequest,
  UpsertAestheticServiceRequest,
} from '../../../services/api/types'

type MetricCardData = {
  accent: 'green' | 'blue' | 'orange'
  description: string
  icon: 'spark' | 'chat' | 'shield' | 'human' | 'send'
  title: string
  value: string
}

type KnowledgeTab = 'services' | 'products' | 'rules' | 'policies' | 'audit'
type BusinessAiArea = 'assistant-config' | 'conversation-preview' | 'knowledge-base'

type AssistantMode = 'suggest' | 'auto'
type AssistantTone = 'Cercano' | 'Profesional' | 'Comercial'

type AssistantConfigState = {
  active: boolean
  mode: AssistantMode
  tone: AssistantTone
  language: string
  escalationThreshold: string
  allowPrices: boolean
  allowBooking: boolean
  allowPromotions: boolean
  requireAvailabilityCheck: boolean
}

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

const PAGE_SIZE = 100
const KNOWLEDGE_PAGE_SIZE = 10

const allowedTopicDefaults = [
  'Servicios',
  'Productos',
  'Citas',
  'Promociones',
  'Horarios',
  'Políticas',
  'Ubicación',
]

const blockedTopicDefaults = [
  'Diagnósticos médicos',
  'Resultados garantizados',
  'Temas legales',
  'Mensajes técnicos',
  'Difusiones masivas',
]

const defaultPrompt = `
Eres el asistente conversacional de Centro Estético Bella para WhatsApp.

Tu objetivo principal es atender clientes por WhatsApp, responder consultas comerciales, ayudar a vender servicios o productos, gestionar solicitudes de agenda, consultar disponibilidad real, crear reservas temporales, enviar enlaces de confirmación, reprogramar, cancelar, recordar citas y derivar a atención humana cuando corresponda.

Contexto del negocio:
- Canal de atención: WhatsApp.
- Modelo operativo: una o múltiples sucursales.
- Agenda: digital, con validación real de disponibilidad.
- El asistente debe trabajar siempre con la información registrada en la base de conocimiento, catálogo de servicios, sucursales, horarios, profesionales, reglas comerciales y agenda del negocio.

Reglas generales:
1. Responde de forma clara, breve, cercana y profesional.
2. Haz solo una pregunta principal por turno.
3. Conserva el contexto de la conversación.
4. No vuelvas a pedir datos que el cliente ya entregó.
5. No inventes servicios, precios, horarios, sucursales, promociones, disponibilidad ni políticas.
6. No confirmes disponibilidad sin consultar la agenda digital.
7. No confirmes una reserva sin servicio, sucursal, fecha y hora.
8. No envíes enlace de confirmación si todavía falta un dato crítico.
9. No confirmes pagos sin validación del sistema.
10. Si el cliente pide hablar con una persona, deriva a atención humana.
11. Si el mensaje es ambiguo, sensible, urgente, molesto, técnico, de difusión o fuera de alcance comercial, deriva o solicita aclaración mínima.
12. Si la confianza de la IA es baja, no inventes respuesta: pide aclaración o deriva.

Intenciones que debes reconocer:
- saludo;
- consulta de servicios;
- consulta de precios;
- consulta de productos;
- consulta de sucursales;
- consulta de ubicación;
- consulta de horarios de atención;
- agendar;
- consultar disponibilidad;
- confirmar reserva;
- reprogramar reserva;
- cancelar reserva;
- reenviar enlace de confirmación;
- pagar o consultar señal;
- pedir atención humana;
- reclamo;
- seguimiento posterior;
- consulta fuera de alcance.

Datos que debes extraer cuando el cliente quiera agendar:
- nombre del cliente, si aparece;
- teléfono, si aparece;
- servicio solicitado;
- sucursal preferida;
- fecha solicitada;
- horario o preferencia horaria;
- profesional preferido, si aparece;
- observaciones del cliente;
- intención principal.

Regla crítica de agenda:
Si el cliente ya mencionó un servicio, no vuelvas a pedir el servicio. Guarda el servicio detectado y pregunta solo el siguiente dato faltante.

Ejemplo:
Cliente: Hola, me gustaría reservar una limpieza facial para el viernes.
Respuesta correcta:
Perfecto ✅ Te ayudo a agendar una limpieza facial para el viernes.
¿En qué sucursal prefieres atenderte?

Ejemplo:
Cliente: En Providencia.
Respuesta correcta:
Perfecto. ¿Prefieres horario en la mañana o en la tarde?

Ejemplo:
Cliente: En la tarde.
Respuesta correcta:
Perfecto. Revisaré disponibilidad para limpieza facial en Providencia este viernes por la tarde.

Reglas para reconocer servicios:
1. Si el cliente menciona un servicio de forma parcial, intenta relacionarlo con el catálogo del negocio.
2. Si existe una coincidencia razonable con un servicio registrado, úsala.
3. Si hay más de una coincidencia posible, pregunta cuál prefiere.
4. Si no existe coincidencia, informa que no encontraste ese servicio y ofrece opciones del catálogo.
5. No inventes servicios que no existan.

Ejemplos de interpretación:
- “limpieza facial”, “facial”, “limpieza de rostro”, “higiene facial” pueden corresponder a un servicio facial registrado.
- “depilación bozo”, “bozo”, “depilación axilas”, “piernas”, “bikini” pueden corresponder a servicios de depilación registrados.
- “masaje”, “relajación”, “descontracturante” deben buscarse en el catálogo antes de responder.

Reglas para reconocer fechas:
Debes interpretar como fecha o preferencia de fecha:
- hoy;
- mañana;
- pasado mañana;
- lunes;
- martes;
- miércoles;
- jueves;
- viernes;
- sábado;
- domingo;
- esta semana;
- próxima semana;
- la otra semana;
- en la mañana;
- en la tarde;
- después de las 18:00;
- antes del mediodía.

Si la fecha es relativa, usa el calendario actual del sistema para resolverla. Si no puedes resolverla con seguridad, pregunta una aclaración breve.

Flujo obligatorio para agendar:
1. Detecta que el cliente quiere agendar.
2. Extrae servicio, fecha, sucursal y horario si aparecen.
3. Si falta servicio, pregunta por el servicio.
4. Si falta sucursal y el negocio tiene más de una sucursal, pregunta la sucursal.
5. Si el negocio tiene una sola sucursal, no preguntes sucursal.
6. Si falta fecha, pregunta qué día prefiere.
7. Si falta horario, pregunta si prefiere mañana, tarde o una hora específica.
8. Cuando existan servicio, sucursal, fecha y preferencia horaria, consulta la agenda digital.
9. Propón solo horarios realmente disponibles.
10. Espera que el cliente elija un horario.
11. Cuando el cliente elija un horario, crea una reserva temporal.
12. Genera y envía un enlace de confirmación por WhatsApp.
13. Informa que el enlace tiene vencimiento.
14. Si el cliente confirma desde el enlace, la reserva queda confirmada.
15. Si el enlace vence, el cupo se libera.

Respuesta cuando faltan datos:
Si falta el servicio:
“Claro 😊 ¿Qué servicio quieres agendar?”

Si falta la sucursal:
“Perfecto. ¿En qué sucursal prefieres atenderte?”

Si falta fecha:
“Perfecto. ¿Qué día te gustaría agendar?”

Si falta horario:
“Perfecto. ¿Prefieres horario en la mañana, en la tarde o una hora específica?”

Si ya están servicio y fecha:
No vuelvas a pedir servicio. Pregunta sucursal u horario según corresponda.

Respuesta cuando hay disponibilidad:
“Tengo estos horarios disponibles para {servicio} en {sucursal} el {fecha}:
1. {hora_1}
2. {hora_2}
3. {hora_3}

¿Cuál prefieres?”

Respuesta cuando el cliente elige horario:
“Perfecto ✅ Dejé tu reserva temporal para:

Servicio: {servicio}
Sucursal: {sucursal}
Fecha: {fecha}
Hora: {hora}

Confirma tu reserva aquí:
{enlace_confirmacion}

El enlace vence en {tiempo_expiracion} minutos.”

Reglas para enlaces:
Solo debes enviar enlaces cuando exista una acción concreta lista para el cliente.

Tipos de enlaces permitidos:
- enlace de confirmación de reserva;
- enlace de reprogramación;
- enlace de cancelación;
- enlace de pago o señal;
- enlace de ubicación;
- enlace de catálogo, si existe;
- enlace de preparación o cuidados, si existe.

No envíes enlace de confirmación si falta:
- servicio;
- sucursal, cuando exista más de una;
- fecha;
- hora;
- cliente o conversación válida.

Si el cliente pide “mándame el enlace”, “reenviar enlace” o “no me llegó el link”:
1. Busca una reserva temporal vigente.
2. Si existe, reenvía el enlace.
3. Si expiró, informa que el cupo fue liberado y ofrece buscar nuevo horario.
4. Si no existe reserva, pide los datos mínimos para crear una.

Respuesta para reenviar enlace:
“Claro ✅ Aquí tienes nuevamente tu enlace de confirmación:

{enlace_confirmacion}

Recuerda confirmarlo antes de {hora_expiracion} para mantener tu cupo.”

Respuesta si el enlace expiró:
“Tu enlace anterior ya expiró y el cupo fue liberado.
Puedo ayudarte a buscar un nuevo horario disponible. ¿Quieres que revise opciones para el mismo servicio?”

Flujo para reprogramar:
1. Identifica la reserva del cliente.
2. Pide nueva fecha u horario preferido.
3. Consulta disponibilidad real.
4. Propón horarios disponibles.
5. Cuando el cliente elija, genera enlace de reprogramación si aplica.
6. No confirmes el nuevo horario sin validación.

Respuesta para reprogramar:
“Claro, puedo ayudarte a reprogramar tu cita.
¿Qué día u horario prefieres?”

Flujo para cancelar:
1. Identifica la reserva.
2. Confirma la intención de cancelar.
3. Registra motivo si el sistema lo solicita.
4. Cancela la reserva o envía enlace de cancelación si aplica.
5. Informa que el cupo quedará liberado.

Respuesta para cancelar:
“Entiendo. Para cancelar tu reserva, confirma aquí:

{enlace_cancelacion}

Al cancelar, el cupo quedará disponible para otros clientes.”

Flujo para pagos o señal:
1. Indica si el servicio requiere señal solo si esa regla existe en el sistema.
2. No inventes montos.
3. Si existe pago pendiente, entrega enlace de pago.
4. No confirmes pago sin validación.

Respuesta para señal:
“Para asegurar tu reserva se requiere una señal de {monto}.
Puedes pagar aquí:

{enlace_pago}

Cuando el pago sea validado, actualizaremos el estado de tu reserva.”

Reglas para sucursales:
1. Si el negocio tiene una sola sucursal, no preguntes sucursal.
2. Si el negocio tiene varias sucursales, pregunta la sucursal antes de consultar agenda.
3. Si el cliente menciona una sucursal, guárdala y no vuelvas a pedirla.
4. Si el cliente pregunta ubicación, responde con dirección y enlace de mapa si existe.

Respuesta para ubicación:
“La sucursal {sucursal} está en:
{direccion}

Puedes verla aquí:
{enlace_ubicacion}”

Reglas para derivación humana:
Deriva a humano cuando:
- el cliente lo pide explícitamente;
- hay reclamo;
- hay molestia o urgencia;
- hay baja confianza de IA;
- el cliente entrega información sensible;
- el caso requiere criterio humano;
- hay error de agenda, pago o enlace;
- el cliente insiste después de dos respuestas sin resolver.

Respuesta de derivación:
“Te voy a derivar con una persona del equipo para ayudarte mejor. Un momento por favor.”

Formato de respuesta:
- Usa español claro.
- Usa mensajes cortos.
- No uses párrafos largos.
- No hagas más de una pregunta principal por turno.
- Confirma los datos importantes antes de crear una reserva.
- Mantén tono cercano, profesional y orientado a resolver.

Resumen de comportamiento esperado:
Tu tarea es convertir conversaciones de WhatsApp en resultados concretos: consultas resueltas, reservas temporales, reservas confirmadas, pagos derivados, reprogramaciones, cancelaciones y atención humana cuando corresponda. Siempre debes avanzar al siguiente paso lógico sin pedir información ya entregada.
`.trim()

const emptyEditor: EditorState = {
  active: true,
  categoryCode: '',
  description: '',
  durationMinutes: '30',
  id: undefined,
  mode: 'create',
  open: false,
  price: '0',
  priority: '50',
  ruleType: 'AI_RESPONSE',
  stock: '0',
  title: '',
  type: 'service',
}

const normalizeConfidence = (value: number) => (value <= 1 ? value * 100 : value)

const formatPercent = (value: number) => `${Math.round(value)}%`

const formatMoney = (value: number) =>
  new Intl.NumberFormat('es-CL', {
    currency: 'CLP',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return 'Sin fecha'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

function serviceToRow(service: AestheticServiceResponse): KnowledgeRow {
  return {
    category: service.categoryName,
    description: `${service.description}\nDuración: ${service.durationMinutes} min. Valor base: ${formatMoney(service.priceBase)}.`,
    id: service.id,
    service,
    status: formatEstadoRegistro(service.active),
    title: service.name,
    type: 'service',
    updatedAt: formatDateTime(service.updatedAt),
  }
}

function productToRow(product: AestheticProductResponse): KnowledgeRow {
  return {
    category: product.categoryName,
    description: `${product.description}\nPrecio: ${formatMoney(product.price)}. Stock: ${product.stock}.`,
    id: product.id,
    product,
    status: formatEstadoRegistro(product.active),
    title: product.name,
    type: 'product',
    updatedAt: formatDateTime(product.updatedAt),
  }
}

function ruleToRow(rule: AestheticBusinessRuleResponse): KnowledgeRow {
  return {
    category: formatRuleType(rule.ruleType),
    description: rule.description,
    id: rule.id,
    rule,
    status: formatEstadoRegistro(rule.active),
    title: rule.name,
    type: 'rule',
    updatedAt: formatDateTime(rule.updatedAt),
  }
}

function logToRow(log: AestheticIntentLogResponse): KnowledgeRow {
  return {
    category: log.intent,
    description: log.suggestedResponse ?? log.sourceMessage,
    id: log.id,
    log,
    status: log.requiresHumanHandoff ? 'Requiere derivación' : 'Sincronizado',
    title: log.sourceMessage,
    type: 'audit',
    updatedAt: formatDateTime(log.createdAt),
  }
}

function buildServiceStatusRequest(
  service: AestheticServiceResponse,
  active: boolean,
): UpsertAestheticServiceRequest {
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
  }
}

function buildProductStatusRequest(
  product: AestheticProductResponse,
  active: boolean,
): UpsertAestheticProductRequest {
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

function buildRuleStatusRequest(
  rule: AestheticBusinessRuleResponse,
  active: boolean,
): UpsertAestheticBusinessRuleRequest {
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

const matchesSearch = (row: KnowledgeRow, search: string) => {
  if (!search.trim()) {
    return true
  }

  const normalizedSearch = search.trim().toLowerCase()
  return [row.title, row.category, row.description]
    .join(' ')
    .toLowerCase()
    .includes(normalizedSearch)
}

function buildPrompt(config: AssistantConfigState, basePrompt: string) {
  return [
    basePrompt.trim(),
    '',
    `Modo: ${config.mode === 'auto' ? 'responder automaticamente' : 'sugerir respuestas para aprobacion humana'}.`,
    `Tono: ${config.tone}. Idioma: ${config.language}.`,
    `Umbral de derivación humana: ${config.escalationThreshold}%.`,
    `Precios permitidos: ${config.allowPrices ? 'si' : 'no'}. Agenda permitida: ${config.allowBooking ? 'si' : 'no'}. Promociones permitidas: ${config.allowPromotions ? 'si' : 'no'}.`,
    `Disponibilidad real: ${config.requireAvailabilityCheck ? 'validar antes de confirmar' : 'solo orientar sin confirmar'}.`,
  ].join('\n')
}

const emptyHours: BusinessHoursDay[] = [
  { day: 'Lunes', startTime: '09:00', endTime: '18:00' },
  { day: 'Martes', startTime: '09:00', endTime: '18:00' },
  { day: 'Miércoles', startTime: '09:00', endTime: '18:00' },
  { day: 'Jueves', startTime: '09:00', endTime: '18:00' },
  { day: 'Viernes', startTime: '09:00', endTime: '18:00' },
  { day: 'Sábado', startTime: '09:00', endTime: '13:00' },
  { day: 'Domingo', startTime: '', endTime: '' },
]

const DAY_MAP: Record<string, number> = {
  Lunes: 1,
  Martes: 2,
  Miércoles: 3,
  Jueves: 4,
  Viernes: 5,
  Sábado: 6,
  Domingo: 7,
}

interface BusinessHoursState {
  hours: BusinessHoursDay[]
  saved: boolean
}

type BusinessHoursAction =
  | { type: 'LOAD_SERVER'; hours: BusinessHoursDay[] }
  | { type: 'SET_HOURS'; hours: BusinessHoursDay[] }
  | { type: 'MARK_SAVED' }
  | { type: 'MARK_UNSAVED' }

function businessHoursReducer(
  state: BusinessHoursState,
  action: BusinessHoursAction,
): BusinessHoursState {
  switch (action.type) {
    case 'LOAD_SERVER':
      return { hours: action.hours, saved: true }
    case 'SET_HOURS':
      return { hours: action.hours, saved: false }
    case 'MARK_SAVED':
      return { ...state, saved: true }
    case 'MARK_UNSAVED':
      return { ...state, saved: false }
    default:
      return state
  }
}

export function BusinessAiPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [activeArea, setActiveArea] = useState<BusinessAiArea>('assistant-config')
  const [activeTab, setActiveTab] = useState<KnowledgeTab>('services')
  const [knowledgePage, setKnowledgePage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [auditPage, setAuditPage] = useState(0)
  const [scenario, setScenario] = useState(
    'Hola, quiero saber que tipo de depilacion ofrecen y agendar depilacion bozo para manana a las 14 horas.',
  )
  const [conversationSearch, setConversationSearch] = useState('')
  const [selectedConversationId, setSelectedConversationId] = useState('')
  const [analysisResult, setAnalysisResult] = useState<IntentAnalysisResponse | null>(null)
  const [assistantPrompt, setAssistantPrompt] = useState(defaultPrompt)
  const [previewEditable, setPreviewEditable] = useState(false)
  const [previewResponse, setPreviewResponse] = useState('')
  const [showBaseModal, setShowBaseModal] = useState(false)
  const [editor, setEditor] = useState<EditorState>(emptyEditor)
  const [rowToToggle, setRowToToggle] = useState<{ row: KnowledgeRow; active: boolean } | null>(
    null,
  )
  const [allowedTopics, setAllowedTopics] = useState(
    () =>
      Object.fromEntries(allowedTopicDefaults.map((topic) => [topic, true])) as Record<
        string,
        boolean
      >,
  )
  const [blockedTopics, setBlockedTopics] = useState(
    () =>
      Object.fromEntries(blockedTopicDefaults.map((topic) => [topic, true])) as Record<
        string,
        boolean
      >,
  )
  const [businessHoursState, businessHoursDispatch] = useReducer(businessHoursReducer, {
    hours: emptyHours,
    saved: false,
  })
  const { hours: businessHours, saved: businessHoursSaved } = businessHoursState
  const [businessHoursError, setBusinessHoursError] = useState<string | null>(null)
  const [hoursLocationId, setHoursLocationId] = useState('')
  const [config, setConfig] = useState<AssistantConfigState>({
    active: true,
    allowBooking: true,
    allowPrices: true,
    allowPromotions: true,
    escalationThreshold: '70',
    language: 'es',
    mode: 'auto',
    requireAvailabilityCheck: true,
    tone: 'Cercano',
  })

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

  const logsQuery = useQuery({
    queryKey: ['business-ai', 'intent-logs'],
    queryFn: () => listAestheticIntentLogs({ size: PAGE_SIZE }),
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

  const conversationsQuery = useQuery({
    queryKey: ['business-ai', 'conversations', conversationSearch],
    queryFn: () =>
      getConversationsRequest({
        page: 0,
        size: 50,
        search: conversationSearch.trim() || undefined,
        status: 'OPEN',
      }),
    placeholderData: keepPreviousData,
  })

  const services = useMemo(() => servicesQuery.data?.items ?? [], [servicesQuery.data?.items])
  const products = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data?.items])
  const rules = useMemo(() => rulesQuery.data?.items ?? [], [rulesQuery.data?.items])
  const logs = useMemo(() => logsQuery.data?.items ?? [], [logsQuery.data?.items])
  const serviceCategories = useMemo(
    () => serviceCategoriesQuery.data?.items ?? [],
    [serviceCategoriesQuery.data?.items],
  )
  const productCategories = useMemo(
    () => productCategoriesQuery.data?.items ?? [],
    [productCategoriesQuery.data?.items],
  )
  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'ai-page'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const locations = useMemo(() => locationsQuery.data ?? [], [locationsQuery.data])
  const firstLocationId = locations.length > 0 ? locations[0].id : ''

  const businessHoursQuery = useQuery({
    queryKey: ['agenda-business-hours', hoursLocationId || firstLocationId],
    queryFn: () => getBusinessHoursRequest(hoursLocationId || firstLocationId),
    enabled: !!(hoursLocationId || firstLocationId),
  })

  const saveHoursMutation = useMutation({
    mutationFn: async () => {
      const locationId = hoursLocationId || firstLocationId
      if (!locationId) throw new Error('No hay sucursales disponibles.')
      return saveBusinessHoursRequest({
        locationId,
        hours: businessHours
          .filter((h) => h.startTime && h.endTime)
          .map((h) => ({
            dayOfWeek: DAY_MAP[h.day] ?? 1,
            startTime: h.startTime,
            endTime: h.endTime,
          })),
      })
    },
    onSuccess: () => {
      businessHoursDispatch({ type: 'MARK_SAVED' })
      showToast({
        title: 'Horario guardado',
        description: 'El horario de atención se actualizó correctamente.',
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'No se pudo guardar el horario',
        description: error instanceof Error ? error.message : 'Intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  useEffect(() => {
    if (businessHoursQuery.data) {
      const serverHours = businessHoursQuery.data as BusinessHoursResponse[]
      if (serverHours.length > 0) {
        const days: BusinessHoursDay[] = emptyHours.map((day) => {
          const dow = DAY_MAP[day.day]
          const match = serverHours.find((h) => h.dayOfWeek === dow)
          return match
            ? { ...day, startTime: match.startTime.slice(0, 5), endTime: match.endTime.slice(0, 5) }
            : day
        })
        businessHoursDispatch({ type: 'LOAD_SERVER', hours: days })
      }
    }
  }, [businessHoursQuery.data])

  const conversations = useMemo(
    () => conversationsQuery.data?.items ?? [],
    [conversationsQuery.data?.items],
  )

  const operationalPromptRule = rules.find((rule) => rule.code === 'PROMPT_OPERATIVO_IA_NEGOCIO')

  const analyzeMutation = useMutation({
    mutationFn: analyzeAestheticIntent,
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo ejecutar la prueba.',
        title: 'No se pudo probar la IA',
        tone: 'error',
      })
    },
    onSuccess: (result) => {
      setAnalysisResult(result)
      setPreviewResponse(result.respuestaSugerida)
      setAuditPage(0)
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'intent-logs'] })
      showToast({
        description: `Intencion detectada: ${result.intencion}. Confianza: ${formatPercent(normalizeConfidence(result.confianza))}.`,
        title: 'Prueba ejecutada',
        tone: 'success',
      })
    },
  })

  const savePromptMutation = useMutation({
    mutationFn: async () => {
      const prompt = buildPrompt(config, assistantPrompt)
      const request: UpsertAestheticBusinessRuleRequest = {
        active: config.active,
        code: 'PROMPT_OPERATIVO_IA_NEGOCIO',
        description: prompt.slice(0, 3900),
        name: 'Prompt operativo de IA del negocio',
        priority: 1,
        rulePayload: JSON.stringify({ config, prompt }),
        ruleType: 'AI_PROMPT',
      }

      if (operationalPromptRule) {
        return updateAestheticRule(operationalPromptRule.id, request)
      }

      return createAestheticRule(request)
    },
    onError: (error) => {
      showToast({
        description:
          error instanceof Error ? error.message : 'No se pudo guardar la configuracion.',
        title: 'Error al guardar prompt',
        tone: 'error',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'rules'] })
      showToast({
        description: 'La configuracion quedó registrada como regla del negocio.',
        title: 'Prompt guardado',
        tone: 'success',
      })
    },
  })

  const saveEditorMutation = useMutation({
    mutationFn: async (state: EditorState) => {
      if (state.type === 'service') {
        const source = state.source?.service
        const categoryCode =
          state.categoryCode || source?.categoryCode || serviceCategories[0]?.code || 'DEPILACION'
        const request: UpsertAestheticServiceRequest = {
          active: state.active,
          aftercareRecommendations: source?.aftercareRecommendations ?? null,
          availabilityRules:
            source?.availabilityRules ?? 'Validar disponibilidad en agenda antes de confirmar.',
          bookingRules:
            source?.bookingRules ?? 'Confirmar servicio, fecha y hora antes de reservar.',
          cancellationRules: source?.cancellationRules ?? 'Avisar con anticipacion para reagendar.',
          categoryCode,
          code: source?.code ?? null,
          contraindications: source?.contraindications ?? null,
          description: state.description,
          durationMinutes: Number(state.durationMinutes) || 30,
          name: state.title,
          priceBase: Number(state.price) || 0,
          professionalRequired: source?.professionalRequired ?? 'Profesional estetica',
          requiresInformedConsent: source?.requiresInformedConsent ?? false,
          requiresPriorEvaluation: source?.requiresPriorEvaluation ?? false,
          supplies: source?.supplies ?? null,
        }

        if (state.mode === 'edit' && state.id) {
          return updateAestheticService(state.id, request)
        }
        return createAestheticService(request)
      }

      if (state.type === 'product') {
        const source = state.source?.product
        const categoryCode =
          state.categoryCode ||
          source?.categoryCode ||
          productCategories[0]?.code ||
          'POST_TRATAMIENTO'
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
          recommendationRules:
            source?.recommendationRules ?? 'Recomendar solo si aporta al tratamiento consultado.',
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

  const statusMutation = useMutation({
    mutationFn: async ({ active, row }: { active: boolean; row: KnowledgeRow }) => {
      if (row.type === 'service' && row.service) {
        return updateAestheticService(
          row.service.id,
          buildServiceStatusRequest(row.service, active),
        )
      }

      if (row.type === 'product' && row.product) {
        return updateAestheticProduct(
          row.product.id,
          buildProductStatusRequest(row.product, active),
        )
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
      setRowToToggle(null)
      void queryClient.invalidateQueries({ queryKey: ['business-ai'] })
      showToast({
        description: `El contenido quedó ${variables.active ? 'activo' : 'desactivado'}.`,
        title: variables.active ? 'Contenido activado' : 'Contenido desactivado',
        tone: 'success',
      })
    },
  })

  const sendApprovedMutation = useMutation({
    mutationFn: ({ body, conversationId }: { body: string; conversationId: string }) =>
      sendConversationMessageRequest(conversationId, { body }),
    onError: (error) => {
      showToast({
        description:
          error instanceof Error
            ? error.message
            : 'No se pudo enviar la respuesta aprobada al cliente.',
        title: 'No se pudo enviar',
        tone: 'error',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'conversations'] })
      void queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        description:
          'La respuesta aprobada fue enviada y registrada en la conversación seleccionada.',
        title: 'Respuesta enviada',
        tone: 'success',
      })
    },
  })

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

  const totalKnowledgePages = Math.max(1, Math.ceil(rows.length / KNOWLEDGE_PAGE_SIZE))
  const resolvedKnowledgePage = Math.min(knowledgePage, totalKnowledgePages - 1)
  const paginatedRows = rows.slice(
    resolvedKnowledgePage * KNOWLEDGE_PAGE_SIZE,
    resolvedKnowledgePage * KNOWLEDGE_PAGE_SIZE + KNOWLEDGE_PAGE_SIZE,
  )

  const metrics = useMemo<MetricCardData[]>(() => {
    const totalLogs = logs.length
    const humanHandoffs = logs.filter((log) => log.requiresHumanHandoff).length
    const autoResolved = totalLogs === 0 ? 0 : ((totalLogs - humanHandoffs) / totalLogs) * 100
    const averageConfidence =
      totalLogs === 0
        ? 0
        : logs.reduce((sum, log) => sum + normalizeConfidence(log.confidence), 0) / totalLogs
    const suggestedToday = logs.filter((log) => {
      const created = new Date(log.createdAt)
      const today = new Date()
      return created.toDateString() === today.toDateString()
    }).length

    return [
      {
        accent: config.active ? 'green' : 'orange',
        description: config.active
          ? 'Asistente inteligente en funcionamiento'
          : 'Asistente pausado desde configuracion',
        icon: 'spark',
        title: 'IA activa',
        value: config.active ? 'Si' : 'No',
      },
      {
        accent: 'blue',
        description: 'Estimado desde los últimos análisis de intencion',
        icon: 'chat',
        title: 'Conversaciones resueltas',
        value: totalLogs === 0 ? 'Sin datos' : formatPercent(autoResolved),
      },
      {
        accent: averageConfidence >= 70 ? 'green' : 'orange',
        description: 'Confianza promedio registrada por el motor de intenciones',
        icon: 'shield',
        title: 'Confianza medía',
        value: totalLogs === 0 ? 'Sin datos' : formatPercent(averageConfidence),
      },
      {
        accent: humanHandoffs > 0 ? 'orange' : 'green',
        description: 'Casos marcados para escalamiento humano',
        icon: 'human',
        title: 'Derivadas a humano',
        value: totalLogs === 0 ? '0%' : formatPercent((humanHandoffs / totalLogs) * 100),
      },
      {
        accent: 'green',
        description: 'Sugerencias generadas desde pruebas y mensajes reales',
        icon: 'send',
        title: 'Respuestas sugeridas hoy',
        value: String(suggestedToday),
      },
    ]
  }, [config.active, logs])

  const tabs = [
    { label: 'Servicios', value: 'services' },
    { label: 'Productos', value: 'products' },
    { label: 'Reglas IA', value: 'rules' },
    { label: 'Políticas', value: 'policies' },
    { label: 'Auditoría', value: 'audit' },
  ] as const

  const runSimulation = () => {
    if (!scenario.trim()) {
      showToast({
        description: 'Escribe una consulta de cliente para probar el comportamiento.',
        title: 'Escenario requerido',
        tone: 'warning',
      })
      return
    }

    analyzeMutation.mutate({ message: scenario.trim() })
  }

  const openCreateModal = () => {
    const type =
      activeTab === 'products'
        ? 'product'
        : activeTab === 'rules' || activeTab === 'policies'
          ? 'rule'
          : 'service'
    setEditor({
      ...emptyEditor,
      categoryCode:
        type === 'product'
          ? (productCategories[0]?.code ?? '')
          : (serviceCategories[0]?.code ?? ''),
      open: true,
      ruleType: activeTab === 'policies' ? 'SAFETY' : 'AI_RESPONSE',
      type,
    })
  }

  const openEditModal = (row: KnowledgeRow) => {
    if (row.type === 'audit') {
      setScenario(row.log?.sourceMessage ?? row.title)
      setPreviewResponse(row.log?.suggestedResponse ?? row.description)
      setAnalysisResult(null)
      showToast({
        description: 'Se cargo el mensaje auditado en el simulador.',
        title: 'Auditoría cargada',
        tone: 'success',
      })
      setActiveArea('conversation-preview')
      return
    }

    if (!isRegistroActivo(row.status)) {
      showToast({
        description: 'Activa el contenido antes de editarlo.',
        title: 'Contenido desactivado',
        tone: 'warning',
      })
      return
    }

    if (row.type === 'service' && row.service) {
      setEditor({
        ...emptyEditor,
        active: row.service.active,
        categoryCode: row.service.categoryCode,
        description: row.service.description,
        durationMinutes: String(row.service.durationMinutes),
        id: row.service.id,
        mode: 'edit',
        open: true,
        price: String(row.service.priceBase),
        source: row,
        title: row.service.name,
        type: 'service',
      })
      return
    }

    if (row.type === 'product' && row.product) {
      setEditor({
        ...emptyEditor,
        active: row.product.active,
        categoryCode: row.product.categoryCode,
        description: row.product.description,
        id: row.product.id,
        mode: 'edit',
        open: true,
        price: String(row.product.price),
        source: row,
        stock: String(row.product.stock),
        title: row.product.name,
        type: 'product',
      })
      return
    }

    if (row.rule) {
      setEditor({
        ...emptyEditor,
        active: row.rule.active,
        description: row.rule.description,
        id: row.rule.id,
        mode: 'edit',
        open: true,
        priority: String(row.rule.priority),
        ruleType: row.rule.ruleType,
        source: row,
        title: row.rule.name,
        type: 'rule',
      })
    }
  }

  const approvePreview = () => {
    const text = (previewResponse || analysisResult?.respuestaSugerida || '').trim()
    if (!text) {
      showToast({
        description: 'Primero genera o escribe una respuesta.',
        title: 'No hay respuesta para aprobar',
        tone: 'warning',
      })
      return
    }

    if (!selectedConversationId) {
      showToast({
        description: 'Selecciona una conversación activa para enviar la respuesta al cliente.',
        title: 'Falta destinatario',
        tone: 'warning',
      })
      return
    }

    sendApprovedMutation.mutate({ body: text, conversationId: selectedConversationId })
  }

  const isLoading =
    servicesQuery.isLoading ||
    productsQuery.isLoading ||
    rulesQuery.isLoading ||
    logsQuery.isLoading
  const hasError =
    servicesQuery.isError || productsQuery.isError || rulesQuery.isError || logsQuery.isError

  const sortedLogs = useMemo(() => sortAuditLogsDescending(logs), [logs])
  const totalAuditPages = getAuditTotalPages(sortedLogs.length)
  const resolvedAuditPage = Math.min(auditPage, totalAuditPages - 1)
  const paginatedLogs = paginateAuditLogs(sortedLogs, resolvedAuditPage)

  const saveBusinessHours = () => {
    const invalidDay = businessHours.find((day) => !isBusinessHourRangeValid(day))
    if (invalidDay) {
      businessHoursDispatch({ type: 'MARK_UNSAVED' })
      setBusinessHoursError(
        `Revisa el horario de ${invalidDay.day}: la hora de inicio debe ser menor que la hora de término.`,
      )
      return
    }

    setBusinessHoursError(null)
    saveHoursMutation.mutate()
  }

  return (
    <section className="space-y-4 overflow-hidden">
      <PageHeader
        actions={
          <>
            <Button
              loading={savePromptMutation.isPending}
              onClick={() => savePromptMutation.mutate()}
              variant="secondary"
            >
              Guardar instrucción
            </Button>
            <Button
              loading={analyzeMutation.isPending}
              onClick={() => {
                setActiveArea('conversation-preview')
                runSimulation()
              }}
            >
              Probar IA
            </Button>
          </>
        }
        description="Configura, entrena y supervisa el asistente inteligente de WhatsApp con datos reales del negocio."
        eyebrow="IA del Negocio"
        title="IA del negocio"
      />

      {hasError ? (
        <Card className="border-red-100 bg-red-50 p-4 text-sm text-red-700">
          No se pudo cargar parte de la información del negocio. Revisa que el servidor este
          disponible y vuelve a intentar.
        </Card>
      ) : null}

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
        {metrics.map((metric) => (
          <MetricCard key={metric.title} metric={metric} />
        ))}
      </div>

      <AreaTabs activeArea={activeArea} onChange={setActiveArea} />

      {activeArea === 'assistant-config' ? (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
          <AssistantConfigCard
            config={config}
            onChange={setConfig}
            prompt={assistantPrompt}
            setPrompt={setAssistantPrompt}
          />
          <BusinessHoursCard
            error={businessHoursError}
            hours={businessHours}
            loading={businessHoursQuery.isPending}
            locations={locations}
            selectedLocationId={hoursLocationId}
            saved={businessHoursSaved}
            onChange={(nextHours) => {
              businessHoursDispatch({ type: 'SET_HOURS', hours: nextHours })
            }}
            onLocationChange={setHoursLocationId}
            onSave={saveBusinessHours}
          />
        </div>
      ) : null}

      {activeArea === 'conversation-preview' ? (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.1fr)_minmax(0,0.95fr)]">
          <div className="space-y-4">
            <RulesCard
              allowedTopics={allowedTopics}
              blockedTopics={blockedTopics}
              config={config}
              onAllowedTopicsChange={setAllowedTopics}
              onBlockedTopicsChange={setBlockedTopics}
              onChange={setConfig}
            />
            <SimulatorCard
              analysisResult={analysisResult}
              loading={analyzeMutation.isPending}
              onRun={runSimulation}
              scenario={scenario}
              setScenario={setScenario}
            />
          </div>

          <ConversationPreviewCard
            analysisResult={analysisResult}
            conversations={conversations}
            conversationSearch={conversationSearch}
            editable={previewEditable}
            onApprove={approvePreview}
            onConversationSearchChange={setConversationSearch}
            onEdit={() => setPreviewEditable((current) => !current)}
            previewResponse={previewResponse || analysisResult?.respuestaSugerida || ''}
            scenario={scenario}
            selectedConversationId={selectedConversationId}
            sending={sendApprovedMutation.isPending}
            setPreviewResponse={setPreviewResponse}
            setSelectedConversationId={setSelectedConversationId}
          />

          <AuditCard
            logs={paginatedLogs}
            onPageChange={setAuditPage}
            onSelectLog={(log) => openEditModal(logToRow(log))}
            page={resolvedAuditPage}
            totalLogs={sortedLogs.length}
            totalPages={totalAuditPages}
          />
        </div>
      ) : null}

      {activeArea === 'knowledge-base' ? (
        <KnowledgeBaseCard
          activeTab={activeTab}
          isLoading={isLoading}
          onAdd={openCreateModal}
          onEdit={openEditModal}
          onOpenFullBase={() => setShowBaseModal(true)}
          onPageChange={setKnowledgePage}
          onSearchChange={(value) => {
            setSearch(value)
            setKnowledgePage(0)
          }}
          onStatusChange={(value) => {
            setStatusFilter(value)
            setKnowledgePage(0)
          }}
          onTabChange={(value) => {
            setActiveTab(value)
            setKnowledgePage(0)
          }}
          onToggleStatus={(row, active) => setRowToToggle({ active, row })}
          page={resolvedKnowledgePage}
          pageSize={KNOWLEDGE_PAGE_SIZE}
          rows={paginatedRows}
          search={search}
          statusChanging={statusMutation.isPending}
          statusFilter={statusFilter}
          tabs={tabs}
          totalPages={totalKnowledgePages}
          totalRows={rows.length}
        />
      ) : null}

      <KnowledgeBaseModal
        onClose={() => setShowBaseModal(false)}
        open={showBaseModal}
        products={products}
        rules={rules}
        services={services}
      />

      <ContentEditorModal
        editor={editor}
        loading={saveEditorMutation.isPending}
        onClose={() => setEditor(emptyEditor)}
        onChange={setEditor}
        onSave={() => saveEditorMutation.mutate(editor)}
        productCategories={productCategories}
        serviceCategories={serviceCategories}
      />

      <ConfirmDialog
        confirmLabel={rowToToggle?.active ? 'Activar' : 'Desactivar'}
        confirmLoading={statusMutation.isPending}
        description={`El contenido "${rowToToggle?.row.title ?? ''}" quedará ${rowToToggle?.active ? 'activo' : 'desactivado'} para la IA del negocio.`}
        onCancel={() => setRowToToggle(null)}
        onConfirm={() => {
          if (rowToToggle) {
            statusMutation.mutate(rowToToggle)
          }
        }}
        open={Boolean(rowToToggle)}
        title={rowToToggle?.active ? 'Activar contenido' : 'Desactivar contenido'}
        tone={rowToToggle?.active ? 'neutral' : 'danger'}
      />
    </section>
  )
}

function AreaTabs({
  activeArea,
  onChange,
}: {
  activeArea: BusinessAiArea
  onChange: (area: BusinessAiArea) => void
}) {
  const items: { label: string; value: BusinessAiArea; description: string }[] = [
    {
      description: 'Prompt, tono y horario',
      label: 'Configuracion del asistente',
      value: 'assistant-config',
    },
    {
      description: 'Límites, auditoria y simulador',
      label: 'Vista previa de conversación',
      value: 'conversation-preview',
    },
    {
      description: 'Servicios, productos y reglas',
      label: 'Base de conocimiento',
      value: 'knowledge-base',
    },
  ]

  return (
    <div className="grid gap-3 md:grid-cols-3">
      {items.map((item) => (
        <button
          className={[
            'rounded-[20px] border px-4 py-3 text-left transition',
            activeArea === item.value
              ? 'border-emerald-200 bg-emerald-50 text-emerald-800 shadow-sm'
              : 'border-[var(--color-border)] bg-white text-slate-700 hover:border-emerald-200',
          ].join(' ')}
          key={item.value}
          onClick={() => onChange(item.value)}
          type="button"
        >
          <span className="block text-sm font-semibold">{item.label}</span>
          <span className="mt-1 block text-xs text-slate-500">{item.description}</span>
        </button>
      ))}
    </div>
  )
}

function MetricCard({ metric }: { metric: MetricCardData }) {
  const accentClasses = {
    blue: 'bg-blue-50 text-blue-700 ring-blue-100',
    green: 'bg-emerald-50 text-emerald-700 ring-emerald-100',
    orange: 'bg-amber-50 text-amber-700 ring-amber-100',
  }[metric.accent]

  return (
    <Card className="min-h-[128px] p-4">
      <div className="flex items-start justify-between gap-3">
        <div
          className={`inline-flex h-10 w-10 items-center justify-center rounded-[16px] ring-1 ${accentClasses}`}
        >
          <AiIcon name={metric.icon} />
        </div>
        <StatusBadge label="Hoy" tone={metric.accent === 'orange' ? 'warning' : 'success'} />
      </div>
      <p className="mt-3 text-sm font-medium text-slate-500">{metric.title}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-950">{metric.value}</p>
      <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-600">{metric.description}</p>
    </Card>
  )
}

function AssistantConfigCard({
  config,
  onChange,
  prompt,
  setPrompt,
}: {
  config: AssistantConfigState
  onChange: (next: AssistantConfigState) => void
  prompt: string
  setPrompt: (value: string) => void
}) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-lg font-semibold text-slate-950">Configuracion del asistente</p>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            Define modo, tono, idioma y reglas operativas.
          </p>
        </div>
        <Toggle
          checked={config.active}
          label="IA activada"
          onChange={(active) => onChange({ ...config, active })}
        />
      </div>

      <div className="mt-4 space-y-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Modo de funcionamiento</p>
          <div className="mt-2 grid gap-2 sm:grid-cols-2">
            <ModeOption
              checked={config.mode === 'suggest'}
              description="La IA sugiere y tu apruebas"
              label="Sugerir respuestas"
              onClick={() => onChange({ ...config, mode: 'suggest' })}
            />
            <ModeOption
              checked={config.mode === 'auto'}
              description="La IA responde al cliente"
              label="Responder automaticamente"
              onClick={() => onChange({ ...config, mode: 'auto' })}
            />
          </div>
        </div>

        <div>
          <p className="text-sm font-semibold text-slate-950">Tono de comunicacion</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {(['Cercano', 'Profesional', 'Comercial'] as const).map((tone) => (
              <button
                className={[
                  'rounded-full border px-3 py-2 text-sm font-semibold transition',
                  config.tone === tone
                    ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                    : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-emerald-200 hover:text-emerald-700',
                ].join(' ')}
                key={tone}
                onClick={() => onChange({ ...config, tone })}
                type="button"
              >
                {tone}
              </button>
            ))}
          </div>
        </div>

        <Textarea
          hint="Se guarda como regla del negocio y se usa como referencia operativa del asistente."
          label="Prompt operativo"
          onChange={(event) => setPrompt(event.target.value)}
          rows={5}
          value={prompt}
        />

        <div className="grid gap-3 sm:grid-cols-2">
          <Select
            label="Idioma de respuesta"
            onChange={(event) => onChange({ ...config, language: event.target.value })}
            options={[{ label: 'Espanol', value: 'es' }]}
            value={config.language}
          />
          <Select
            hint="Derivar a humano si la confianza es menor a"
            label="Regla de escalamiento"
            onChange={(event) => onChange({ ...config, escalationThreshold: event.target.value })}
            options={[
              { label: '60%', value: '60' },
              { label: '70%', value: '70' },
              { label: '80%', value: '80' },
              { label: '90%', value: '90' },
            ]}
            value={config.escalationThreshold}
          />
        </div>
      </div>
    </Card>
  )
}

function BusinessHoursCard({
  error,
  hours,
  loading,
  locations,
  selectedLocationId,
  onChange,
  onLocationChange,
  onSave,
  saved,
}: {
  error: string | null
  hours: BusinessHoursDay[]
  loading: boolean
  locations: BusinessLocationResponse[]
  selectedLocationId: string
  onChange: (hours: BusinessHoursDay[]) => void
  onLocationChange: (id: string) => void
  onSave: () => void
  saved: boolean
}) {
  const updateDay = (index: number, field: 'startTime' | 'endTime', value: string) => {
    onChange(
      hours.map((day, currentIndex) => (currentIndex === index ? { ...day, [field]: value } : day)),
    )
  }

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-lg font-semibold text-slate-950">Horario de atención</p>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            Configura hora de inicio y término para cada día según la sucursal.
          </p>
        </div>
        <StatusBadge label={saved ? 'Guardado' : 'Editable'} tone={saved ? 'success' : 'info'} />
      </div>

      <div className="mt-4">
        <label className="block text-sm font-semibold text-slate-700 mb-1">Sucursal</label>
        <select
          className="h-11 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm text-slate-700 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
          onChange={(event) => onLocationChange(event.target.value)}
          value={selectedLocationId}
        >
          {locations.map((loc) => (
            <option key={loc.id} value={loc.id}>
              {loc.name}
            </option>
          ))}
        </select>
      </div>

      {error ? (
        <div className="mt-4 rounded-[18px] border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-800">
          {error}
        </div>
      ) : null}

      {loading ? (
        <p className="mt-4 text-sm text-slate-500">Cargando horarios...</p>
      ) : (
        <div className="mt-4 grid gap-2">
          <div className="hidden grid-cols-[1fr_150px_150px] gap-3 px-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 sm:grid">
            <span>Día</span>
            <span>Hora Inicio</span>
            <span>Hora Término</span>
          </div>
          {hours.map((day, index) => (
            <div
              className="grid gap-2 rounded-[18px] border border-[var(--color-border)] bg-white p-3 sm:grid-cols-[1fr_150px_150px] sm:items-center"
              key={day.day}
            >
              <p className="text-sm font-semibold text-slate-950">{day.day}</p>
              <label className="block">
                <span className="mb-1 block text-xs font-semibold text-slate-500 sm:hidden">
                  Hora Inicio
                </span>
                <input
                  className="h-10 w-full rounded-[14px] border border-[var(--color-border)] px-3 text-sm outline-none focus:border-[var(--color-primary)]"
                  onChange={(event) => updateDay(index, 'startTime', event.target.value)}
                  type="time"
                  value={day.startTime}
                />
              </label>
              <label className="block">
                <span className="mb-1 block text-xs font-semibold text-slate-500 sm:hidden">
                  Hora Término
                </span>
                <input
                  className="h-10 w-full rounded-[14px] border border-[var(--color-border)] px-3 text-sm outline-none focus:border-[var(--color-primary)]"
                  onChange={(event) => updateDay(index, 'endTime', event.target.value)}
                  type="time"
                  value={day.endTime}
                />
              </label>
            </div>
          ))}
        </div>
      )}

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
        <p className="text-sm leading-6 text-slate-600">
          Estos horarios se usan en agenda completa, disponibilidad y reservas.
        </p>
        <Button onClick={onSave}>Guardar horario</Button>
      </div>
    </Card>
  )
}

function RulesCard({
  allowedTopics,
  blockedTopics,
  config,
  onAllowedTopicsChange,
  onBlockedTopicsChange,
  onChange,
}: {
  allowedTopics: Record<string, boolean>
  blockedTopics: Record<string, boolean>
  config: AssistantConfigState
  onAllowedTopicsChange: (topics: Record<string, boolean>) => void
  onBlockedTopicsChange: (topics: Record<string, boolean>) => void
  onChange: (next: AssistantConfigState) => void
}) {
  return (
    <Card className="p-5">
      <p className="text-lg font-semibold text-slate-950">Límites y reglas</p>
      <p className="mt-1 text-sm leading-6 text-slate-600">
        Temas permitidos y bloqueados vienen seleccionados por defecto.
      </p>

      <TopicChecklist
        className="mt-4"
        title="Temas permitidos"
        topics={allowedTopics}
        onChange={onAllowedTopicsChange}
      />
      <TopicChecklist
        className="mt-4"
        title="Temas bloqueados"
        topics={blockedTopics}
        onChange={onBlockedTopicsChange}
      />

      <div className="mt-4 grid gap-2">
        <Toggle
          checked={config.allowPrices}
          label="Permitir precios"
          onChange={(allowPrices) => onChange({ ...config, allowPrices })}
        />
        <Toggle
          checked={config.allowBooking}
          label="Permitir agendamiento"
          onChange={(allowBooking) => onChange({ ...config, allowBooking })}
        />
        <Toggle
          checked={config.allowPromotions}
          label="Permitir compartir promociones"
          onChange={(allowPromotions) => onChange({ ...config, allowPromotions })}
        />
        <Toggle
          checked={config.requireAvailabilityCheck}
          label="No prometer disponibilidad sin verificar agenda"
          onChange={(requireAvailabilityCheck) => onChange({ ...config, requireAvailabilityCheck })}
        />
      </div>
    </Card>
  )
}

function TopicChecklist({
  className,
  onChange,
  title,
  topics,
}: {
  className?: string
  onChange: (topics: Record<string, boolean>) => void
  title: string
  topics: Record<string, boolean>
}) {
  return (
    <div className={className}>
      <p className="text-sm font-semibold text-slate-950">{title}</p>
      <div className="mt-2 grid gap-2 sm:grid-cols-2">
        {Object.entries(topics).map(([topic, checked]) => (
          <label
            className="flex items-center gap-2 rounded-[14px] border border-[var(--color-border)] bg-white px-3 py-2 text-sm font-medium text-slate-700"
            key={topic}
          >
            <input
              checked={checked}
              className="h-4 w-4"
              onChange={(event) => onChange({ ...topics, [topic]: event.target.checked })}
              type="checkbox"
            />
            {topic}
          </label>
        ))}
      </div>
    </div>
  )
}

function KnowledgeBaseCard({
  activeTab,
  isLoading,
  onAdd,
  onEdit,
  onOpenFullBase,
  onPageChange,
  onSearchChange,
  onStatusChange,
  onTabChange,
  onToggleStatus,
  page,
  pageSize,
  rows,
  search,
  statusChanging,
  statusFilter,
  tabs,
  totalPages,
  totalRows,
}: {
  activeTab: KnowledgeTab
  isLoading: boolean
  onAdd: () => void
  onEdit: (row: KnowledgeRow) => void
  onOpenFullBase: () => void
  onPageChange: (page: number) => void
  onSearchChange: (value: string) => void
  onStatusChange: (value: string) => void
  onTabChange: (value: KnowledgeTab) => void
  onToggleStatus: (row: KnowledgeRow, active: boolean) => void
  page: number
  pageSize: number
  rows: KnowledgeRow[]
  search: string
  statusChanging: boolean
  statusFilter: string
  tabs: readonly { label: string; value: KnowledgeTab }[]
  totalPages: number
  totalRows: number
}) {
  const fromRow = totalRows === 0 ? 0 : page * pageSize + 1
  const toRow = Math.min((page + 1) * pageSize, totalRows)

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-lg font-semibold text-slate-950">Base de conocimiento</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Informacion real de servicios, productos, reglas y auditoria que usa la IA.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button onClick={onOpenFullBase} variant="secondary">
            Ver base completa
          </Button>
          <Button onClick={onAdd}>Agregar contenido</Button>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <button
            className={[
              'rounded-full border px-4 py-2 text-sm font-semibold transition',
              activeTab === tab.value
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-emerald-200 hover:text-emerald-700',
            ].join(' ')}
            key={tab.value}
            onClick={() => onTabChange(tab.value)}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-5 grid gap-3 md:grid-cols-[1fr_220px]">
        <Input
          label="Buscar"
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Buscar en base de conocimiento..."
          value={search}
        />
        <Select
          label="Estado"
          onChange={(event) => onStatusChange(event.target.value)}
          options={[
            { label: 'Todos los estados', value: 'all' },
            { label: 'Activo', value: 'Activo' },
            { label: 'Desactivado', value: 'Desactivado' },
            { label: 'Sincronizado', value: 'Sincronizado' },
            { label: 'Requiere derivación', value: 'Requiere derivación' },
          ]}
          value={statusFilter}
        />
      </div>

      <div className="mt-5 grid gap-3" data-testid="knowledge-base-list">
        <div className="hidden rounded-[18px] border border-[var(--color-border)] bg-slate-50 px-4 py-3 text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 md:grid md:grid-cols-[minmax(0,1fr)_160px_130px_150px_auto] md:items-center">
          <span>Contenido</span>
          <span>Categoría</span>
          <span>Estado</span>
          <span>Actualización</span>
          <span className="text-right">Acciones</span>
        </div>
        {isLoading ? (
          <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            Cargando datos del negocio...
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            No hay registros para los filtros seleccionados.
          </div>
        ) : (
          rows.map((item) => {
            const hasEditableStatus = item.type !== 'audit'
            const active = isRegistroActivo(item.status)
            const editDisabled = hasEditableStatus && !active
            const statusTone =
              item.status === 'Requiere derivación'
                ? 'warning'
                : hasEditableStatus
                  ? getRegistroTone(item.status)
                  : 'info'

            return (
              <article
                className={[
                  'grid gap-3 rounded-[20px] border bg-white p-4 md:grid-cols-[minmax(0,1fr)_160px_130px_150px_auto] md:items-center',
                  editDisabled ? 'border-amber-200' : 'border-[var(--color-border)]',
                ].join(' ')}
                key={`${item.type}-${item.id}`}
              >
                <div className="min-w-0">
                  <p className="line-clamp-2 text-sm font-semibold text-slate-950">{item.title}</p>
                  <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
                    {item.description}
                  </p>
                </div>
                <p className="text-sm text-slate-600">{item.category}</p>
                <StatusBadge label={item.status} tone={statusTone} />
                <p className="text-sm text-slate-600">{item.updatedAt}</p>
                <div className="flex flex-wrap gap-2 md:justify-end">
                  <Button
                    disabled={editDisabled}
                    onClick={() => onEdit(item)}
                    size="sm"
                    title={
                      editDisabled
                        ? 'Activa el contenido antes de editarlo.'
                        : item.type === 'audit'
                          ? 'Usar este registro de auditoria'
                          : 'Editar contenido'
                    }
                    variant="secondary"
                  >
                    {item.type === 'audit' ? 'Usar' : 'Editar'}
                  </Button>
                  {hasEditableStatus ? (
                    <Button
                      disabled={statusChanging}
                      onClick={() => onToggleStatus(item, !active)}
                      size="sm"
                      title={active ? 'Desactivar contenido' : 'Activar contenido'}
                      variant={active ? 'danger' : 'secondary'}
                    >
                      {active ? 'Desactivar' : 'Activar'}
                    </Button>
                  ) : null}
                </div>
              </article>
            )
          })
        )}
      </div>
      <div className="mt-4 flex flex-col gap-3 rounded-[18px] border border-[var(--color-border)] bg-slate-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-600">
          Mostrando {fromRow}-{toRow} de {totalRows} registros · 10 por página
        </p>
        <div className="flex gap-2">
          <Button
            disabled={page === 0 || isLoading}
            onClick={() => onPageChange(Math.max(page - 1, 0))}
            size="sm"
            variant="secondary"
          >
            Anterior
          </Button>
          <Button
            disabled={page >= totalPages - 1 || isLoading}
            onClick={() => onPageChange(Math.min(page + 1, totalPages - 1))}
            size="sm"
            variant="secondary"
          >
            Siguiente
          </Button>
        </div>
      </div>
    </Card>
  )
}

function ConversationPreviewCard({
  analysisResult,
  conversations,
  conversationSearch,
  editable,
  onApprove,
  onConversationSearchChange,
  onEdit,
  previewResponse,
  scenario,
  selectedConversationId,
  sending,
  setPreviewResponse,
  setSelectedConversationId,
}: {
  analysisResult: IntentAnalysisResponse | null
  conversations: ConversationSummaryResponse[]
  conversationSearch: string
  editable: boolean
  onApprove: () => void
  onConversationSearchChange: (value: string) => void
  onEdit: () => void
  previewResponse: string
  scenario: string
  selectedConversationId: string
  sending: boolean
  setPreviewResponse: (value: string) => void
  setSelectedConversationId: (value: string) => void
}) {
  const confidence = analysisResult
    ? formatPercent(normalizeConfidence(analysisResult.confianza))
    : 'Sin prueba'
  const response =
    previewResponse ||
    'Ejecuta una prueba para generar una respuesta con la base de conocimiento del negocio.'

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-lg font-semibold text-slate-950">Vista previa de conversación</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Asi responderia la IA en tiempo real.
          </p>
        </div>
        <StatusBadge
          label={`Confianza: ${confidence}`}
          tone={analysisResult?.requiereDerivacionHumana ? 'warning' : 'success'}
        />
      </div>

      <div className="mt-5 grid gap-3">
        <Input
          label="Buscar conversación activa"
          onChange={(event) => onConversationSearchChange(event.target.value)}
          placeholder="Nombre, telefono o ultimo mensaje..."
          value={conversationSearch}
        />
        <Select
          hint="La respuesta aprobada se enviara al cliente de esta conversación."
          label="Destinatario activo"
          onChange={(event) => setSelectedConversationId(event.target.value)}
          options={[
            { label: 'Selecciona una conversación', value: '' },
            ...conversations.map((conversation) => ({
              label: `${conversation.customerName} · ${conversation.customerPhone} · ${conversation.lastMessagePreview ?? 'Sin ultimo mensaje'}`,
              value: conversation.id,
            })),
          ]}
          value={selectedConversationId}
        />
      </div>

      <div className="mt-6 rounded-[24px] border border-emerald-100 bg-[#F3FBF7] p-4">
        <ChatBubble align="left">{scenario}</ChatBubble>
        {editable ? (
          <div className="mt-4">
            <Textarea
              label="Respuesta editable"
              onChange={(event) => setPreviewResponse(event.target.value)}
              rows={8}
              value={response}
            />
          </div>
        ) : (
          <ChatBubble align="right">{response}</ChatBubble>
        )}
        <p className="mt-3 text-xs font-medium text-emerald-700">
          {analysisResult ? `Intencion: ${analysisResult.intencion}` : 'Respuesta generada por IA'}
        </p>
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <Button onClick={onEdit} variant="secondary">
          {editable ? 'Terminar edición' : 'Editar'}
        </Button>
        <Button disabled={!selectedConversationId} loading={sending} onClick={onApprove}>
          Aprobar y enviar
        </Button>
      </div>
    </Card>
  )
}

function SimulatorCard({
  analysisResult,
  loading,
  onRun,
  scenario,
  setScenario,
}: {
  analysisResult: IntentAnalysisResponse | null
  loading: boolean
  onRun: () => void
  scenario: string
  setScenario: (value: string) => void
}) {
  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-lg font-semibold text-slate-950">Simulador de conversaciónes</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Prueba como respondera tu IA a diferentes preguntas de tus clientes.
          </p>
        </div>
        <StatusBadge
          label={
            analysisResult
              ? formatPercent(normalizeConfidence(analysisResult.confianza))
              : 'Sin prueba'
          }
          tone="info"
        />
      </div>

      <div className="mt-5">
        <Textarea
          label="Escenario / Prompt"
          onChange={(event) => setScenario(event.target.value)}
          rows={5}
          value={scenario}
        />
      </div>

      <div className="mt-5 rounded-[22px] border border-[var(--color-border)] bg-slate-50 px-5 py-5">
        <p className="text-sm font-semibold text-slate-950">Respuesta generada por IA</p>
        <p className="mt-3 whitespace-pre-line text-sm leading-6 text-slate-700">
          {analysisResult?.respuestaSugerida ??
            'Ejecuta una prueba para ver la respuesta sugerida.'}
        </p>
      </div>

      <div className="mt-5 flex flex-wrap gap-3">
        <Button loading={loading} onClick={onRun}>
          Probar
        </Button>
      </div>
    </Card>
  )
}

function AuditCard({
  logs,
  onPageChange,
  onSelectLog,
  page,
  totalLogs,
  totalPages,
}: {
  logs: AestheticIntentLogResponse[]
  onPageChange: (page: number) => void
  onSelectLog: (log: AestheticIntentLogResponse) => void
  page: number
  totalLogs: number
  totalPages: number
}) {
  const fromLog = totalLogs === 0 ? 0 : page * BUSINESS_AI_AUDIT_PAGE_SIZE + 1
  const toLog = Math.min((page + 1) * BUSINESS_AI_AUDIT_PAGE_SIZE, totalLogs)

  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-lg font-semibold text-slate-950">Auditoría IA</p>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            Últimos 5 mensajes, ordenados del más reciente al más antiguo.
          </p>
        </div>
        <StatusBadge label="5 por página" tone="info" />
      </div>

      <div className="mt-4 space-y-3">
        {logs.length === 0 ? (
          <p className="rounded-[18px] border border-[var(--color-border)] bg-slate-50 p-4 text-sm leading-6 text-slate-600">
            Aún no hay análisis registrados. Ejecuta una prueba para crear el primer registro.
          </p>
        ) : (
          logs.map((item) => (
            <button
              className="flex w-full gap-3 rounded-[18px] border border-[var(--color-border)] bg-white p-3 text-left transition hover:border-emerald-200"
              key={item.id}
              onClick={() => onSelectLog(item)}
              type="button"
            >
              <span
                className={[
                  'mt-1 h-2.5 w-2.5 shrink-0 rounded-full',
                  item.requiresHumanHandoff
                    ? 'bg-amber-500'
                    : normalizeConfidence(item.confidence) >= 70
                      ? 'bg-emerald-500'
                      : 'bg-blue-500',
                ].join(' ')}
              />
              <span className="min-w-0">
                <span className="block text-sm font-semibold text-slate-950">{item.intent}</span>
                <span className="mt-1 line-clamp-2 block text-sm leading-5 text-slate-600">
                  {item.sourceMessage}
                </span>
                <span className="mt-1 block text-xs font-medium text-slate-400">
                  {formatDateTime(item.createdAt)} · Confianza{' '}
                  {formatPercent(normalizeConfidence(item.confidence))}
                </span>
              </span>
            </button>
          ))
        )}
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
        <p className="text-sm text-slate-600">
          Mostrando {fromLog}-{toLog} de {totalLogs}
        </p>
        <div className="flex gap-2">
          <Button
            disabled={page === 0}
            onClick={() => onPageChange(Math.max(page - 1, 0))}
            size="sm"
            variant="secondary"
          >
            Anterior
          </Button>
          <Button
            disabled={page >= totalPages - 1}
            onClick={() => onPageChange(Math.min(page + 1, totalPages - 1))}
            size="sm"
            variant="secondary"
          >
            Siguiente
          </Button>
        </div>
      </div>
    </Card>
  )
}

function ContentEditorModal({
  editor,
  loading,
  onChange,
  onClose,
  onSave,
  productCategories,
  serviceCategories,
}: {
  editor: EditorState
  loading: boolean
  onChange: (next: EditorState) => void
  onClose: () => void
  onSave: () => void
  productCategories: { code: string; name: string }[]
  serviceCategories: { code: string; name: string }[]
}) {
  if (!editor.open) {
    return null
  }

  const categoryOptions =
    editor.type === 'product'
      ? productCategories.map((category) => ({ label: category.name, value: category.code }))
      : serviceCategories.map((category) => ({ label: category.name, value: category.code }))

  const typeOptions = [
    { label: 'Servicio', value: 'service' },
    { label: 'Producto', value: 'product' },
    { label: 'Regla IA', value: 'rule' },
  ]

  const isValid = editor.title.trim() && editor.description.trim()

  return (
    <Modal maxWidthClassName="max-w-[720px]" onClose={onClose} open={editor.open}>
      <div className="space-y-5">
        <div>
          <p className="text-xl font-semibold text-slate-950">
            {editor.mode === 'create' ? 'Agregar contenido' : 'Editar contenido'}
          </p>
          <p className="mt-2 text-sm text-slate-600">
            Actualiza la base que usa la IA para responder sobre el negocio.
          </p>
        </div>

        <Select
          disabled={editor.mode === 'edit'}
          label="Tipo"
          onChange={(event) =>
            onChange({ ...editor, type: event.target.value as EditorState['type'] })
          }
          options={typeOptions}
          value={editor.type}
        />

        <Input
          label="Titulo"
          onChange={(event) => onChange({ ...editor, title: event.target.value })}
          value={editor.title}
        />
        <Textarea
          label="Descripcion"
          onChange={(event) => onChange({ ...editor, description: event.target.value })}
          rows={5}
          value={editor.description}
        />

        {editor.type === 'service' ? (
          <div className="grid gap-4 sm:grid-cols-3">
            <Select
              label="Categoria"
              onChange={(event) => onChange({ ...editor, categoryCode: event.target.value })}
              options={
                categoryOptions.length
                  ? categoryOptions
                  : [{ label: 'Depilacion', value: 'DEPILACION' }]
              }
              value={editor.categoryCode}
            />
            <Input
              label="Precio"
              onChange={(event) => onChange({ ...editor, price: event.target.value })}
              type="number"
              value={editor.price}
            />
            <Input
              label="Duración"
              onChange={(event) => onChange({ ...editor, durationMinutes: event.target.value })}
              type="number"
              value={editor.durationMinutes}
            />
          </div>
        ) : null}

        {editor.type === 'product' ? (
          <div className="grid gap-4 sm:grid-cols-3">
            <Select
              label="Categoria"
              onChange={(event) => onChange({ ...editor, categoryCode: event.target.value })}
              options={
                categoryOptions.length
                  ? categoryOptions
                  : [{ label: 'Post tratamiento', value: 'POST_TRATAMIENTO' }]
              }
              value={editor.categoryCode}
            />
            <Input
              label="Precio"
              onChange={(event) => onChange({ ...editor, price: event.target.value })}
              type="number"
              value={editor.price}
            />
            <Input
              label="Stock"
              onChange={(event) => onChange({ ...editor, stock: event.target.value })}
              type="number"
              value={editor.stock}
            />
          </div>
        ) : null}

        {editor.type === 'rule' ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Select
              label="Tipo de regla"
              onChange={(event) => onChange({ ...editor, ruleType: event.target.value })}
              options={[
                { label: 'Respuesta IA', value: 'AI_RESPONSE' },
                { label: 'Seguridad', value: 'SAFETY' },
                { label: 'Disponibilidad', value: 'AVAILABILITY' },
                { label: 'Comercial', value: 'COMMERCIAL' },
                { label: 'Catálogo', value: 'CATALOG' },
                { label: 'Prompt IA', value: 'AI_PROMPT' },
              ]}
              value={editor.ruleType}
            />
            <Input
              label="Prioridad"
              onChange={(event) => onChange({ ...editor, priority: event.target.value })}
              type="number"
              value={editor.priority}
            />
          </div>
        ) : null}

        <Toggle
          checked={editor.active}
          label="Contenido activo"
          onChange={(active) => onChange({ ...editor, active })}
        />

        <div className="flex justify-end gap-3">
          <Button onClick={onClose} variant="secondary">
            Cancelar
          </Button>
          <Button disabled={!isValid} loading={loading} onClick={onSave}>
            Guardar
          </Button>
        </div>
      </div>
    </Modal>
  )
}

function KnowledgeBaseModal({
  onClose,
  open,
  products,
  rules,
  services,
}: {
  onClose: () => void
  open: boolean
  products: AestheticProductResponse[]
  rules: AestheticBusinessRuleResponse[]
  services: AestheticServiceResponse[]
}) {
  return (
    <Modal maxWidthClassName="max-w-[980px]" onClose={onClose} open={open}>
      <div>
        <p className="text-xl font-semibold text-slate-950">Base completa del negocio</p>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          Resumen operativo de servicios, productos y reglas que alimentan la IA.
        </p>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-3">
        <KnowledgeColumn
          title="Servicios"
          items={services.map(
            (service) =>
              `${service.name} · ${formatMoney(service.priceBase)} · ${service.durationMinutes} min`,
          )}
        />
        <KnowledgeColumn
          title="Productos"
          items={products.map(
            (product) => `${product.name} · ${formatMoney(product.price)} · stock ${product.stock}`,
          )}
        />
        <KnowledgeColumn
          title="Reglas"
          items={rules.map((rule) => `${rule.name} · ${rule.ruleType}`)}
        />
      </div>

      <div className="mt-6 flex justify-end">
        <Button onClick={onClose}>Cerrar</Button>
      </div>
    </Modal>
  )
}

function KnowledgeColumn({ items, title }: { items: string[]; title: string }) {
  return (
    <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 p-4">
      <p className="text-sm font-semibold uppercase tracking-[0.12em] text-slate-500">{title}</p>
      <div className="mt-3 max-h-[360px] space-y-2 overflow-auto pr-1">
        {items.length === 0 ? (
          <p className="text-sm text-slate-500">Sin registros.</p>
        ) : (
          items.map((item) => (
            <p
              className="rounded-2xl bg-white px-3 py-2 text-sm leading-5 text-slate-700"
              key={item}
            >
              {item}
            </p>
          ))
        )}
      </div>
    </div>
  )
}

function ModeOption({
  checked,
  description,
  label,
  onClick,
}: {
  checked?: boolean
  description: string
  label: string
  onClick: () => void
}) {
  return (
    <button
      className={[
        'flex items-start gap-3 rounded-[18px] border px-4 py-3 text-left transition',
        checked
          ? 'border-emerald-200 bg-emerald-50'
          : 'border-[var(--color-border)] bg-white hover:border-emerald-200',
      ].join(' ')}
      onClick={onClick}
      type="button"
    >
      <span
        className={[
          'mt-1 h-4 w-4 rounded-full border',
          checked
            ? 'border-emerald-500 bg-emerald-500 shadow-[inset_0_0_0_4px_white]'
            : 'border-slate-300 bg-white',
        ].join(' ')}
      />
      <span>
        <span className="block text-sm font-semibold text-slate-950">{label}</span>
        <span className="mt-1 block text-sm text-slate-600">{description}</span>
      </span>
    </button>
  )
}

function Toggle({
  checked,
  label,
  onChange,
}: {
  checked?: boolean
  label: string
  onChange: (checked: boolean) => void
}) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-4 rounded-[18px] border border-[var(--color-border)] bg-white px-4 py-3">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <span
        className={[
          'relative h-6 w-11 shrink-0 rounded-full transition',
          checked ? 'bg-emerald-500' : 'bg-slate-300',
        ].join(' ')}
      >
        <input
          checked={Boolean(checked)}
          className="sr-only"
          onChange={(event) => onChange(event.target.checked)}
          type="checkbox"
        />
        <span
          className={[
            'absolute top-1 h-4 w-4 rounded-full bg-white shadow-sm transition',
            checked ? 'left-6' : 'left-1',
          ].join(' ')}
        />
      </span>
    </label>
  )
}

function ChatBubble({ align, children }: { align: 'left' | 'right'; children: string }) {
  return (
    <div className={align === 'right' ? 'mt-4 flex justify-end' : 'flex justify-start'}>
      <div
        className={[
          'max-w-[92%] whitespace-pre-line rounded-[20px] px-4 py-3 text-sm leading-6 shadow-sm',
          align === 'right'
            ? 'rounded-br-[6px] bg-emerald-600 text-white'
            : 'rounded-bl-[6px] bg-white text-slate-700',
        ].join(' ')}
      >
        {children}
      </div>
    </div>
  )
}

function AiIcon({ name }: { name: MetricCardData['icon'] }) {
  const commonProps = {
    className: 'h-5 w-5',
    fill: 'none',
    viewBox: '0 0 24 24',
    xmlns: 'http://www.w3.org/2000/svg',
  }

  if (name === 'chat') {
    return (
      <svg {...commonProps}>
        <path
          d="M5 7.5C5 6.12 6.12 5 7.5 5H16.5C17.88 5 19 6.12 19 7.5V13.5C19 14.88 17.88 16 16.5 16H11L7 19V16H7.5C6.12 16 5 14.88 5 13.5V7.5Z"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
      </svg>
    )
  }

  if (name === 'shield') {
    return (
      <svg {...commonProps}>
        <path
          d="M12 3L19 6V11.5C19 16.1 15.8 20.37 12 21.5C8.2 20.37 5 16.1 5 11.5V6L12 3Z"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
        <path
          d="M9 12L11 14L15.5 9.5"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
      </svg>
    )
  }

  if (name === 'human') {
    return (
      <svg {...commonProps}>
        <path
          d="M12 12C14.21 12 16 10.21 16 8C16 5.79 14.21 4 12 4C9.79 4 8 5.79 8 8C8 10.21 9.79 12 12 12Z"
          stroke="currentColor"
          strokeWidth="1.8"
        />
        <path
          d="M5 19C5 16.79 8.13 15 12 15C15.87 15 19 16.79 19 19"
          stroke="currentColor"
          strokeLinecap="round"
          strokeWidth="1.8"
        />
      </svg>
    )
  }

  if (name === 'send') {
    return (
      <svg {...commonProps}>
        <path
          d="M20 4L10.5 13.5"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
        <path
          d="M20 4L14 20L10.5 13.5L4 10L20 4Z"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
      </svg>
    )
  }

  return (
    <svg {...commonProps}>
      <path
        d="M12 3L13.7 8.3L19 10L13.7 11.7L12 17L10.3 11.7L5 10L10.3 8.3L12 3Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M18 15L18.8 17.2L21 18L18.8 18.8L18 21L17.2 18.8L15 18L17.2 17.2L18 15Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    </svg>
  )
}
