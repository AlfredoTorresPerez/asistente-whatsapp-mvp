# Evaluación semántica v2

## Resumen

| Métrica | Valor |
|---|---|
| Total evaluados | 10 |
| Óptimas (>=85) | 0 (0%) |
| Aceptables (65-84) | 4 (40%) |
| Deficientes (<65) | 6 (60%) |
| Error crítico | 0 (0%) |
| Excluidas | 0 |
| Puntaje promedio | 52/100 |

## Casos deficientes

- **P002** (36/100): "Quisiera hacer una consulta...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P003** (36/100): "¿Qué cosas puedo hacer por este WhatsApp?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P004** (34/100): "Necesito información del centro...." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P005** (36/100): "¿Me puedes orientar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P006** (36/100): "No sé por dónde empezar...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P010** (59/100): "Quiero que me llame alguien del centro...." → "Te derivaré con una persona del equipo para que pueda ayudar..."

## Desglose por intención

| Intención | Casos | Promedio | Mín | Máx |
|---|---:|---:|---:|---:|
| greeting | 1 | 75 | 75 | 75 |
| ambiguous | 5 | 41 | 36 | 59 |
| service_info | 1 | 34 | 34 | 34 |
| human | 3 | 70 | 70 | 70 |

## Respuestas duplicadas

| Respuesta | Ocurrencias |
|---|---:|
| "hola, gracias por escribirnos. ¿te ayudo con servicios, prec..." | 4 |
| "te derivare con una persona del equipo para que pueda ayudar..." | 4 |

