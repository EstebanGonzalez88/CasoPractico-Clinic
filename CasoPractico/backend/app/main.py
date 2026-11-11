"""API FastAPI: expedientes cifrados (simétrico) + comprobantes firmados (asimétrico)."""
from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from app import config, database
from app.crypto_service import (
    decrypt_data,
    encrypt_data,
    get_public_key_pem,
    sign_payload,
    verify_signature,
)

database.init_db()

app = FastAPI(title="Clínica Segura - Caso Práctico Criptografía")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


class ExpedienteIn(BaseModel):
    paciente: str = Field(min_length=1, max_length=120)
    diagnostico: str = Field(min_length=1, max_length=2000)  # Campo sensible -> se cifra


class VerificacionIn(BaseModel):
    payload: dict
    firma_hex: str


def _aad(record_id: int) -> bytes:
    """Associated data: ata el cifrado al id del expediente."""
    return f"expediente:{record_id}".encode()


# ------------------------------------------------------------------
# MECANISMO 1: CIFRADO SIMÉTRICO
# ------------------------------------------------------------------
@app.post("/api/expedientes", summary="Guarda un expediente con el diagnóstico cifrado (AES-256-GCM)")
def crear_expediente(data: ExpedienteIn):
    record_id = database.insert_placeholder(data.paciente)
    cifrado = encrypt_data(data.diagnostico, _aad(record_id))
    database.update_cipher(record_id, cifrado)
    return {
        "mensaje": "Expediente guardado. El diagnóstico se almacenó cifrado.",
        "id": record_id,
        "almacenado_en_bd": cifrado,
    }


@app.get("/api/expedientes", summary="Lista expedientes tal como están en la BD (cifrados)")
def listar_expedientes():
    return [dict(r) for r in database.list_records()]


@app.get("/api/expedientes/{record_id}", summary="Lee y descifra un expediente")
def leer_expediente(record_id: int):
    row = database.get_record(record_id)
    if not row:
        raise HTTPException(404, "Expediente no encontrado")
    try:
        claro = decrypt_data(row["diagnostico_cifrado"], _aad(record_id))
    except ValueError as exc:
        raise HTTPException(409, str(exc))
    return {
        "id": row["id"],
        "paciente": row["paciente"],
        "diagnostico_descifrado": claro,
        "evidencia_cifrado_bd": row["diagnostico_cifrado"],
    }


# ------------------------------------------------------------------
# MECANISMO 2: FIRMA DIGITAL ASIMÉTRICA
# ------------------------------------------------------------------
@app.post("/api/comprobantes/{record_id}/firmar", summary="Emite una receta/comprobante firmado (Ed25519)")
def emitir_comprobante(record_id: int):
    row = database.get_record(record_id)
    if not row:
        raise HTTPException(404, "Expediente no encontrado")
    payload = {
        "expediente_id": row["id"],
        "paciente": row["paciente"],
        "diagnostico": decrypt_data(row["diagnostico_cifrado"], _aad(record_id)),
        "emisor": "Clínica Segura",
        "emitido_en": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    return {"comprobante": payload, "firma_digital": sign_payload(payload), "algoritmo": "Ed25519"}


@app.post("/api/comprobantes/verificar", summary="Verifica un comprobante con la llave pública")
def verificar_comprobante(req: VerificacionIn):
    ok = verify_signature(req.payload, req.firma_hex)
    return {
        "valido": ok,
        "mensaje": "Firma auténtica: el comprobante no fue alterado."
        if ok
        else "Firma inválida: el comprobante fue manipulado o no lo emitimos nosotros.",
    }


@app.get("/api/llave-publica", response_class=PlainTextResponse, summary="Llave pública Ed25519 (PEM)")
def llave_publica():
    return get_public_key_pem()


# Sirve el frontend en http://localhost:8000/
app.mount("/", StaticFiles(directory=config.FRONTEND_DIR, html=True), name="frontend")
