"""
Servicio criptográfico. Usa exclusivamente la librería `cryptography` (no se implementa ningún algoritmo a mano).

  Mecanismo 1 - Cifrado simétrico AES-256-GCM (cifrado autenticado) para datos en reposo.
  Mecanismo 2 - Firma digital asimétrica Ed25519 para comprobantes/recetas.
"""
import base64
import json
import os

from cryptography.exceptions import InvalidSignature, InvalidTag
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ed25519
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from app import config

# =====================================================================
# MECANISMO 1: CIFRADO SIMÉTRICO (AES-256-GCM)
# =====================================================================
_aesgcm = AESGCM(config.AES_KEY)
_NONCE_SIZE = 12  # 96 bits, tamaño recomendado para GCM


def encrypt_data(plain_text: str, associated_data: bytes | None = None) -> str:
    """
    Cifra texto plano. Devuelve base64(nonce || ciphertext+tag).
    Se genera un nonce aleatorio nuevo en cada cifrado (nunca se reutiliza).
    `associated_data` (AAD) se autentica pero no se cifra: lo usamos para "atar"
    el dato cifrado a su registro y evitar que se copie a otra fila.
    """
    nonce = os.urandom(_NONCE_SIZE)
    ciphertext = _aesgcm.encrypt(nonce, plain_text.encode("utf-8"), associated_data)
    return base64.b64encode(nonce + ciphertext).decode("ascii")


def decrypt_data(token: str, associated_data: bytes | None = None) -> str:
    """Descifra lo producido por encrypt_data. Lanza ValueError si fue alterado."""
    raw = base64.b64decode(token)
    nonce, ciphertext = raw[:_NONCE_SIZE], raw[_NONCE_SIZE:]
    try:
        return _aesgcm.decrypt(nonce, ciphertext, associated_data).decode("utf-8")
    except InvalidTag as exc:
        raise ValueError("El dato cifrado fue manipulado o la clave es incorrecta") from exc


# =====================================================================
# MECANISMO 2: FIRMA DIGITAL ASIMÉTRICA (Ed25519)
# =====================================================================
def _load_private_key() -> ed25519.Ed25519PrivateKey:
    # La llave privada está cifrada en disco; se abre con la passphrase del entorno
    return serialization.load_pem_private_key(
        config.PRIVATE_KEY_PATH.read_bytes(), password=config.SIGNING_KEY_PASSPHRASE
    )


def _load_public_key() -> ed25519.Ed25519PublicKey:
    return serialization.load_pem_public_key(config.PUBLIC_KEY_PATH.read_bytes())


def get_public_key_pem() -> str:
    return config.PUBLIC_KEY_PATH.read_text(encoding="utf-8")


def _canonical(data: dict) -> bytes:
    """Serialización determinista: mismo dict -> mismos bytes -> misma firma."""
    return json.dumps(data, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def sign_payload(data: dict) -> str:
    """Firma el payload con la llave privada. Devuelve la firma en hexadecimal."""
    return _load_private_key().sign(_canonical(data)).hex()


def verify_signature(data: dict, signature_hex: str) -> bool:
    """Verifica la firma con la llave pública. True si es auténtica e íntegra."""
    try:
        _load_public_key().verify(bytes.fromhex(signature_hex), _canonical(data))
        return True
    except (InvalidSignature, ValueError):
        return False
