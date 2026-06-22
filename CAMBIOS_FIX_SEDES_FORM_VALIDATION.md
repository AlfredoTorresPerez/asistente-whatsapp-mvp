# Correccion formulario Sedes del negocio

## Objetivo

Corregir el formulario de `/admin/locations` para evitar solicitudes invalidas al crear o editar sedes del negocio.

## Cambios aplicados

- Se agrego validacion local antes de enviar el formulario.
- Se muestran errores por campo para `code`, `name`, `timezone`, direccion, ciudad, comuna, telefono y WhatsApp.
- Se evita ejecutar el POST/PUT cuando existen errores locales.
- Se autogenera el codigo desde el nombre cuando el campo codigo esta vacio.
- Se normaliza el codigo:
  - minusculas;
  - sin tildes;
  - espacios convertidos a guion;
  - caracteres no permitidos eliminados;
  - guiones duplicados reducidos.
- Se muestran errores devueltos por backend bajo los campos correspondientes.
- Se maneja el error 401 mostrando sesion expirada y redirigiendo a login.
- Se reemplazo el uso de `mutateAsync` sin captura por `mutate` para evitar promesas no capturadas en consola.

## Archivo modificado

- `frontend-react/src/modules/administration/pages/AdminLocationsPage.tsx`

## Validacion esperada

- Nombre `Sucursal Maipu` genera codigo `sucursal-maipu`.
- Nombre `Sucursal Maipú` genera codigo `sucursal-maipu`.
- Codigo vacio muestra `Código obligatorio.`.
- Nombre vacio muestra `Nombre obligatorio.`.
- Si backend devuelve errores por campo, se muestran bajo el campo correspondiente.
