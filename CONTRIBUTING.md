# Contributing

## Proceso

1. Crea un fork del repositorio.
2. Crea una rama desde `develop`: `feature/tu-cambio` o `fix/tu-arreglo`.
3. Realiza los cambios y asegura cobertura de pruebas.
4. Ejecuta `pnpm lint` y `pnpm format:check` (frontend) o `mvn verify` (backend).
5. Envía un Pull Request a `develop`.

## Estándares

- **Testing**: toda funcionalidad nueva debe incluir pruebas.
- **Backend**: pruebas unitarias con JUnit 5 + Mockito; integración con Testcontainers.
- **Frontend**: pruebas con Vitest + Testing Library + MSW.
- **Base de datos**: `business_id` en toda consulta SQL que acceda datos multi-empresa.
- **Mensajes de commit**: usar [Conventional Commits](https://www.conventionalcommits.org/).

## Perfiles Maven

| Perfil | Uso |
|--------|-----|
| `unit` | Pruebas rápidas sin base de datos |
| `integration` | Pruebas con Testcontainers |
| `local` | Ejecución local con H2 |

## Reporte de bugs

Usa GitHub Issues con la plantilla correspondiente. Incluye:
- Versión del proyecto
- Perfil activo (local, producción, etc.)
- Logs relevantes (ocultando datos sensibles)
- Pasos para reproducir
