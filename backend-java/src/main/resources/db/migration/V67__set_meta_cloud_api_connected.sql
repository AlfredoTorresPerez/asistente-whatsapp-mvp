update channel_account
set operational_status = 'CONNECTED',
    active = true,
    version = version + 1,
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and provider_name = 'META_CLOUD_API'
  and channel_type = 'WHATSAPP'
  and operational_status <> 'CONNECTED';
