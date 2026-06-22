# Correccion CORS - ETAPA 3

Se corrigio el bloqueo CORS que impedia que el frontend en `http://localhost:5173` llamara al backend en `http://localhost:8080/api/v1/auth/login`.

## Cambios aplicados

1. Se movio `SecurityConfig.java` a la ruta correcta de paquete:

```text
backend-java/src/main/java/com/asistentewhatsapp/security/config/SecurityConfig.java
```

2. Se elimino el archivo anterior ubicado en:

```text
backend-java/src/main/java/com/asistentewhatsapp/security/SecurityConfig.java
```

3. Se habilito CORS en `SecurityFilterChain`:

```java
.cors(Customizer.withDefaults())
```

4. Se permitieron las solicitudes `OPTIONS` para preflight CORS:

```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```

5. Se agrego `CorsConfigurationSource` permitiendo:

```text
http://localhost:5173
http://127.0.0.1:5173
```

6. Se corrigio `JwtAuthenticationFilter` para no procesar:

- solicitudes `OPTIONS`;
- `/api/v1/auth/login`;
- `/api/v1/auth/logout`;
- `/api/v1/auth/forgot-password`;
- `/api/v1/auth/reset-password`;
- `/api/v1/health`;
- `/actuator/**`;
- `/v3/api-docs/**`;
- `/swagger-ui/**`.

7. Se agregaron beans de seguridad faltantes:

- `PasswordEncoder` con `BCryptPasswordEncoder`;
- `AuthenticationProvider` con `DaoAuthenticationProvider`;
- `AuthenticationManager`.

## Comandos recomendados

```powershell
docker compose down
docker compose build --no-cache backend-java
docker compose up
```

## Prueba CORS preflight

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method OPTIONS `
  -Headers @{
    "Origin" = "http://localhost:5173";
    "Access-Control-Request-Method" = "POST";
    "Access-Control-Request-Headers" = "content-type"
  }
```

Debe responder con encabezado:

```text
Access-Control-Allow-Origin: http://localhost:5173
```

## Prueba login backend

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"admin@demo.cl","password":"Cambiar123!"}'
```
