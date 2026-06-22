# Centro Estetico - Catalogo completo y respuestas IA

## Objetivo

Esta ampliacion agrega un catalogo mas completo de categorias, servicios, productos, reglas y promociones para que el asistente pueda construir respuestas segun la consulta real del cliente.

## Migracion agregada

Archivo:

```text
backend-java/src/main/resources/db/migration/V8__aesthetic_center_full_catalog_rules.sql
```

La migracion realiza operaciones `upsert` para que pueda ejecutarse en instalaciones nuevas o sobre la version anterior sin duplicar registros.

## Categorias de servicios cubiertas

- Tratamientos faciales.
- Tratamientos corporales.
- Depilacion.
- Manicure y pedicure.
- Pestanas y cejas.
- Peluqueria.
- Maquillaje.
- Medicina estetica no invasiva.

## Categorias de productos cubiertas

- Cremas faciales.
- Serums.
- Protectores solares.
- Exfoliantes.
- Mascarillas.
- Productos capilares.
- Aceites corporales.
- Esmaltes.
- Kits de cuidado facial.
- Productos post tratamiento.
- Gift cards.
- Packs promocionales.

## Productos agregados

Se agregaron productos complementarios por categoria, por ejemplo:

- Crema nutritiva noche.
- Crema piel sensible.
- Crema contorno de ojos.
- Serum vitamina C cosmetico.
- Serum niacinamida.
- Serum retinol cosmetico suave.
- Protector solar FPS 50 con color.
- Protector solar corporal FPS 50.
- Exfoliante corporal suave.
- Exfoliante enzimatico suave.
- Mascarilla arcilla purificante.
- Mascarilla hidratante facial.
- Mascarilla capilar nutritiva.
- Productos capilares post coloracion y post alisado.
- Aceites corporales.
- Productos para unas.
- Kits para piel grasa, piel seca, post peeling y post laser.
- Gift cards y packs promocionales.

## Reglas agregadas

Se agregaron reglas para controlar respuestas de la IA:

- Responder usando solo catalogo interno.
- Responder precio exacto desde `aesthetic_service.price_base`.
- Responder duracion exacta desde `aesthetic_service.duration_minutes`.
- Responder stock y precio desde `aesthetic_product`.
- Responder cuidados posteriores desde `aesthetic_service.aftercare_recommendations`.
- Responder contraindicaciones desde `aesthetic_service.contraindications`.
- Pedir aclaracion cuando falten datos.
- Derivar a humano por riesgo estetico.
- No confirmar agenda sin consultar disponibilidad real.
- Validar promociones por vigencia, stock y condiciones.
- Validar reglas especiales para laser, peeling, unas, pestanas, cejas y servicios capilares.

## Logica de respuesta mejorada

Archivo modificado:

```text
backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java
```

Ahora el metodo de analisis construye un contexto interno con:

- servicios activos;
- productos activos;
- reglas activas;
- promociones activas;
- servicio detectado;
- producto detectado.

Con esa informacion el asistente puede responder, por ejemplo:

### Consulta de precio

Cliente:

```text
Cuanto sale una limpieza facial?
```

Respuesta sugerida:

```text
El servicio Limpieza facial profunda tiene un valor base de $34.990 y una duracion estimada de 60 minutos. No se debe confirmar si existen contraindicaciones personales no evaluadas. Para reservar, debo revisar disponibilidad real de agenda antes de confirmar una hora.
```

### Consulta de duracion

Cliente:

```text
Cuanto dura el peeling?
```

Respuesta sugerida:

```text
La duracion estimada de Peeling estetico es de 50 minutos. Su valor base configurado es $39.990. Este servicio requiere evaluacion previa y consentimiento informado.
```

### Consulta de producto

Cliente:

```text
Tienen protector solar?
```

Respuesta sugerida:

```text
Protector solar FPS 50 tiene un valor de $15.990. Stock disponible: 32 unidad(es). Stock suficiente segun catalogo actual. Restricciones: Reaplicar segun exposicion; no reemplaza indicacion medica.
```

### Consulta de riesgo

Cliente:

```text
Estoy embarazada, puedo hacerme radiofrecuencia?
```

Respuesta sugerida:

```text
Gracias por contarnos. Para Radiofrecuencia facial existe informacion de seguridad que debe revisar una profesional antes de confirmar o recomendar el tratamiento. Contraindicaciones configuradas: Marcapasos, embarazo, implantes metalicos cercanos, enfermedad activa no evaluada.
```

## Consideraciones

- La IA no confirma reservas sin consulta de agenda.
- La IA no emite diagnosticos.
- La IA no promete resultados.
- La IA no recomienda productos o servicios si detecta condiciones sensibles.
- Las respuestas quedan auditadas en `aesthetic_intent_log`.
