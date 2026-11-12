"""Script de sembrado (seeding) de base de datos con 4 expedientes cifrados reales."""
import sys
from app.database import init_db, insert_placeholder, update_cipher, list_records
from app.crypto_service import encrypt_data

def seed():
    init_db()
    existing = list_records()
    if len(existing) >= 4:
        print(f"[*] La base de datos ya contiene {len(existing)} registros. No es necesario sembrar más.")
        return

    pacientes_ejemplo = [
        ("Carlos Mendoza", "Hipertensión arterial sistémica grado I. Tratamiento con Losartán 50mg/día. Control en 30 días."),
        ("Ana Sofía Ramírez", "Diabetes Mellitus Tipo 2 compensada. Dieta baja en carbohidratos y Metformina 850mg c/12h."),
        ("Roberto Gómez", "Cuadro de lumbalgia aguda mecánica. Reposo relativo, Paracetamol 1g y terapia física."),
        ("Elena Rostova", "Faringoadenitis bacteriana aguda. Protocolo antibiótico con Amoxicilina 875mg cada 12 horas.")
    ]

    print("[+] Cifrando y sembrando expedientes de ejemplo...")
    for paciente, diagnostico in pacientes_ejemplo:
        record_id = insert_placeholder(paciente)
        aad = f"expediente:{record_id}".encode()
        diagnostico_cifrado = encrypt_data(diagnostico, aad)
        update_cipher(record_id, diagnostico_cifrado)
        print(f"  └─ ID {record_id}: Paciente '{paciente}' | Diagnóstico Cifrado AES-256-GCM: {diagnostico_cifrado[:30]}...")

    print("[✅] Sembrado completado con éxito.")

if __name__ == "__main__":
    seed()
