# Seeds demo por ambiente

Este directorio queda reservado para scripts de datos demostrativos que no deben mezclarse con nuevas migraciones de esquema.

Reglas:

- No modificar migraciones Flyway ya aplicadas para mover datos historicos.
- Nuevos datos ficticios para demostracion deben vivir aqui o en un script equivalente por ambiente.
- No ejecutar seeds demo en produccion salvo que el ambiente haya sido creado explicitamente para demostracion controlada.
- Los datos de referencia indispensables para que el dominio funcione deben documentarse como referencia, no como demo.

Estado actual:

- Las migraciones historicas del MVP ya contienen datos semilla de demo/reference.
- La estrategia segura es conservar ese historial y separar cualquier nuevo dataset demostrativo desde este directorio.
