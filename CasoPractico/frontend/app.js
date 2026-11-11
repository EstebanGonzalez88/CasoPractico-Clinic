// Si el frontend lo sirve el backend, la API está en el mismo origen.
const API = window.location.protocol === "file:" ? "http://localhost:8000/api" : "/api";
const $ = (id) => document.getElementById(id);
const show = (id, obj) => ($(id).textContent = JSON.stringify(obj, null, 2));

async function api(path, options = {}) {
  const res = await fetch(API + path, { headers: { "Content-Type": "application/json" }, ...options });
  const ct = res.headers.get("content-type") || "";
  const body = ct.includes("application/json") ? await res.json() : await res.text();
  if (!res.ok) throw new Error(body.detail || JSON.stringify(body));
  return body;
}

// ---------- Mecanismo 1: cifrado simétrico ----------
$("form-expediente").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const r = await api("/expedientes", {
      method: "POST",
      body: JSON.stringify({ paciente: $("paciente").value, diagnostico: $("diagnostico").value }),
    });
    show("out-crear", r);
    e.target.reset();
    cargarTabla();
  } catch (err) { $("out-crear").textContent = "Error: " + err.message; }
});

async function cargarTabla() {
  const filas = await api("/expedientes");
  $("tabla").innerHTML = "";
  for (const f of filas) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${f.id}</td><td></td><td class="cipher"></td>
      <td><button class="small" data-act="leer">Descifrar</button>
          <button class="small secondary" data-act="firmar">Emitir receta</button></td>`;
    tr.children[1].textContent = f.paciente;            // textContent evita XSS
    tr.children[2].textContent = f.diagnostico_cifrado;
    tr.querySelector('[data-act="leer"]').onclick = () => leer(f.id);
    tr.querySelector('[data-act="firmar"]').onclick = () => firmar(f.id);
    $("tabla").appendChild(tr);
  }
}

async function leer(id) {
  try { show("out-leer", await api(`/expedientes/${id}`)); }
  catch (err) { $("out-leer").textContent = "Error: " + err.message; }
}

// ---------- Mecanismo 2: firma asimétrica ----------
async function firmar(id) {
  const r = await api(`/comprobantes/${id}/firmar`, { method: "POST" });
  $("payload").value = JSON.stringify(r.comprobante, null, 2);
  $("firma").value = r.firma_digital;
  $("out-verificar").textContent = "";
  $("payload").scrollIntoView({ behavior: "smooth" });
}

$("btn-verificar").addEventListener("click", async () => {
  const out = $("out-verificar");
  try {
    const r = await api("/comprobantes/verificar", {
      method: "POST",
      body: JSON.stringify({ payload: JSON.parse($("payload").value), firma_hex: $("firma").value.trim() }),
    });
    out.className = "result " + (r.valido ? "ok" : "bad");
    out.textContent = (r.valido ? "✅ " : "❌ ") + r.mensaje;
  } catch (err) {
    out.className = "result bad";
    out.textContent = "Error: " + err.message;
  }
});

$("btn-refrescar").onclick = cargarTabla;
api("/llave-publica").then((pem) => ($("pubkey").textContent = pem));
cargarTabla();
