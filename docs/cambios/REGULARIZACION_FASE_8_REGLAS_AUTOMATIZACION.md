# Regularizacion Fase 8 - Reglas de automatizacion

Fecha: 2026-08-03

## Alcance aplicado

- Se incorporaron estados funcionales visibles: Borrador, En prueba, Publicada, Pausada y Archivada.
- Se preservo compatibilidad con el backend actual: `active` sigue representando publicacion operativa y el estado funcional se registra en la configuracion estructurada de la regla.
- La pantalla de reglas agrega busqueda por nombre, codigo, tipo o descripcion sobre los resultados visibles.
- Los indicadores fueron renombrados para evitar inconsistencias entre totales globales y conteos de la pagina actual.
- Se elimino la etiqueta tecnica `BD` en metricas de reglas.
- Las descripciones de reglas se muestran completas, sin truncamiento visual.
- Se agrego accion `Probar` desde cada regla y desde el formulario de edicion.
- El formulario agrega estado, condicion, accion y casos de prueba en una configuracion estructurada.
- Se bloquea la publicacion directa de una regla nueva sin caso de prueba registrado.
- Los mensajes visibles ya no hacen referencia al servidor ni a la base de datos.

## Archivos modificados

- `frontend-react/src/modules/rules/pages/AutomationRulesPage.tsx`
- `frontend-react/src/modules/rules/pages/AutomationRuleFormPage.tsx`
- `frontend-react/src/modules/rules/lib/ruleTypeLabels.ts`

## Validacion

- Backend: `mvn -q -DskipTests compile`
- Frontend: `pnpm build`
- Pruebas relacionadas: `pnpm test -- --run src/modules/rules/lib/ruleTypeLabels.test.ts`

## Riesgos residuales

- La persistencia nativa de estado, version, autor, casos de prueba y registro de ejecucion requiere evolucionar el modelo backend. En esta fase se regularizo la experiencia y se conservo el contrato existente sin migracion destructiva.
