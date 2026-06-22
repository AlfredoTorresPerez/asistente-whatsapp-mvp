# Reparacion de DNS de Docker Desktop en Windows

## Diagnostico

Si el build falla con mensajes como estos, el problema no esta en el codigo fuente del proyecto:

- `Temporary failure resolving 'security.ubuntu.com'`
- `getaddrinfo EAI_AGAIN registry.npmjs.org`
- `Temporary failure in name resolution repo.maven.apache.org`
- `wget: Failed to fetch https://repo.maven.apache.org/...`

Esos errores indican que los contenedores de build de Docker Desktop no pueden resolver nombres DNS o no tienen salida HTTPS estable.

## Validacion rapida

Desde PowerShell, en la raiz del proyecto:

```powershell
.\scripts\diagnosticar-dns-docker.ps1
```

Si falla cualquiera de las pruebas, corrige Docker Desktop antes de volver a ejecutar `docker compose up --build`.

## Solucion recomendada en Docker Desktop

1. Abre Docker Desktop.
2. Ve a Settings > Docker Engine.
3. Agrega o fusiona esta configuracion en el JSON existente:

```json
{
  "dns": ["8.8.8.8", "1.1.1.1"],
  "features": {
    "buildkit": true
  }
}
```

No reemplaces todo el JSON si ya tiene otras claves. Fusiona las propiedades.

4. Presiona Apply & Restart.
5. Abre PowerShell como administrador y ejecuta:

```powershell
ipconfig /flushdns
wsl --shutdown
```

6. Cierra y abre Docker Desktop.
7. Ejecuta nuevamente:

```powershell
.\scripts\diagnosticar-dns-docker.ps1
```

## Comando normal de arranque local

Cuando el diagnostico sea correcto:

```powershell
.\scripts\levantar-local-seguro.ps1
```

O directamente:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

## Alternativa si BuildKit sigue fallando

Si el diagnostico funciona pero el build con BuildKit falla, prueba:

```powershell
.\scripts\build-local-legacy-network.ps1
```

Luego:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml up
```

## Nota tecnica

Los ajustes del proyecto reducen descargas innecesarias:

- El backend usa una imagen con Maven preinstalado.
- El backend runtime ya no instala curl con apt-get.
- El frontend fija pnpm 10.18.3.
- Los Dockerfile agregan reintentos y preferencia por IPv4.
- Los servicios tienen DNS explicito en runtime.

Aun asi, la primera construccion necesita internet para descargar dependencias Maven, paquetes npm y paquetes apt del servicio WhatsApp local.
