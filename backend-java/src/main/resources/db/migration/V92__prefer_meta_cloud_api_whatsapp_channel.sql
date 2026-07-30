-- V92: Preferir Meta Cloud API como canal WhatsApp conectado para el negocio demo.

update channel_account
set status = 'CONNECTED',
    operational_status = 'CONNECTED',
    connected_at = coalesce(connected_at, current_timestamp),
    disconnected_at = null,
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and channel_type = 'WHATSAPP'
  and provider_name = 'META_CLOUD_API';

update channel_account
set status = 'DISCONNECTED',
    operational_status = 'DISCONNECTED',
    disconnected_at = current_timestamp,
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and channel_type = 'WHATSAPP'
  and provider_name = 'WHATSAPP_WEB';
