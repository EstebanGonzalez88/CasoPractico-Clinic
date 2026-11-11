"""
Aprovisiona las llaves de la aplicación. Se ejecuta UNA vez antes de levantar el backend.

  1. Clave simétrica AES-256  -> backend/.env  (variable APP_AES_KEY)
  2. Par de llaves Ed25519     -> backend/secrets/
       - ed25519_private.pem  (cifrada con passphrase, permisos restringidos)
       - ed25519_public.pem   (pública, se puede compartir)
     La passphrase de la llave privada se guarda en backend/.env (APP_SIGNING_KEY_PASSPHRASE)

Uso:  python setup_keys.py [--force]
"""
import base64
import os
import secrets
import stat
import sys
from pathlib import Path

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ed25519
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

BASE_DIR = Path(__file__).resolve().parent
ENV_PATH = BASE_DIR / ".env"
SECRETS_DIR = BASE_DIR / "secrets"
PRIVATE_KEY_PATH = SECRETS_DIR / "ed25519_private.pem"
PUBLIC_KEY_PATH = SECRETS_DIR / "ed25519_public.pem"


def restrict_permissions(path: Path) -> None:
    """Solo el dueño puede leer/escribir (chmod 600). En Windows solo aplica 'solo lectura/escritura'."""
    os.chmod(path, stat.S_IRUSR | stat.S_IWUSR)


def main() -> None:
    force = "--force" in sys.argv
    if (ENV_PATH.exists() or PRIVATE_KEY_PATH.exists()) and not force:
        print("[!] Ya existen llaves. Usa --force para regenerarlas "
              "(los datos cifrados con la clave anterior ya no se podrán descifrar).")
        return

    # ---------- 1. Clave simétrica AES-256-GCM ----------
    aes_key = AESGCM.generate_key(bit_length=256)
    aes_key_b64 = base64.b64encode(aes_key).decode()

    # Passphrase aleatoria para proteger la llave privada en disco
    passphrase = secrets.token_urlsafe(32)

    ENV_PATH.write_text(
        f"APP_AES_KEY={aes_key_b64}\n"
        f"APP_SIGNING_KEY_PASSPHRASE={passphrase}\n",
        encoding="utf-8",
    )
    restrict_permissions(ENV_PATH)
    print(f"[+] Clave AES-256 y passphrase guardadas en {ENV_PATH}")

    # ---------- 2. Par de llaves Ed25519 ----------
    SECRETS_DIR.mkdir(exist_ok=True)
    private_key = ed25519.Ed25519PrivateKey.generate()

    PRIVATE_KEY_PATH.write_bytes(
        private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            # La llave privada queda cifrada en disco: sin la passphrase no sirve
            encryption_algorithm=serialization.BestAvailableEncryption(passphrase.encode()),
        )
    )
    restrict_permissions(PRIVATE_KEY_PATH)

    PUBLIC_KEY_PATH.write_bytes(
        private_key.public_key().public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo,
        )
    )
    print(f"[+] Par de llaves Ed25519 generado en {SECRETS_DIR}")
    print("[i] En Windows puedes restringir aún más con:")
    print(f'    icacls "{SECRETS_DIR}" /inheritance:r /grant:r "%USERNAME%:F"')


if __name__ == "__main__":
    main()
