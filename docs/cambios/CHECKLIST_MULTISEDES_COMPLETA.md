# Checklist de validacion multisede

## Base de datos

- [ ] Existe `business_location`.
- [ ] Existen sedes Providencia, Maipu y Santiago Centro.
- [ ] `lead.location_id` existe.
- [ ] `order_request.location_id` existe.
- [ ] `product_service_location` existe.
- [ ] `product_location_stock` existe.
- [ ] `professional_location_schedule` existe.
- [ ] `user_location_access` existe.
- [ ] `channel_account.location_id` existe.

## Backend

- [ ] `GET /api/v1/multisite/summary` responde.
- [ ] `GET /api/v1/multisite/catalog-availability` responde.
- [ ] `GET /api/v1/multisite/professionals` responde.
- [ ] `GET /api/v1/multisite/professional-schedules` responde.
- [ ] `GET /api/v1/multisite/user-access` responde.
- [ ] `GET /api/v1/multisite/channels` responde.

## Frontend

- [ ] Menu lateral muestra `Operacion multisede`.
- [ ] `/admin/multisite` abre pantalla operativa.
- [ ] Resumen muestra metricas por sede.
- [ ] Catalogo muestra disponibilidad y stock por sede.
- [ ] Profesionales muestra sedes y horarios.
- [ ] Permisos muestra usuario + sede.
- [ ] Canales muestra WhatsApp centralizado o por sede.

## Flujo manual sugerido

1. Iniciar sesion como admin.
2. Abrir `Sedes del negocio`.
3. Confirmar sedes Providencia, Maipu y Santiago Centro.
4. Abrir `Operacion multisede`.
5. Revisar resumen por sede.
6. Activar un producto o servicio en Maipu.
7. Crear horario de profesional en Providencia.
8. Revisar permisos por sede.
9. Revisar canales WhatsApp por sede.
