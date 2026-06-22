# Instrucciones de ejecucion local

## Backend Java

1. Configurar PostgreSQL.
2. Configurar variables desde `.env.example`.
3. Ejecutar migraciones Flyway al iniciar Spring Boot.
4. Ejecutar:

```bash
cd backend-java
./mvnw spring-boot:run
```

Nota: en este entorno no fue posible descargar Maven desde internet, por lo que la compilacion completa debe validarse localmente con acceso a dependencias.

## Frontend React

1. Instalar dependencias.
2. Ejecutar:

```bash
cd frontend-react
npm install
npm run build
npm run dev
```

Nota: en este entorno no existe `node_modules`, por lo que la compilacion de interfaz debe validarse localmente.

## Flujo minimo de prueba funcional

1. Entrar al panel privado.
2. Abrir `/agenda`.
3. Seleccionar sucursal, servicio y fecha.
4. Consultar disponibilidad.
5. Ingresar cliente y telefono WhatsApp.
6. Elegir horario disponible.
7. Verificar que se cree reserva temporal.
8. Generar o revisar enlace de confirmacion.
9. Abrir `/reservas/confirmar/{token}`.
10. Confirmar reserva.
11. Verificar estado confirmado, historial y recordatorios.
