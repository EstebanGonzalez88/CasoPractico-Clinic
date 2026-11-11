# Clínica Segura — Caso práctico: criptografía en una aplicación propia

Una app web pequeña para expedientes clínicos con dos mecanismos criptográficos. Usa la librería [`cryptography`](https://cryptography.io).

| # | Mecanismo | Algoritmo | Caso de uso | Dónde está la llave |
|---|-----------|-----------|-------------|---------------------|
| 1 | Cifrado simétrico | **AES-256-GCM** (cifrado autenticado) | Cifrar el diagnóstico del paciente antes de guardarlo en SQLite | `backend/.env` → `APP_AES_KEY` (variable de entorno, fuera de Git) |
| 2 | Firma asimétrica | **Ed25519** | Firmar recetas/comprobantes y verificar que no se alteraron | `backend/secrets/ed25519_private.pem`, **cifrada con passphrase** (`APP_SIGNING_KEY_PASSPHRASE` en `.env`) y con permisos 600 |

## Estructura
```
CasoPractico/
├── records.db              # Base de datos SQLite (se crea al arrancar)
├── backend/
│   ├── .env                # Secretos (generado, en .gitignore)
│   ├── .env.example        # Plantilla sin valores
│   ├── secrets/            # Llaves Ed25519 (generadas, en .gitignore)
│   ├── setup_keys.py       # Genera las llaves
│   ├── requirements.txt
│   └── app/
│       ├── config.py         # Carga los secretos del entorno
│       ├── crypto_service.py # AES-GCM + Ed25519
│       ├── database.py       # SQLite
│       └── main.py           # API FastAPI (también sirve el frontend)
└── frontend/
    ├── index.html
    ├── app.js
    └── styles.css
```

## Cómo ejecutarla
```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python setup_keys.py              # genera .env y secrets/
uvicorn app.main:app --reload
```
Abre **http://localhost:8000** (frontend) o **http://localhost:8000/docs** (Swagger).

## Guion de la demostración
1. **Llaves protegidas:** muestra `backend/.env`, `backend/secrets/` y `.gitignore`. Muestra también que en el código no hay ninguna llave escrita. El PEM privado empieza con `BEGIN ENCRYPTED PRIVATE KEY`.
2. **Ciclo simétrico:**
   - Registra un expediente.
   - En la tabla se ve el `diagnostico_cifrado` tal como está en la BD (base64 ilegible).
   - Pruébalo directamente en el disco: `sqlite3 records.db "SELECT * FROM expedientes;"`
   - Pulsa **Descifrar**: la app devuelve el texto en claro.
3. **Ciclo asimétrico:**
   - Pulsa **Emitir receta**: el servidor firma el JSON con su llave privada.
   - Pulsa **Verificar firma** → ✅ válida.
   - Cambia una sola letra del JSON y verifica otra vez → ❌ inválida (integridad y autenticidad).

## Decisiones de seguridad
- **AES-GCM** con un nonce aleatorio de 96 bits en cada cifrado. Además, el id del expediente va como *associated data*: si alguien copia el texto cifrado de una fila a otra, el descifrado falla.
- **JSON canónico** (`sort_keys`, sin espacios) antes de firmar, así la misma información siempre da los mismos bytes.
- Las variables de entorno reales del sistema tienen prioridad sobre `.env`. En producción se pueden inyectar desde un gestor de secretos.
