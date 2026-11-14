# Cómo levantar el backend y el túnel público (Windows PowerShell)

## 1. Backend (FastAPI)

```powershell
cd backend
python -m venv .venv                      # solo la primera vez
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt           # solo la primera vez
# Mientras no exista el main.py de Matías, usar la app temporal de Alvin:
uvicorn app.dev_main:app --host 0.0.0.0 --port 8000
# Cuando exista main.py:  uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- Documentación interactiva: http://localhost:8000/docs
- Pruebas: `python -m pytest -q`. Ninguna llama a NASA: todas usan la caché versionada.
- Datos NASA: ya están en `backend/data/cache/`. Para re-descargarlos: `python -m pipeline.power download --refresh`.

## 2. Túnel público (cloudflared, quick tunnel, sin cuenta)

```powershell
# Instalación sin admin (binario oficial firmado por Cloudflare):
$dir = "$env:LOCALAPPDATA\cloudflared"; New-Item -ItemType Directory -Force $dir | Out-Null
Invoke-WebRequest "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" -OutFile "$dir\cloudflared.exe"
# Levantar (con el backend corriendo en el puerto 8000):
& "$env:LOCALAPPDATA\cloudflared\cloudflared.exe" tunnel --no-autoupdate --url http://localhost:8000
```

Busca en la salida la línea `https://<palabras>.trycloudflare.com`: esa es la URL pública (HTTPS).

> ⚠️ **La URL cambia cada vez que se reinicia cloudflared.** Hay que avisar al canal y actualizar el firmware de Mateo
> y el `local.properties` de los teléfonos. Para la demo final conviene el despliegue estable (paso 10) o no reiniciar el túnel.
> La PC tiene que quedar encendida y sin suspenderse mientras se use el túnel.

## 3. Quién usa qué URL

| Cliente | URL |
|---|---|
| Navegador en la PC | `http://localhost:8000/` |
| Emulador Android | `http://10.0.2.2:8000/` (en `local.properties`: `farmealotu.apiBaseUrl=http://10.0.2.2:8000/`) |
| Teléfono Android físico | URL HTTPS del túnel (`farmealotu.apiBaseUrl=https://….trycloudflare.com/`) y recompilar |
| ESP32 en Wokwi / nodo físico (Mateo) | URL HTTPS del túnel. Wokwi gratis no llega a `localhost` |
| Gemelo digital, `replay.py` (Christian) | `http://localhost:8000/` si corre en esta PC; si no, la URL del túnel |
