# Prompt 01 — Contener secretos expuestos y asegurar el empaquetado

Actúa como responsable de seguridad y reproducibilidad del repositorio. Corrige la brecha por la cual una copia completa del workspace puede incluir archivos de entorno con credenciales, `.git`, dependencias, artefactos, resultados de pruebas y datos identificables.

## Contexto verificado

- Un ZIP externo del workspace incluyó `.env.local`, `.env.qa`, `.git`, `node_modules`, `target` y resultados de ejecución.
- Algunos archivos de entorno contenían credenciales literales y secretos de alta entropía.
- Existe un paquete distribuible interno generado por `scripts/local-package.ps1` y verificado por `scripts/verify-package.ps1`, pero el flujo no impide compartir por error un ZIP manual.
- El paquete oficial no garantiza que el árbol Git esté limpio ni registra de forma inequívoca si fue construido desde cambios no confirmados.
- Hay documentación y resultados con números de prueba completos.

## Objetivo

Establecer un único proceso de distribución seguro, verificable y fail-closed; eliminar secretos persistidos en archivos compartibles; y producir un procedimiento de rotación sin revelar ni modificar credenciales externas sin autorización.

## Trabajo requerido

1. Inspecciona `.gitignore`, archivos `.env*`, scripts de Credential Manager, scripts de empaquetado/verificación, workflows CI y documentación de entrega.
2. Clasifica los archivos de entorno en:
   - plantillas versionables sin valores reales;
   - archivos locales ignorados;
   - secretos que deben residir exclusivamente en Windows Credential Manager u otro almacén aprobado.
3. No imprimas valores. Si necesitas comprobarlos, informa únicamente nombre de variable, presencia, tipo de riesgo y acción requerida.
4. Asegura que ningún archivo distribuible contenga valores reales de tokens, contraseñas, claves, secretos de cifrado, identificadores sensibles o credenciales de proveedores.
5. Endurece `local-package.ps1/.sh` y `verify-package.ps1/.sh`:
   - rechazar árbol Git sucio o registrar explícitamente `dirty=true` y requerir una opción consciente para continuar;
   - usar lista blanca real de rutas, no solo confiar en extensiones o `.gitignore`;
   - rechazar `.git`, `.env`, `.env.local`, `.env.qa`, credenciales, logs, resultados, capturas, dependencias y directorios de build;
   - detectar archivos comprimidos anidados y auditarlos o rechazarlos;
   - ejecutar un escaneo de secretos sobre archivos actuales y, cuando sea viable, historial Git;
   - buscar números telefónicos completos en documentación, informes y logs;
   - generar manifiesto con revisión, estado limpio/sucio, herramientas, hashes y fecha;
   - no imprimir contenido sensible al fallar.
6. Agrega un gate de CI que ejecute escaneo de secretos y verificación del paquete. El pipeline debe fallar ante cualquier hallazgo crítico.
7. Sanitiza documentación y resultados versionados: reemplaza números completos por marcadores como `[TELEFONO_PRUEBA_AUTORIZADO]` o versiones enmascaradas. No alteres fixtures que requieran formato telefónico salvo que uses valores inequívocamente ficticios y documentados.
8. Crea una guía de respuesta al incidente que liste, solo por nombre de credencial, qué debe rotarse. No ejecutes revocaciones, rotaciones ni llamadas externas. Advierte que cambiar un secreto de cifrado puede requerir recifrar credenciales almacenadas.
9. Elimina o pone en cuarentena artefactos inseguros únicamente con confirmación explícita si la operación pudiera borrar datos del usuario. La corrección del código y las plantillas sí debe completarse.

## Pruebas obligatorias

- Generar un paquete desde un árbol limpio y ejecutar el verificador dos veces.
- Probar casos negativos controlados: `.env.local`, `.git`, token ficticio de alta entropía, número completo en documentación, ZIP anidado y árbol Git sucio.
- Confirmar que todos los casos negativos fallan sin revelar el valor detectado.
- Reconstruir frontend y backend desde el paquete verificado usando lockfiles.
- Ejecutar `docker compose config --quiet` con una plantilla segura.

## Criterios de aceptación

- Solo existe un procedimiento documentado y automatizado para crear el ZIP compartible.
- El paquete no contiene archivos de entorno reales, secretos, `.git`, dependencias instaladas, builds intermedios, resultados ni teléfonos completos en documentación/logs.
- CI impide publicar un paquete inseguro.
- El manifiesto demuestra integridad y procedencia desde un árbol limpio.
- Se entrega una lista de credenciales a rotar sin valores y sin realizar acciones externas no autorizadas.
- El proveedor simulado y el arranque local seguro continúan funcionando.

## Entrega esperada

Implementa los cambios, enumera los archivos modificados, presenta resultados de pruebas y deja un resumen de riesgos residuales. No incluyas secretos ni números reales en la respuesta.
