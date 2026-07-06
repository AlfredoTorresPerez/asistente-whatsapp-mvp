# Cambio: menu lateral Sedes del negocio

## Objetivo

Incorporar una funcion visible en el menu lateral llamada **Sedes del negocio**, enlazada a la pantalla de administracion de sucursales.

## Cambios realizados

- Se actualizo `frontend-react/src/lib/navigation.ts` para mostrar el item **Sedes del negocio**.
- La ruta asociada es `/admin/locations`.
- Se mantiene acceso restringido a roles `OWNER` y `ADMIN`.
- Se actualizo `frontend-react/src/components/navigation/Sidebar.tsx` para evitar que **Administracion** quede activa cuando se navega a `/admin/locations`.
- Se agrego un icono especifico para la opcion **Sedes del negocio**.

## Validacion manual esperada

1. Iniciar sesion con un usuario `OWNER` o `ADMIN`.
2. Verificar que el menu lateral muestre **Sedes del negocio**.
3. Hacer clic en **Sedes del negocio**.
4. Confirmar que navega a `/admin/locations`.
5. Confirmar que solo queda activa la opcion **Sedes del negocio** y no la opcion **Administracion**.
