-- ============================================================================
-- seed-local-whatsapp-data.sql
-- Datos semilla para validación local del flujo WhatsApp + IA
-- Ejecutar después de levantar la BD local: docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp -f /scripts/seed-local-whatsapp-data.sql
-- ============================================================================

-- Configuración de WhatsApp Channel para el business demo
INSERT INTO channel_account (id, business_id, channel_type, session_key, status, phone_number, last_event_at)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'WHATSAPP',
    'demo-sales',
    'CONNECTED',
    '56927305158',
    now()
)
ON CONFLICT (business_id, channel_type) DO UPDATE
SET status = 'CONNECTED', phone_number = '56927305158', last_event_at = now();

-- Clientes de prueba para simular mensajes entrantes
INSERT INTO customer (id, business_id, display_name, phone, normalized_phone, created_at)
VALUES
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'Cliente Test 1', '56950954580', '56950954580', now()),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'Cliente Test 2', '56987654321', '56987654321', now())
ON CONFLICT (business_id, normalized_phone) DO NOTHING;

-- Conversaciones activas para probar auto-reply
INSERT INTO conversation (id, business_id, channel_account_id, customer_id, assigned_user_id, location_id, unread_count, last_message_body, last_inbound_at, last_outbound_at)
SELECT
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    ca.id,
    c.id,
    u.id,
    bl.id,
    0,
    'Hola, quiero agendar una hora',
    now(),
    now()
FROM channel_account ca
CROSS JOIN customer c
CROSS JOIN LATERAL (
    SELECT id FROM "user" WHERE business_id = '11111111-1111-1111-1111-111111111111' LIMIT 1
) u
CROSS JOIN LATERAL (
    SELECT id FROM business_location WHERE business_id = '11111111-1111-1111-1111-111111111111' AND code = 'providencia' LIMIT 1
) bl
WHERE ca.business_id = '11111111-1111-1111-1111-111111111111'
  AND ca.channel_type = 'WHATSAPP'
  AND c.normalized_phone IN ('56950954580', '56987654321')
ON CONFLICT DO NOTHING;

-- Validación: verificar que las sedes tienen whatsapp_number configurado
DO $$
DECLARE
    loc RECORD;
BEGIN
    FOR loc IN SELECT id, name, whatsapp_number, active FROM business_location WHERE business_id = '11111111-1111-1111-1111-111111111111' AND active = true
    LOOP
        IF loc.whatsapp_number IS NULL OR loc.whatsapp_number = '' THEN
            RAISE NOTICE 'ADVERTENCIA: Sede "%" (id: %) no tiene whatsapp_number configurado', loc.name, loc.id;
        ELSE
            RAISE NOTICE 'OK: Sede "%" tiene whatsapp_number = %', loc.name, loc.whatsapp_number;
        END IF;
    END LOOP;
END $$;

-- Validación: verificar que los servicios tienen requiresRoom, preparationMinutes, cleanupMinutes
DO $$
DECLARE
    svc RECORD;
BEGIN
    FOR svc IN SELECT id, name, requires_room, preparation_minutes, cleanup_minutes FROM aesthetic_service WHERE business_id = '11111111-1111-1111-1111-111111111111'
    LOOP
        IF svc.requires_room IS NULL THEN
            RAISE NOTICE 'ADVERTENCIA: Servicio "%" (id: %) no tiene requires_room', svc.name, svc.id;
        ELSE
            RAISE NOTICE 'OK: Servicio "%" requires_room=% preparation=% cleanup=%', svc.name, svc.requires_room, svc.preparation_minutes, svc.cleanup_minutes;
        END IF;
    END LOOP;
END $$;

-- Validación: verificar que hay horarios de negocio configurados
DO $$
DECLARE
    bh_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO bh_count
    FROM agenda_business_hours
    WHERE business_id = '11111111-1111-1111-1111-111111111111';
    
    IF bh_count = 0 THEN
        RAISE NOTICE 'ADVERTENCIA: No hay agenda_business_hours configuradas para el business demo';
    ELSE
        RAISE NOTICE 'OK: % horarios de negocio configurados', bh_count;
    END IF;
END $$;

-- Validación: verificar que hay profesionales con horarios
DO $$
DECLARE
    ph_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO ph_count
    FROM agenda_professional_hours aph
    JOIN aesthetic_professional_location apl ON apl.professional_id = aph.professional_id AND apl.location_id = aph.location_id AND apl.business_id = aph.business_id
    WHERE aph.business_id = '11111111-1111-1111-1111-111111111111';
    
    IF ph_count = 0 THEN
        RAISE NOTICE 'ADVERTENCIA: No hay agenda_professional_hours configuradas para el business demo';
    ELSE
        RAISE NOTICE 'OK: % horarios de profesionales configurados', ph_count;
    END IF;
END $$;

-- Validación: verificar que hay cabinas (rooms) configuradas
DO $$
DECLARE
    room_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO room_count
    FROM agenda_room r
    JOIN business_location bl ON bl.id = r.location_id AND bl.business_id = r.business_id
    WHERE r.business_id = '11111111-1111-1111-1111-111111111111' AND bl.active = true;
    
    IF room_count = 0 THEN
        RAISE NOTICE 'ADVERTENCIA: No hay agenda_room configuradas para sedes activas del business demo';
    ELSE
        RAISE NOTICE 'OK: % cabinas configuradas para sedes activas', room_count;
    END IF;
END $$;

-- Validación: servicios asociados a cabinas
DO $$
DECLARE
    rs_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO rs_count
    FROM agenda_room_service rs
    JOIN agenda_room r ON r.id = rs.room_id AND r.business_id = rs.business_id
    JOIN business_location bl ON bl.id = r.location_id AND bl.business_id = r.business_id
    WHERE rs.business_id = '11111111-1111-1111-1111-111111111111' AND bl.active = true;
    
    IF rs_count = 0 THEN
        RAISE NOTICE 'ADVERTENCIA: No hay agenda_room_service configuradas para sedes activas';
    ELSE
        RAISE NOTICE 'OK: % asociaciones room-service para sedes activas', rs_count;
    END IF;
END $$;

-- Verificar canal WhatsApp
DO $$
DECLARE
    ca RECORD;
BEGIN
    SELECT * INTO ca FROM channel_account WHERE business_id = '11111111-1111-1111-1111-111111111111' AND channel_type = 'WHATSAPP';
    IF FOUND THEN
        RAISE NOTICE 'OK: Canal WhatsApp configurado - session_key: % status: % phone: %', ca.session_key, ca.status, ca.phone_number;
    ELSE
        RAISE NOTICE 'ADVERTENCIA: No hay canal WhatsApp configurado para el business demo';
    END IF;
END $$;

RAISE NOTICE '========================================';
RAISE NOTICE 'SEED LOCAL WHATSAPP DATA COMPLETADO';
RAISE NOTICE '========================================';