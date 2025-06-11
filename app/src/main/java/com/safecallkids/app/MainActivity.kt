package com.safecallkids.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {
      private lateinit var statusText: TextView
    private lateinit var contactsCount: TextView
    private lateinit var blockedCount: TextView
    private lateinit var enableButton: Button
    private lateinit var btnLangPt: ImageButton
    private lateinit var btnLangEn: ImageButton
      private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ANSWER_PHONE_CALLS
    )
      private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Load saved language preference first
            loadLocale()
            
            setContentView(R.layout.activity_main)
            initViews()
            updateUI()
            Log.d("MainActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            logErrorToFile("onCreate", e)
            Toast.makeText(this, "Erro ao inicializar o app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
      private fun initViews() {
        try {
            statusText = findViewById(R.id.statusText)
            contactsCount = findViewById(R.id.contactsCount)
            blockedCount = findViewById(R.id.blockedCount)
            enableButton = findViewById(R.id.enableButton)
            btnLangPt = findViewById(R.id.btn_lang_pt)
            btnLangEn = findViewById(R.id.btn_lang_en)
            
            enableButton.setOnClickListener {
                try {
                    if (hasAllProtection()) {
                        openAppSettings()
                    } else {
                        requestAllProtection()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in button click", e)
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            
            enableButton.setOnLongClickListener {
                try {
                    runDiagnostic()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in diagnostic", e)
                    Toast.makeText(this, "Erro no diagnóstico: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            
            // Setup language switch buttons
            btnLangPt.setOnClickListener {
                setLocale("pt")
            }
            
            btnLangEn.setOnClickListener {
                setLocale("en")
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing views", e)
            logErrorToFile("initViews", e)
            throw e
        }
    }
      private fun hasAllPermissions(): Boolean {
        return try {
            REQUIRED_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
            false
        }
    }
    
    private fun hasOverlayPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking overlay permission", e)
            false
        }
    }    /**
     * Verifica se o app está configurado como CallScreeningService (Android 10+)
     */
    private fun isDefaultCallScreeningService(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para Android 10+, vamos verificar se temos permissões necessárias
                // e se o usuário já configurou o serviço
                val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
                val userConfigured = prefs.getBoolean("call_screening_configured", false)
                
                Log.d("MainActivity", "Android ${Build.VERSION.SDK_INT} detected")
                Log.d("MainActivity", "User configured call screening: $userConfigured")
                
                // Se o usuário já marcou como configurado, assumimos que está ok
                return userConfigured
            } else {
                true // Em versões antigas não é necessário
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking call screening service", e)
            false
        }
    }    /**
     * Solicita ao usuário para configurar o app como CallScreeningService
     * Agora usa detecção inteligente por versão do Android
     */
    private fun requestCallScreeningService() {
        try {
            Log.d("MainActivity", "Requesting call screening service setup")
            
            // Usar a nova função inteligente que detecta a versão
            requestAllPermissionsAtOnce()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting call screening service", e)
            Toast.makeText(this, "Erro ao solicitar configuração: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Abre as configurações para o usuário configurar CallScreeningService
     */
    private fun openCallScreeningSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Tentar abrir configurações específicas de telefone
                val intents = listOf(
                    Intent("android.telecom.action.CHANGE_DEFAULT_DIALER").apply {
                        putExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME", packageName)
                    },
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
                
                var opened = false
                for (intent in intents) {
                    try {
                        startActivity(intent)
                        opened = true
                        break
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Failed to open intent: ${intent.action}", e)
                    }
                }
                
                if (!opened) {
                    Toast.makeText(this, "Por favor, configure manualmente nas configurações do sistema", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening call screening settings", e)
            Toast.makeText(this, "Erro ao abrir configurações: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hasAllProtection(): Boolean {
        return try {
            hasAllPermissions() && hasOverlayPermission() && isDefaultCallScreeningService()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking protection status", e)
            false
        }
    }
    
    private fun requestPermissions() {
        try {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting permissions", e)
            Toast.makeText(this, "Erro ao solicitar permissões: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestOverlayPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                overlayPermissionLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting overlay permission", e)
            Toast.makeText(this, "Erro ao solicitar permissão de sobreposição: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startCallBlockingService() {
        try {
            val serviceIntent = Intent(this, CallBlockingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("MainActivity", "Call blocking service started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting service", e)
            Toast.makeText(this, "Erro ao iniciar serviço: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {        try {
            if (hasAllProtection()) {
                statusText.text = getString(R.string.protection_enabled_text)
                enableButton.text = getString(R.string.app_settings)
                
                try {
                    val contactsHelper = ContactsHelper(this)
                    val contactsNum = contactsHelper.getContactsCount()
                    contactsCount.text = getString(R.string.contacts_loaded_format, contactsNum)
                } catch (e: SecurityException) {
                    Log.w("MainActivity", "Permissão de contatos negada", e)
                    contactsCount.text = getString(R.string.contacts_loaded_no_permission)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao carregar contatos", e)
                    contactsCount.text = getString(R.string.contacts_loaded_error)
                }
                
                try {
                    val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
                    val blocked = prefs.getInt("blocked_calls_count", 0)
                    blockedCount.text = getString(R.string.calls_blocked_format, blocked)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao acessar preferências", e)
                    blockedCount.text = getString(R.string.calls_blocked_error)
                }
                
                if (hasAllPermissions()) {
                    startCallBlockingService()
                }
                
            } else {
                val missing = mutableListOf<String>()
                if (!hasAllPermissions()) missing.add(getString(R.string.basic_permissions))
                if (!hasOverlayPermission()) missing.add(getString(R.string.overlay_apps))
                if (!isDefaultCallScreeningService()) missing.add(getString(R.string.call_screening))
                
                statusText.text = getString(R.string.protection_disabled_text, missing.joinToString(", "))
                enableButton.text = getString(R.string.grant_permissions)
                contactsCount.text = getString(R.string.contacts_loaded_format, 0)
                blockedCount.text = getString(R.string.calls_blocked_format, 0)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao atualizar UI", e)
            logErrorToFile("updateUI", e)
            statusText.text = getString(R.string.interface_error)
            enableButton.text = getString(R.string.try_again)
            Toast.makeText(this, "Erro ao atualizar interface: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestAllProtection() {
        try {
            if (!hasAllPermissions()) {
                requestPermissions()
                return
            }
            
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return
            }
            
            if (!isDefaultCallScreeningService()) {
                requestCallScreeningService()
                return
            }
            
            updateUI()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao solicitar proteção", e)
            Toast.makeText(this, "Erro ao solicitar permissões: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao abrir configurações", e)
            Toast.makeText(this, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runDiagnostic() {
        Log.d("MainActivity", "=== DIAGNÓSTICO SAFECALLKIDS ===")
        try {
            REQUIRED_PERMISSIONS.forEach { permission ->
                val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                Log.d("MainActivity", "Permissão $permission: $granted")
            }
            
            val overlay = hasOverlayPermission()
            Log.d("MainActivity", "Overlay permission: $overlay")
            
            Log.d("MainActivity", "Android API Level: ${Build.VERSION.SDK_INT}")
            
            try {
                val contactsHelper = ContactsHelper(this)
                val contactsNum = contactsHelper.getContactsCount()
                Log.d("MainActivity", "Contatos carregados: $contactsNum")
                
                // Show sample contacts for debugging
                val samples = contactsHelper.getContactSample(3)
                Log.d("MainActivity", "Amostra de contatos: $samples")
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao carregar contatos", e)
            }
            
            val status = if (hasAllProtection()) "ATIVO" else "INATIVO"
            Log.d("MainActivity", "Status do bloqueador: $status")
            
            Toast.makeText(this, "Diagnóstico concluído - Status: $status\nVerifique os logs para detalhes", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro no diagnóstico", e)
            logErrorToFile("runDiagnostic", e)
            Toast.makeText(this, "Erro no diagnóstico: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun logErrorToFile(tag: String, error: Throwable) {
        try {
            val logMsg = "[${System.currentTimeMillis()}] $tag: ${error.message}\n${Log.getStackTraceString(error)}\n"
            openFileOutput("safecall_error.log", MODE_APPEND).use { fos ->
                fos.write(logMsg.toByteArray())
            }
        } catch (e: Exception) {
            // Se não conseguir logar, ignora para não causar novo crash
        }
    }

    /**
     * Marca o Call Screening como configurado pelo usuário
     */
    private fun markCallScreeningAsConfigured() {
        try {
            val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("call_screening_configured", true).apply()
            
            Log.d("MainActivity", "Call screening marked as configured by user")
            Toast.makeText(this, "✅ Call Screening marcado como configurado!", Toast.LENGTH_SHORT).show()
            
            updateUI()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error marking call screening as configured", e)
            Toast.makeText(this, "Erro ao salvar configuração", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Solicita TODAS as permissões necessárias de uma vez, baseado na versão do Android
     */
    private fun requestAllPermissionsAtOnce() {
        try {
            val androidVersion = Build.VERSION.SDK_INT
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL
            
            Log.d("MainActivity", "Android $androidVersion, Manufacturer: $manufacturer, Model: $model")
            
            val message = when {
                androidVersion >= Build.VERSION_CODES.TIRAMISU -> // Android 13+ (API 33+)
                    getAndroid13Instructions(manufacturer)
                
                androidVersion >= Build.VERSION_CODES.S -> // Android 12 (API 31-32)
                    getAndroid12Instructions(manufacturer)
                
                androidVersion >= Build.VERSION_CODES.Q -> // Android 10-11 (API 29-30)
                    getAndroid10Instructions()
                
                else -> // Android 9 e anteriores
                    getAndroidLegacyInstructions()
            }            // Criar layout personalizado
            val dialogView = layoutInflater.inflate(R.layout.dialog_configuration, null)
            val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
            val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
            val settingsButton = dialogView.findViewById<Button>(R.id.btn_settings)
            val alreadyConfiguredButton = dialogView.findViewById<Button>(R.id.btn_already_configured)
            
            messageTextView.text = message
            
            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_configuration_title, androidVersion))
                .setView(dialogView as android.view.View)
                .setCancelable(false)
                .create()
            
            cancelButton.setOnClickListener { dialog.dismiss() }
            settingsButton.setOnClickListener { 
                dialog.dismiss()
                openSpecificSettings(androidVersion)
            }
            alreadyConfiguredButton.setOnClickListener { 
                dialog.dismiss()
                markAllAsConfigured()
            }
            
            dialog.show()
                
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing complete setup", e)
            Toast.makeText(this, "Erro ao mostrar instruções: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
      /**
     * Instruções específicas para Android 13+
     */
    private fun getAndroid13Instructions(manufacturer: String): String {
        val baseInstructions = getString(R.string.android_13_instructions)
        
        return when (manufacturer) {
            "samsung" -> baseInstructions + "\n\n" + getString(R.string.samsung_note)
            "xiaomi" -> baseInstructions + "\n\n" + getString(R.string.xiaomi_note)
            "huawei" -> baseInstructions + "\n\n" + getString(R.string.huawei_note)
            "oppo", "oneplus" -> baseInstructions + "\n\n" + getString(R.string.oppo_note)
            else -> baseInstructions
        }
    }
      @Suppress("UNUSED_PARAMETER")
    /**
     * Instruções para Android 12
     */
    private fun getAndroid12Instructions(manufacturer: String): String {
        val baseInstructions = getString(R.string.android_12_instructions)
        
        return if (Build.MANUFACTURER.lowercase() == "samsung") {
            baseInstructions + "\n\n" + getString(R.string.samsung_note_android12)
        } else {
            baseInstructions
        }
    }
      /**
     * Instruções para Android 10-11
     */
    private fun getAndroid10Instructions(): String {
        return getString(R.string.android_10_instructions)
    }
    
    /**
     * Instruções para Android 9 e anteriores
     */
    private fun getAndroidLegacyInstructions(): String {
        return getString(R.string.android_legacy_instructions)
    }
    
    /**
     * Abre configurações específicas baseadas na versão e fabricante
     */
    private fun openSpecificSettings(androidVersion: Int) {
        try {
            val intents = mutableListOf<Intent>()
            
            // Primeiro tenta abrir configurações específicas por versão
            when {
                androidVersion >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ - Apps padrão
                    intents.add(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    intents.add(Intent("android.settings.ROLE_SETTINGS"))
                }
                
                androidVersion >= Build.VERSION_CODES.Q -> {
                    // Android 10+ - Call screening
                    intents.add(Intent("android.telecom.action.CHANGE_DEFAULT_DIALER"))
                    intents.add(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
            }
            
            // Sempre adiciona configurações gerais como fallback
            intents.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
            
            // Tenta abrir o primeiro intent que funcionar
            var opened = false
            for (intent in intents) {
                try {
                    startActivity(intent)
                    opened = true
                    Log.d("MainActivity", "Opened settings with intent: ${intent.action}")
                    break
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to open intent: ${intent.action}", e)
                }
            }
            
            if (!opened) {
                Toast.makeText(this, "Abra as Configurações manualmente e procure por 'Apps padrão'", Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening specific settings", e)
            Toast.makeText(this, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Marca tudo como configurado (para quando o usuário fez manualmente)
     */
    private fun markAllAsConfigured() {
        try {
            val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("call_screening_configured", true)
                .putBoolean("all_setup_completed", true)
                .apply()
            
            Log.d("MainActivity", "All configuration marked as completed by user")
            Toast.makeText(this, "✅ Configuração completa! Testando proteção...", Toast.LENGTH_LONG).show()
            
            updateUI()
            
            // Iniciar serviço para testar
            if (hasAllPermissions()) {
                startCallBlockingService()
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error marking all as configured", e)
            Toast.makeText(this, "Erro ao salvar configuração", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            try {
                Log.d("MainActivity", "Resultado das permissões recebido")
                
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allGranted) {
                    Log.d("MainActivity", "Todas as permissões concedidas")
                    requestAllProtection()
                } else {
                    Log.w("MainActivity", "Algumas permissões foram negadas")
                    Toast.makeText(this, "Algumas permissões são necessárias para o funcionamento do app", Toast.LENGTH_LONG).show()
                }
                
                updateUI()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing permission result", e)
                logErrorToFile("onRequestPermissionsResult", e)
                Toast.makeText(this, "Erro ao processar permissões: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            updateUI()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
            logErrorToFile("onResume", e)
            Toast.makeText(this, "Erro ao retomar app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
      override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "MainActivity destruída")
    }    /**
     * Set the locale for the app and recreate activity
     */
    private fun setLocale(languageCode: String) {
        try {
            Log.d("MainActivity", "Setting locale to: $languageCode")
            
            // Save preference
            val prefs = getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("language", languageCode).apply()
            
            // Apply locale immediately to current context
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createConfigurationContext(config)
            }
            
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            
            // Recreate activity to apply the new language completely
            recreate()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting locale", e)
            Toast.makeText(this, "Erro ao alterar idioma: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
      
    /**
     * Load saved locale preference
     */private fun loadLocale() {
        try {
            val prefs = getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
            val language = prefs.getString("language", "pt") ?: "pt" // Default to Portuguese
            
            Log.d("MainActivity", "Loading saved locale: $language")
            
            val locale = Locale(language)
            Locale.setDefault(locale)
            
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val context = createConfigurationContext(config)
                resources.updateConfiguration(config, context.resources.displayMetrics)
            } else {
                // For older versions, use the deprecated method
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading locale", e)
        }
    }
}
