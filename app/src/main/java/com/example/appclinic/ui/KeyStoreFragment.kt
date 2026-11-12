package com.example.appclinic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appclinic.crypto.AndroidKeyStoreManager
import com.example.appclinic.data.api.ApiClient
import com.example.appclinic.databinding.FragmentKeystoreBinding
import kotlinx.coroutines.launch

class KeyStoreFragment : Fragment() {

    private var _binding: FragmentKeystoreBinding? = null
    private val binding get() = _binding!!

    private var lastLocalCipher: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKeystoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etBaseUrl.setText(ApiClient.getBaseUrl())

        binding.btnCifrarLocal.setOnClickListener {
            cifrarLocal()
        }

        binding.btnDescifrarLocal.setOnClickListener {
            descifrarLocal()
        }

        binding.btnActualizarUrl.setOnClickListener {
            actualizarUrlYProbar()
        }

        // Probar estado conexión al iniciar
        probarConexion()
    }

    private fun cifrarLocal() {
        val textoPlano = binding.etTextoLocal.text.toString().trim()
        if (textoPlano.isEmpty()) {
            Toast.makeText(context, "Ingrese un texto para cifrar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val cipherBase64 = AndroidKeyStoreManager.encryptLocalData(textoPlano)
            lastLocalCipher = cipherBase64

            // Guardar también en EncryptedSharedPreferences (Jetpack Security)
            context?.let { ctx ->
                AndroidKeyStoreManager.saveEncryptedSetting(ctx, "last_encrypted_note", cipherBase64)
            }

            val resultadoText = """
                [CIFRADO EN HARDWARE ANDROID - AES-256-GCM]
                - Custodia de clave: Android KeyStore (TEE / Hardware)
                - Alias: "AppClinicLocalKey"
                
                CIFRADO BASE64 RESULTANTE:
                $cipherBase64
                
                (Se almacenó una copia en EncryptedSharedPreferences con MasterKey AES-256)
            """.trimIndent()

            binding.tvResultadoLocal.text = resultadoText
        } catch (e: Exception) {
            binding.tvResultadoLocal.text = "Error al cifrar localmente: ${e.localizedMessage}"
        }
    }

    private fun descifrarLocal() {
        val cipherBase64 = lastLocalCipher ?: context?.let { ctx ->
            AndroidKeyStoreManager.getEncryptedSetting(ctx, "last_encrypted_note", "")
        }

        if (cipherBase64.isNullOrEmpty()) {
            Toast.makeText(context, "Realice un cifrado primero", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val textoDescifrado = AndroidKeyStoreManager.decryptLocalData(cipherBase64)
            val resultadoText = """
                [DESCIFRADO DESDE KEYSTORE EXITOSO]
                
                TEXTO ORIGINAL RECUPERADO:
                "$textoDescifrado"
            """.trimIndent()

            binding.tvResultadoLocal.text = resultadoText
        } catch (e: Exception) {
            binding.tvResultadoLocal.text = "Error al descifrar desde KeyStore: ${e.localizedMessage}"
        }
    }

    private fun actualizarUrlYProbar() {
        val newUrl = binding.etBaseUrl.text.toString().trim()
        if (newUrl.isEmpty()) {
            Toast.makeText(context, "Ingrese una URL de API válida", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.updateBaseUrl(newUrl)
        probarConexion()
    }

    private fun probarConexion() {
        lifecycleScope.launch {
            try {
                binding.tvEstadoConexion.text = "Conectando con ${ApiClient.getBaseUrl()}..."
                val context = context ?: return@launch
                val response = ApiClient.getService().getLlavePublica()
                if (response.isSuccessful) {
                    val pem = response.body()?.string() ?: ""
                    binding.tvEstadoConexion.text = "[SERVIDOR ONLINE] (${ApiClient.getBaseUrl()})\nClave Pública Ed25519 activa:\n${pem.take(60)}..."
                    binding.tvEstadoConexion.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_green_dark))
                } else {
                    binding.tvEstadoConexion.text = "[ERROR DE SERVIDOR] Código HTTP: ${response.code()}"
                    binding.tvEstadoConexion.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
            } catch (e: Exception) {
                val context = context ?: return@launch
                binding.tvEstadoConexion.text = "[ERROR DE CONEXIÓN] ${e.localizedMessage}\nVerifique que el servidor Python esté en ejecución."
                binding.tvEstadoConexion.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
