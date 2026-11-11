package com.example.appclinic.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appclinic.data.api.ApiClient
import com.example.appclinic.data.model.VerificacionRequest
import com.example.appclinic.databinding.FragmentComprobantesBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class ComprobantesFragment : Fragment() {

    private var _binding: FragmentComprobantesBinding? = null
    private val binding get() = _binding!!

    private val gson = Gson()
    private var currentPayload: Map<String, Any?>? = null
    private var currentSignatureHex: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComprobantesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEmitirComprobante.setOnClickListener {
            emitirComprobante()
        }

        binding.btnVerificarOriginal.setOnClickListener {
            verificarOriginal()
        }

        binding.btnVerificarManipulado.setOnClickListener {
            verificarManipulado()
        }
    }

    private fun emitirComprobante() {
        val recordIdStr = binding.etComprobanteRecordId.text.toString().trim()
        val recordId = recordIdStr.toIntOrNull()

        if (recordId == null) {
            Toast.makeText(context, "Ingresa un ID válido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnEmitirComprobante.isEnabled = false
                binding.tvDetalleComprobante.text = "Emitiendo receta y firmando con Ed25519..."

                val response = ApiClient.getService().emitirComprobante(recordId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    currentPayload = body.comprobante
                    currentSignatureHex = body.firmaDigital

                    val jsonPretty = gson.toJson(body.comprobante)
                    binding.etJsonEditable.setText(jsonPretty)

                    val texto = """
                        ✍️ COMPROBANTE EMITIDO Y FIRMADO (Ed25519)
                        • Algoritmo: ${body.algoritmo}
                        
                        📜 FIRMA DIGITAL (Hexadecimal):
                        ${body.firmaDigital}
                        
                        📄 CONTENIDO DEL DOCUMENTO (JSON):
                        $jsonPretty
                    """.trimIndent()

                    binding.tvDetalleComprobante.text = texto
                    binding.tvResultadoVerificacion.text = "✅ Receta lista. Pulsa 'Verificar Firma Digital Original' o edita el JSON abajo para simular un ataque."
                    binding.tvResultadoVerificacion.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    binding.tvResultadoVerificacion.setTextColor(Color.parseColor("#1B5E20"))
                } else {
                    binding.tvDetalleComprobante.text = "Error ${response.code()}: No se pudo emitir comprobante para expediente #$recordId (guarda uno primero)."
                }
            } catch (e: Exception) {
                binding.tvDetalleComprobante.text = "Error de conexión: ${e.localizedMessage}"
            } finally {
                binding.btnEmitirComprobante.isEnabled = true
            }
        }
    }

    private fun verificarOriginal() {
        val payload = currentPayload
        val firmaHex = currentSignatureHex

        if (payload == null || firmaHex == null) {
            Toast.makeText(context, "Emite un comprobante primero", Toast.LENGTH_SHORT).show()
            return
        }

        verificarFirma(payload, firmaHex, "Firma Original Intacta")
    }

    private fun verificarManipulado() {
        val jsonText = binding.etJsonEditable.text.toString().trim()
        val firmaHex = currentSignatureHex

        if (jsonText.isEmpty() || firmaHex == null) {
            Toast.makeText(context, "Emite un comprobante primero", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val modifiedPayload: Map<String, Any?> = gson.fromJson(jsonText, mapType)
            verificarFirma(modifiedPayload, firmaHex, "Comprobante Editable (Prueba de Manipulación)")
        } catch (e: Exception) {
            Toast.makeText(context, "El JSON editado no es un JSON válido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarFirma(payload: Map<String, Any?>, firmaHex: String, tagContexto: String) {
        lifecycleScope.launch {
            try {
                binding.tvResultadoVerificacion.text = "Verificando firma con llave pública Ed25519..."

                val req = VerificacionRequest(payload, firmaHex)
                val response = ApiClient.getService().verificarComprobante(req)
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.valido) {
                        binding.tvResultadoVerificacion.text = """
                            ✅ FIRMA VÁLIDA — [$tagContexto]
                            ${res.mensaje}
                            • La firma Ed25519 coincide exactamente con los datos.
                            • Se garantiza AUTENTICIDAD E INTEGRIDAD INALTERADA.
                        """.trimIndent()
                        binding.tvResultadoVerificacion.setBackgroundColor(Color.parseColor("#E8F5E9"))
                        binding.tvResultadoVerificacion.setTextColor(Color.parseColor("#1B5E20"))
                    } else {
                        binding.tvResultadoVerificacion.text = """
                            ❌ FIRMA INVÁLIDA (ALERTA DE SEGURIDAD) — [$tagContexto]
                            ${res.mensaje}
                            • El documento FUE MODIFICADO o manipulado.
                            • La firma digital ya NO coincide con el contenido.
                        """.trimIndent()
                        binding.tvResultadoVerificacion.setBackgroundColor(Color.parseColor("#FFEBEE"))
                        binding.tvResultadoVerificacion.setTextColor(Color.parseColor("#B71C1C"))
                    }
                } else {
                    binding.tvResultadoVerificacion.text = "Error al verificar: ${response.code()}"
                }
            } catch (e: Exception) {
                binding.tvResultadoVerificacion.text = "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
