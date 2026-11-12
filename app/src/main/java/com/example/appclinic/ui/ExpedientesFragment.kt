package com.example.appclinic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appclinic.data.api.ApiClient
import com.example.appclinic.data.model.ExpedienteIn
import com.example.appclinic.databinding.FragmentExpedientesBinding
import kotlinx.coroutines.launch

class ExpedientesFragment : Fragment() {

    private var _binding: FragmentExpedientesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpedientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuardar.setOnClickListener {
            guardarExpediente()
        }

        binding.btnBuscarDescifrar.setOnClickListener {
            descifrarExpediente()
        }

        binding.btnListarTodo.setOnClickListener {
            listarExpedientes()
        }
    }

    private fun guardarExpediente() {
        val paciente = binding.etPaciente.text.toString().trim()
        val diagnostico = binding.etDiagnostico.text.toString().trim()

        if (paciente.isEmpty() || diagnostico.isEmpty()) {
            Toast.makeText(context, "Ingrese el nombre del paciente y el diagnóstico", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnGuardar.isEnabled = false
                binding.tvResultadoGuardado.text = "Procesando cifrado AES-256-GCM y guardando en servidor..."

                val response = ApiClient.getService().crearExpediente(ExpedienteIn(paciente, diagnostico))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val resultadoText = """
                        [REGISTRO GUARDADO Y CIFRADO]
                        - ID de Expediente: ${body.id}
                        - Estado: ${body.mensaje}
                        
                        EVIDENCIA DE CIFRADO EN BD (AES-256-GCM Base64):
                        ${body.almacenadoEnBd}
                    """.trimIndent()
                    binding.tvResultadoGuardado.text = resultadoText
                    binding.etRecordId.setText(body.id.toString())
                } else {
                    binding.tvResultadoGuardado.text = "Error del servidor: ${response.code()} ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                binding.tvResultadoGuardado.text = "Error de conexión: ${e.localizedMessage}\n(Verifique que el servidor de backend esté en ejecución)"
            } finally {
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    private fun descifrarExpediente() {
        val recordIdStr = binding.etRecordId.text.toString().trim()
        val recordId = recordIdStr.toIntOrNull()

        if (recordId == null) {
            Toast.makeText(context, "Ingrese un ID de expediente válido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnBuscarDescifrar.isEnabled = false
                binding.tvResultadoDescifrado.text = "Consultando y descifrando expediente #$recordId..."

                val response = ApiClient.getService().leerExpediente(recordId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val texto = """
                        [CONSULTA Y DESCIFRADO EXITOSO]
                        - ID: ${body.id}
                        - Paciente: ${body.paciente}
                        
                        DIAGNÓSTICO DESCIFRADO (Texto Plano):
                        "${body.diagnosticoDescifrado}"
                        
                        DATO ALMACENADO CIFRADO EN BD (Raw Ciphertext):
                        ${body.evidenciaCifradoBd}
                    """.trimIndent()
                    binding.tvResultadoDescifrado.text = texto
                } else {
                    binding.tvResultadoDescifrado.text = "Error ${response.code()}: No se encontró el expediente #$recordId"
                }
            } catch (e: Exception) {
                binding.tvResultadoDescifrado.text = "Error de conexión: ${e.localizedMessage}"
            } finally {
                binding.btnBuscarDescifrar.isEnabled = true
            }
        }
    }

    private fun listarExpedientes() {
        lifecycleScope.launch {
            try {
                binding.btnListarTodo.isEnabled = false
                val response = ApiClient.getService().listarExpedientes()
                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!
                    if (lista.isEmpty()) {
                        binding.tvResultadoDescifrado.text = "La base de datos no contiene expedientes registrados."
                        return@launch
                    }
                    val sb = StringBuilder("[REGISTROS CIFRADOS EN BASE DE DATOS]\n\n")
                    lista.forEach { item ->
                        sb.append("- ID ${item.id} | Paciente: ${item.paciente}\n")
                        sb.append("  Cifrado BD: ${item.diagnosticoCifrado}\n\n")
                    }
                    binding.tvResultadoDescifrado.text = sb.toString().trimEnd()
                } else {
                    binding.tvResultadoDescifrado.text = "Error al consultar: ${response.code()}"
                }
            } catch (e: Exception) {
                binding.tvResultadoDescifrado.text = "Error de conexión: ${e.localizedMessage}"
            } finally {
                binding.btnListarTodo.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
