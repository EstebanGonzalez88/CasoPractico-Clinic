"""Configuración: carga secretos desde variables de entorno / .env y define rutas."""
import base64
import os
from pathlib import Path

from dotenv import load_dotenv

BACKEND_DIR = Path(__file__).resolve().parent.parent   # .../CasoPractico/backend
PROJECT_DIR = BACKEND_DIR.parent                        # .../CasoPractico
FRONTEND_DIR = PROJECT_DIR / "frontend"

# Las variables reales del sistema tienen prioridad sobre el .env (override=False)
load_dotenv(BACKEND_DIR / ".env", override=False)


def _require(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(
            f"Falta la variable de entorno {name}. Ejecuta 'python setup_keys.py' dentro de backend/."
        )
    return value


# --- Mecanismo 1: clave simétrica AES-256 ---
AES_KEY: bytes = base64.b64decode(_require("APP_AES_KEY"))
if len(AES_KEY) != 32:
    raise RuntimeError("APP_AES_KEY debe ser de 32 bytes (AES-256) codificada en base64.")

# --- Mecanismo 2: llaves Ed25519 ---
SIGNING_KEY_PASSPHRASE: bytes = _require("APP_SIGNING_KEY_PASSPHRASE").encode()
SECRETS_DIR = Path(os.getenv("APP_SECRETS_DIR", BACKEND_DIR / "secrets"))
PRIVATE_KEY_PATH = SECRETS_DIR / "ed25519_private.pem"
PUBLIC_KEY_PATH = SECRETS_DIR / "ed25519_public.pem"

# --- Base de datos SQLite en la raíz del proyecto (CasoPractico/records.db) ---
DB_PATH = Path(os.getenv("APP_DB_PATH", PROJECT_DIR / "records.db"))
