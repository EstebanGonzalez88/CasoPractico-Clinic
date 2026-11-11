"""Persistencia SQLite. La columna `diagnostico_cifrado` NUNCA contiene texto en claro."""
import sqlite3
from contextlib import contextmanager

from app.config import DB_PATH


@contextmanager
def _connect():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db() -> None:
    with _connect() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS expedientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                paciente TEXT NOT NULL,
                diagnostico_cifrado TEXT NOT NULL,
                creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        )


def insert_placeholder(paciente: str) -> int:
    """Inserta la fila para obtener el id (se usa como AAD del cifrado)."""
    with _connect() as conn:
        cur = conn.execute(
            "INSERT INTO expedientes (paciente, diagnostico_cifrado) VALUES (?, '')", (paciente,)
        )
        return cur.lastrowid


def update_cipher(record_id: int, diagnostico_cifrado: str) -> None:
    with _connect() as conn:
        conn.execute(
            "UPDATE expedientes SET diagnostico_cifrado = ? WHERE id = ?",
            (diagnostico_cifrado, record_id),
        )


def get_record(record_id: int):
    with _connect() as conn:
        return conn.execute(
            "SELECT id, paciente, diagnostico_cifrado, creado_en FROM expedientes WHERE id = ?",
            (record_id,),
        ).fetchone()


def list_records():
    with _connect() as conn:
        return conn.execute(
            "SELECT id, paciente, diagnostico_cifrado, creado_en FROM expedientes ORDER BY id DESC"
        ).fetchall()
