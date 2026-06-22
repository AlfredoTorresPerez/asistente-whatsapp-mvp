# Configuracion de WhatsApp Web local

Se agrego la opcion lateral `Configuracion`, disponible en `/configuration`.

## Alcance funcional

La pantalla consume una API local del backend Java para mostrar y operar:

- estado de conexion de WhatsApp Web;
- telefono vinculado;
- codigo QR vigente;
- dispositivos vinculados;
- preferencias generales;
- canal principal;
- historial de sesiones.

## Endpoints agregados

Todos los endpoints requieren autenticacion como el resto del panel privado.

```text
GET   /api/v1/configuration/whatsapp
PATCH /api/v1/configuration/whatsapp/preferences
POST  /api/v1/configuration/whatsapp/connect
POST  /api/v1/configuration/whatsapp/refresh-qr
POST  /api/v1/configuration/whatsapp/disconnect
```

## Persistencia local

Se agrego la migracion `V10__whatsapp_configuration_preferences.sql`, que crea la tabla `whatsapp_configuration_preferences` para guardar preferencias por negocio.

## Prueba local

```bash
docker compose up --build
```

Luego abrir:

```text
http://localhost:5173/configuration
```

La pantalla usa el backend en:

```text
http://localhost:8080/api/v1/configuration/whatsapp
```

El backend delega las acciones de sesion al servicio local `whatsapp-web-service`.
