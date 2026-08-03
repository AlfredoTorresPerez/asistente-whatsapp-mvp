# Solucion para errores DNS de Maven durante Docker build

## Sintoma

Durante la ejecucion local:

```bash
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

puede aparecer un error similar a:

```text
Could not transfer artifact ... from/to central (https://repo.maven.apache.org/maven2): repo.maven.apache.org: Temporary failure in name resolution
```

Esto no es un error de codigo Java. Es un problema de resolucion DNS dentro de la fase de construccion Docker.

## Cambios aplicados en el proyecto

1. `backend-java/Dockerfile` ahora usa cache persistente de Maven con BuildKit.
2. La compilacion Maven reintenta hasta 3 veces antes de fallar.
3. Se fuerza preferencia por IPv4 para reducir problemas de resolucion en redes corporativas o VPN.
4. `docker-compose.local.yml`, `docker-compose.prod.yml` y `docker-compose.full.yml` (antes `docker-compose.yml`) usan `build.network: host` para que la fase de construccion use la red del host Docker cuando sea posible.
5. Los servicios criticos agregan DNS explicitos `1.1.1.1` y `8.8.8.8` para ejecucion en contenedor.

## Comando recomendado

```bash
docker compose --env-file .env.local.example -f docker-compose.local.yml build --no-cache backend-java

docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

## Si el error persiste en Docker Desktop para Windows

Validar conectividad DNS desde un contenedor simple:

```bash
docker run --rm alpine nslookup repo.maven.apache.org
```

Si falla, configurar DNS global en Docker Desktop:

```json
{
  "dns": ["1.1.1.1", "8.8.8.8"]
}
```

Luego reiniciar Docker Desktop y ejecutar nuevamente el comando local.

## Limpieza recomendada si hay cache corrupta

```bash
docker compose -f docker-compose.local.yml down --remove-orphans

docker builder prune

docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

Usar `docker builder prune` con cuidado, porque elimina cache de construcciones Docker anteriores.
