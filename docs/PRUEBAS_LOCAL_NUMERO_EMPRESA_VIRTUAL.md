# Pruebas locales con numero empresa virtual

## Objetivo

Separar el numero de empresa del numero cliente durante pruebas locales. El numero vinculado en WhatsApp Web debe ser distinto del numero que envia los mensajes de prueba.

## Configuracion recomendada

Crea o edita el archivo `.env` en la raiz del proyecto:

```env
WHATSAPP_WEB_COMPANY_PHONE_NUMBER=56XXXXXXXXX
WHATSAPP_WEB_TEST_CUSTOMER_PHONE_NUMBER=56950954580
APP_WHATSAPP_WEB_DEFAULT_PHONE_NUMBER=56XXXXXXXXX
```

- `WHATSAPP_WEB_COMPANY_PHONE_NUMBER`: numero que se vincula en WhatsApp Web mediante QR.
- `WHATSAPP_WEB_TEST_CUSTOMER_PHONE_NUMBER`: numero que actua como cliente y envia mensajes al numero empresa.
- `APP_WHATSAPP_WEB_DEFAULT_PHONE_NUMBER`: numero empresa visible para el backend.

## Flujo correcto

1. Levantar el entorno local con `docker compose up --build`.
2. Abrir `http://localhost:6080/vnc.html?autoconnect=true&resize=scale`.
3. Vincular WhatsApp Web usando el numero empresa virtual.
4. Desde el celular cliente `56950954580`, enviar un mensaje al numero empresa.
5. Verificar que el panel muestre la conversacion del cliente.

## Cambios incluidos

- El numero empresa ya no queda fijado a `56950954580` en el adaptador.
- El backend ya no reemplaza el telefono del canal empresa por el telefono del cliente cuando recibe mensajes.
- Los mensajes manuales enviados desde WhatsApp Web se registran como salientes externos y no activan IA.
- Los mensajes salientes enviados desde el panel no se duplican como mensajes manuales.
- El simulador de mensajes usa `WHATSAPP_WEB_TEST_CUSTOMER_PHONE_NUMBER` como cliente por defecto.

## Regla de validacion

No uses el mismo numero como empresa y cliente. Si se usa el mismo numero, WhatsApp Web marcara mensajes como propios y el flujo entrante puede ignorarlos para evitar bucles de respuesta automatica.
