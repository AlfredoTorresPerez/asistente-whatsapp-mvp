export type NavigationItem = {
  label: string
  path: string
  description: string
  allowedRoles?: string[]
}

export type RouteMeta = {
  title: string
  description: string
  path?: string
  matcher?: RegExp
}

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
    label: 'Agenda completa',
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
    label: 'Catalogo',
    path: '/catalog',
    description: 'Productos y servicios del catalogo comercial.',
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
  },
  {
    label: 'Reportes',
    path: '/reports',
    description: 'Indicadores basicos filtrados por rango y responsable.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Sedes del negocio',
    path: '/admin/locations',
    description: 'Administra sucursales, direccion, telefono, WhatsApp y operacion multisede.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Operacion multisede',
    path: '/admin/multisite',
    description: 'Disponibilidad, stock, profesionales, permisos y canales por sede.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Administracion',
    path: '/admin',
    description: 'Empresa, usuarios, seguridad y WhatsApp Web.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
  {
    label: 'Configuracion',
    path: '/configuration',
    description: 'Conexion, dispositivos, QR y preferencias de WhatsApp Web.',
    allowedRoles: ['OWNER', 'ADMIN', 'SUPERVISOR'],
  },
  {
    label: 'Simulador WhatsApp',
    path: '/admin/whatsapp-simulator',
    description: 'Simular mensajes entrantes de WhatsApp para pruebas.',
    allowedRoles: ['OWNER', 'ADMIN'],
  },
]

export function canAccessNavigationItem(item: NavigationItem, role?: string | null) {
  if (!item.allowedRoles) {
    return true
  }
  return Boolean(role && item.allowedRoles.includes(role))
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
    title: 'Cambiar contrasena',
    description: 'Pantalla reservada para la actualizacion de credenciales del usuario.',
  },
  {
    path: '/profile',
    title: 'Perfil de usuario',
    description: 'Datos personales, zona horaria y configuracion base del usuario.',
  },
  {
    path: '/conversations/new',
    title: 'Nueva conversacion',
    description: 'Alta manual para iniciar una conversacion desde el shell privado.',
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
    description: 'Embudo comercial con filtros, busqueda y navegacion a detalle.',
  },
  {
    path: '/agenda',
    title: 'Agenda digital completa',
    description:
      'Disponibilidad por sucursal, profesional, cabina, reserva temporal y enlace de confirmacion.',
  },
  {
    path: '/appointments/new',
    title: 'Crear cita',
    description: 'Formulario para agendar una cita manual o contextual.',
  },
  {
    matcher: /^\/appointments\/[^/]+\/edit$/,
    title: 'Editar cita',
    description: 'Actualizacion de estado, horario, ubicación y notas de la cita.',
  },
  {
    matcher: /^\/appointments\/[^/]+\/reschedule$/,
    title: 'Reprogramar cita',
    description: 'Actualizacion puntual de fecha, duracion y motivo del cambio.',
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
    description: 'Formulario para cargar productos o servicios del catalogo.',
  },
  {
    matcher: /^\/catalog\/products\/[^/]+\/edit$/,
    title: 'Editar producto',
    description: 'Formulario reservado para actualizar datos del catalogo.',
  },
  {
    path: '/catalog',
    title: 'Catalogo',
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
    description: 'Edicion reservada para la configuracion de automatizaciones.',
  },
  {
    matcher: /^\/automation-rules\/[^/]+\/test$/,
    title: 'Probar regla',
    description: 'Simulacion controlada sin envio real al canal experimental.',
  },
  {
    path: '/automation-rules',
    title: 'Reglas de automatizacion',
    description: 'Listado de reglas simples con filtros por trigger y estado.',
  },
  {
    path: '/business-ai',
    title: 'IA del Negocio',
    description: 'Panel de orquestacion multiagente, conocimiento, auditoria y escalamiento.',
  },
  {
    path: '/reports',
    title: 'Reportes basicos',
    description:
      'Indicadores de conversaciones, prospectos, citas y rendimiento del centro estetico.',
  },
  {
    path: '/configuration',
    title: 'Configuracion',
    description: 'Conexion, dispositivos, QR y preferencias de WhatsApp Web.',
  },
  {
    path: '/admin/company',
    title: 'Configuracion de empresa',
    description: 'Datos corporativos base del negocio y su contexto operativo.',
  },
  {
    path: '/admin/locations',
    title: 'Sedes del negocio',
    description: 'Administracion de sedes para agenda, conversaciones y operacion multisede.',
  },
  {
    path: '/admin/multisite',
    title: 'Operacion multisede',
    description:
      'Control operativo de catalogo, stock, profesionales, permisos y canales por sede.',
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
    title: 'Conexion WhatsApp Web',
    description: 'Vista del estado del adaptador experimental desacoplado.',
  },
  {
    path: '/admin/security',
    title: 'Seguridad',
    description: 'Politicas de contrasena, sesion y bloqueo del negocio.',
  },
  {
    path: '/admin/whatsapp-simulator',
    title: 'Simulador WhatsApp',
    description: 'Simular mensajes entrantes de WhatsApp para pruebas sin Postman.',
  },
  {
    path: '/admin',
    title: 'Administracion',
    description: 'Resumen de empresa, usuarios, seguridad y estado del canal.',
  },
]

export function findNavigationItem(pathname: string) {
  return (
    PRIMARY_NAV_ITEMS.find(
      (item) => pathname === item.path || pathname.startsWith(`${item.path}/`),
    ) ?? null
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
