export type NavigationItem = {
  label: string
  path: string
  description: string
  allowedRoles?: string[]
  allowedPermissions?: string[]
  children?: NavigationItem[]
}

export type RouteMeta = {
  title: string
  description: string
  path?: string
  matcher?: RegExp
}

export const ADMIN_SUBMENU_ITEMS: NavigationItem[] = [
  {
    label: 'Empresa',
    path: '/admin/company',
    description: 'Configuración de la empresa.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Sucursales',
    path: '/admin/branches',
    description: 'Administra sucursales del negocio.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Cabinas',
    path: '/admin/rooms',
    description: 'Gestiona cabinas, salas y recursos por sede.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Servicios',
    path: '/admin/services',
    description: 'Servicios del catálogo comercial.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Asignaciones',
    path: '/admin/assignments',
    description: 'Asigna profesionales y cabinas a servicios.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'MultiSede',
    path: '/admin/multisite',
    description: 'Disponibilidad, stock, profesionales, permisos y canales por sede.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'WhatsApp Web',
    path: '/admin/whatsapp-web',
    description: 'Conexion y estado de WhatsApp Web.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Usuarios y Roles',
    path: '/admin/users',
    description: 'Listado de usuarios, filtros y resumen de roles disponibles.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Seguridad',
    path: '/admin/security',
    description: 'Políticas de contraseña, sesión y bloqueo del negocio.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Profesionales',
    path: '/admin/professionals',
    description: 'Gestiona profesionales, especialidades, contacto y sedes.',
    allowedRoles: ['OWNER', 'ADMIN'],
    allowedPermissions: ['PROFESSIONAL_VIEW'],
  },
]

export const PRIMARY_NAV_ITEMS: NavigationItem[] = [
  {
    label: 'Dashboard',
    path: '/dashboard',
    description: 'Panel principal con KPIs, actividad y accesos rapidos.',
  },
  {
    label: 'Conversaciones',
    path: '/conversations',
    description: 'Listado y detalle de conversaciones de WhatsApp.',
  },
  {
    label: 'Prospectos',
    path: '/prospects',
    description: 'Embudo comercial y seguimiento de leads.',
  },
  {
    label: 'Agenda semanal',
    path: '/agenda',
    description:
      'Disponibilidad real, reservas temporales, cabinas, profesionales y confirmacion por WhatsApp.',
  },
  {
    label: 'Citas',
    path: '/appointments',
    description: 'Citas pendientes y reprogramaciones.',
  },
  {
    label: 'Catálogo',
    path: '/catalog',
    description: 'Productos y servicios del catálogo comercial.',
  },
  {
    label: 'Reglas',
    path: '/automation-rules',
    description: 'Automatizaciones simples disparadas por eventos.',
  },
  {
    label: 'IA del Negocio',
    path: '/business-ai',
    description: 'Orquestacion, confianza, respuestas sugeridas y escalamiento humano.',
    allowedPermissions: ['BUSINESS_AI_VIEW'],
  },
  {
    label: 'Reportes',
    path: '/reports',
    description: 'Indicadores basicos filtrados por rango y responsable.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Administración',
    path: '/admin',
    description: 'Empresa, sucursales, cabinas, servicios, asignaciones, multisede, WhatsApp Web, usuarios, seguridad y profesionales.',
    allowedRoles: ['OWNER', 'ADMIN'],
    children: ADMIN_SUBMENU_ITEMS,
  },
]

export function canAccessNavigationItem(
  item: NavigationItem,
  role?: string | null,
  permissions: string[] = [],
) {
  const allowedByRole = !item.allowedRoles || Boolean(role && item.allowedRoles.includes(role))
  const allowedByPermission =
    !item.allowedPermissions ||
    permissions.includes('ALL') ||
    item.allowedPermissions.some((permission) => permissions.includes(permission))

  return allowedByRole && allowedByPermission
}

const ROUTE_METADATA: RouteMeta[] = [
  {
    path: '/dashboard',
    title: 'Panel principal',
    description: 'Resumen comercial, agenda del dia y actividad reciente.',
  },
  {
    path: '/notifications',
    title: 'Centro de notificaciones',
    description: 'Listado basico de alertas internas y accesos relacionados.',
  },
  {
    path: '/profile/change-password',
    title: 'Cambiar contraseña',
    description: 'Pantalla reservada para la actualización de credenciales del usuario.',
  },
  {
    path: '/profile',
    title: 'Perfil de usuario',
    description: 'Datos personales, zona horaria y configuración base del usuario.',
  },
  {
    path: '/conversations/new',
    title: 'Nueva conversacion',
    description: 'Alta manual para iniciar una conversación desde el shell privado.',
  },
  {
    matcher: /^\/conversations\/[^/]+\/prospects\/new$/,
    title: 'Crear prospecto desde conversacion',
    description: 'Formulario contextual para vincular un prospecto al hilo actual.',
  },
  {
    matcher: /^\/conversations\/[^/]+\/appointments\/new$/,
    title: 'Crear cita desde conversacion',
    description: 'Formulario contextual para agendar una cita desde el hilo actual.',
  },
  {
    matcher: /^\/conversations\/[^/]+$/,
    title: 'Detalle de conversacion',
    description: 'Hilo, acciones contextuales y composer del contacto seleccionado.',
  },
  {
    path: '/conversations',
    title: 'Conversaciones',
    description: 'Lista paginada con filtros, estado y accesos contextuales.',
  },
  {
    path: '/templates/new',
    title: 'Crear plantilla',
    description: 'Formulario para guardar respuestas reutilizables.',
  },
  {
    path: '/templates',
    title: 'Plantillas de respuesta',
    description: 'Biblioteca de respuestas rapidas para conversaciones y reglas.',
  },
  {
    path: '/prospects/new',
    title: 'Crear prospecto',
    description: 'Alta manual de prospectos con datos comerciales base.',
  },
  {
    matcher: /^\/prospects\/[^/]+\/edit$/,
    title: 'Editar prospecto',
    description: 'Formulario reservado para actualizar datos y etapa del prospecto.',
  },
  {
    matcher: /^\/prospects\/[^/]+$/,
    title: 'Detalle de prospecto',
    description: 'Ficha del prospecto con historial, agenda y acciones relacionadas.',
  },
  {
    path: '/prospects',
    title: 'Prospectos',
    description: 'Embudo comercial con filtros, búsqueda y navegación a detalle.',
  },
  {
    path: '/agenda',
    title: 'Agenda digital completa',
    description:
      'Disponibilidad por sucursal, profesional, cabina, reserva temporal y enlace de confirmación.',
  },
  {
    path: '/appointments/new',
    title: 'Crear cita',
    description: 'Formulario para agendar una cita manual o contextual.',
  },
  {
    matcher: /^\/appointments\/[^/]+\/edit$/,
    title: 'Editar cita',
    description: 'Actualización de estado, horario, ubicación y notas de la cita.',
  },
  {
    matcher: /^\/appointments\/[^/]+\/reschedule$/,
    title: 'Reprogramar cita',
    description: 'Actualización puntual de fecha, duración y motivo del cambio.',
  },
  {
    matcher: /^\/appointments\/[^/]+$/,
    title: 'Detalle de cita',
    description: 'Vista de seguimiento para estado, horario y observaciones.',
  },
  {
    path: '/appointments',
    title: 'Agenda',
    description: 'Agenda mensual simple con lista diaria y filtros por estado o responsable.',
  },
  {
    matcher: /^\/prospects\/[^/]+\/appointments\/new$/,
    title: 'Crear cita desde prospecto',
    description: 'Formulario contextual para agendar una cita desde la ficha del prospecto.',
  },
  {
    path: '/catalog/products/new',
    title: 'Crear producto',
    description: 'Formulario para cargar productos o servicios del catálogo.',
  },
  {
    matcher: /^\/catalog\/products\/[^/]+\/edit$/,
    title: 'Editar producto',
    description: 'Formulario reservado para actualizar datos del catálogo.',
  },
  {
    path: '/catalog',
    title: 'Catálogo',
    description: 'Listado de productos y servicios con filtros y estado.',
  },
  {
    path: '/automation-rules/new',
    title: 'Crear regla',
    description: 'Formulario base para triggers simples y acciones controladas.',
  },
  {
    matcher: /^\/automation-rules\/[^/]+\/edit$/,
    title: 'Editar regla',
    description: 'Edición reservada para la configuración de automatizaciones.',
  },
  {
    matcher: /^\/automation-rules\/[^/]+\/test$/,
    title: 'Probar regla',
    description: 'Simulacion controlada sin envio real al canal experimental.',
  },
  {
    path: '/automation-rules',
    title: 'Reglas de automatización',
    description: 'Listado de reglas simples con filtros por trigger y estado.',
  },
  {
    path: '/business-ai',
    title: 'IA del Negocio',
    description: 'Panel de orquestación multiagente, conocimiento, auditoría y escalamiento.',
  },
  {
    path: '/reports',
    title: 'Reportes basicos',
    description:
      'Indicadores de conversaciones, prospectos, citas y rendimiento del centro estetico.',
  },
  {
    path: '/configuration',
    title: 'Configuración',
    description: 'Conexión, dispositivos, QR y preferencias de WhatsApp Web.',
  },
  {
    path: '/admin/company',
    title: 'Configuración de empresa',
    description: 'Datos corporativos base del negocio y su contexto operativo.',
  },
  {
    path: '/admin/locations',
    title: 'Sedes del negocio',
    description: 'Administración de sedes para agenda, conversaciones y operación multisede.',
  },
  {
    path: '/admin/multisite',
    title: 'Operación multisede',
    description:
      'Control operativo de catálogo, stock, profesionales, permisos y canales por sede.',
  },
  {
    path: '/admin/users/new',
    title: 'Crear usuario',
    description: 'Formulario para alta manual de usuarios y rol fijo.',
  },
  {
    matcher: /^\/admin\/users\/[^/]+\/edit$/,
    title: 'Editar usuario',
    description: 'Formulario reservado para actualizar usuarios existentes.',
  },
  {
    path: '/admin/users',
    title: 'Usuarios y roles',
    description: 'Listado de usuarios, filtros y resumen de roles disponibles.',
  },
  {
    path: '/admin/whatsapp-web',
    title: 'Conexión WhatsApp Web',
    description: 'Vista del estado del adaptador experimental desacoplado.',
  },
  {
    path: '/admin/security',
    title: 'Seguridad',
    description: 'Políticas de contraseña, sesión y bloqueo del negocio.',
  },
  {
    path: '/admin/whatsapp-simulator',
    title: 'Simulador WhatsApp',
    description: 'Simular mensajes entrantes de WhatsApp para pruebas sin Postman.',
  },
  {
    path: '/admin/professionals',
    title: 'Profesionales',
    description: 'Listado de profesionales del centro estetico con filtros y acciones.',
  },
  {
    matcher: /^\/admin\/professionals\/[^/]+\/edit$/,
    title: 'Editar profesional',
    description: 'Formulario para actualizar datos del profesional.',
  },
  {
    path: '/admin/professionals/new',
    title: 'Crear profesional',
    description: 'Formulario para alta de nuevo profesional.',
  },
  {
    path: '/admin/rooms',
    title: 'Cabinas y recursos',
    description: 'Listado de cabinas y recursos por sede.',
  },
  {
    matcher: /^\/admin\/rooms\/[^/]+\/edit$/,
    title: 'Editar cabina',
    description: 'Formulario para actualizar datos de la cabina.',
  },
  {
    path: '/admin/rooms/new',
    title: 'Crear cabina',
    description: 'Formulario para alta de nueva cabina o recurso.',
  },
  {
    path: '/admin/assignments',
    title: 'Asignaciones',
    description: 'Asignacion de profesionales y cabinas a servicios del catalogo.',
  },
  {
    path: '/admin',
    title: 'Administración',
    description: 'Resumen de empresa, usuarios, seguridad y estado del canal.',
  },
  {
    path: '/admin/branches',
    title: 'Sucursales',
    description: 'Administración de sucursales del negocio.',
  },
  {
    path: '/admin/services',
    title: 'Servicios',
    description: 'Servicios del catálogo comercial.',
  },
]

export function findNavigationItem(pathname: string) {
  const item = PRIMARY_NAV_ITEMS.find(
    (item) => pathname === item.path || pathname.startsWith(`${item.path}/`),
  )
  if (item) return item

  for (const parent of PRIMARY_NAV_ITEMS) {
    if (parent.children) {
      const child = parent.children.find(
        (child) => pathname === child.path || pathname.startsWith(`${child.path}/`),
      )
      if (child) return child
    }
  }

  return null
}

export function isAdminSubmenuPath(pathname: string) {
  return ADMIN_SUBMENU_ITEMS.some(
    (item) => pathname === item.path || pathname.startsWith(`${item.path}/`),
  )
}

export function findRouteMeta(pathname: string) {
  const directMatch = ROUTE_METADATA.find(
    (route) => route.path === pathname || route.matcher?.test(pathname),
  )

  if (directMatch) {
    return directMatch
  }

  const navigationItem = findNavigationItem(pathname)

  if (!navigationItem) {
    return null
  }

  return {
    title: navigationItem.label,
    description: navigationItem.description,
  }
}
