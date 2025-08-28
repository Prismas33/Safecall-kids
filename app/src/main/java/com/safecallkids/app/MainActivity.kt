package com.safecallkids.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import android.app.role.RoleManager
import android.util.Log
import androidx.core.view.WindowCompat
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var contactsCount: TextView
    private lateinit var blockedCount: TextView
    private lateinit var setupProtectionButton: Button
    private lateinit var verifyButton: Button
    private lateinit var deactivateButton: Button
    private lateinit var instructionsButton: Button
    private lateinit var btnLangPt: ImageButton
    private lateinit var btnLangEn: ImageButton
    
    // Flag to track if guided setup dialog should be reopened
    private var shouldReopenGuidedSetup = false
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
            // Enable Edge-to-Edge for Android 15+ (API 35+) compatibility
            if (Build.VERSION.SDK_INT >= 35) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
            
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
    }    private fun initViews() {
        try {
            statusText = findViewById(R.id.statusText)
            contactsCount = findViewById(R.id.contactsCount)
            blockedCount = findViewById(R.id.blockedCount)
            setupProtectionButton = findViewById(R.id.setupProtectionButton)
            verifyButton = findViewById(R.id.verifyButton)
            deactivateButton = findViewById(R.id.deactivateButton)
            instructionsButton = findViewById(R.id.instructionsButton)
            btnLangPt = findViewById(R.id.btn_lang_pt)
            btnLangEn = findViewById(R.id.btn_lang_en)

            // Define explicit text and visibility for buttons
            verifyButton.text = getString(R.string.verify_and_activate)
            verifyButton.visibility = android.view.View.VISIBLE

            setupProtectionButton.setOnClickListener {
                try {
                    showGuidedSetupDialog()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error showing guided setup", e)
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            deactivateButton.setOnClickListener {
                try {
                    showDeactivationDialog()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error showing deactivation dialog", e)
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            instructionsButton.setOnClickListener {
                try {
                    showSimpleInstructionsDialog()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error showing instructions", e)
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Configure verify button
            verifyButton.setOnClickListener {
                try {
                    verifyAndActivateProtection()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in verify button click", e)
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            setupProtectionButton.setOnLongClickListener {
                try {
                    runDiagnostic()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in diagnostic", e)
                    Toast.makeText(this, "Erro no diagnóstico: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }

            // Setup language switch buttons
            btnLangPt.setOnClickListener { setLocale("pt") }
            btnLangEn.setOnClickListener { setLocale("en") }
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
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }
            Log.d("MainActivity", "hasOverlayPermission() returning: $result (API level: ${Build.VERSION.SDK_INT})")
            result
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
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    return true
                }
            }
            val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val isDefaultDialer = telecom.defaultDialerPackage == packageName
            Log.d("MainActivity", "Is default dialer: $isDefaultDialer")
            isDefaultDialer
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking call screening/default dialer", e)
            false
        }
    }

    private fun requestCallScreeningRoleOrDialer(): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)

                // 1) Tentar definir como app de telefone padrão (ROLE_DIALER)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    val dialerRoleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    try { startActivity(dialerRoleIntent); return true } catch (_: Exception) { Log.w("MainActivity","ROLE_DIALER intent falhou") }
                }

                // 2) Tentar papel de triagem de chamadas (ROLE_CALL_SCREENING)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val screeningRoleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    try { startActivity(screeningRoleIntent); return true } catch (_: Exception) { Log.w("MainActivity","ROLE_CALL_SCREENING intent falhou") }
                }
            }
            // Try to open change default dialer directly
            try {
                val dialerIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
                startActivity(dialerIntent)
                return true
            } catch (_: Exception) { Log.w("MainActivity","ACTION_CHANGE_DEFAULT_DIALER falhou") }

            // System settings (generic)
            val fallbacks = listOf(
                Intent("android.settings.ROLE_SETTINGS"),
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
                Intent(Settings.ACTION_SETTINGS)
            )
            for (i in fallbacks) {
                try { startActivity(i); return true } catch (_: Exception) { Log.w("MainActivity","Falha ao abrir ${i.action}") }
            }

            // Explicit components for some OEMs/Android versions
            val explicitAttempts = listOf(
                // AOSP default apps
                Intent().setClassName("com.android.settings", "com.android.settings.Settings\$ManageDefaultAppsActivity"),
                Intent().setClassName("com.android.settings", "com.android.settings.Settings\$RoleSettingsActivity"),
                Intent().setClassName("com.android.settings", "com.android.settings.Settings")
            )
            for (i in explicitAttempts) {
                try { startActivity(i); return true } catch (_: Exception) { /* ignore */ }
            }

            // App details as last resort
            try {
                val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(appDetails)
                return true
            } catch (_: Exception) { Log.w("MainActivity","Falha ao abrir detalhes do app") }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao solicitar papel de triagem/dialer", e)
        }
        return false
    }
    /**
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
    
    /**
     * Verifica se a proteção está realmente ativa (permissões + flag manual do utilizador)
     */
    private fun isProtectionReallyActive(): Boolean {
        return try {
            val hasSystemRequirements = hasAllProtection()
            val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
            val userEnabled = prefs.getBoolean("all_setup_completed", false)
            
            Log.d("MainActivity", "System requirements: $hasSystemRequirements, User enabled: $userEnabled")
            
            hasSystemRequirements && userEnabled
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking real protection status", e)
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
    
    private fun updateUI() {        
        try {            
            val hasSystemRequirements = hasAllProtection() // permissões + call screening ativo
            val isReallyActive = isProtectionReallyActive() // + flag manual
            
            Log.d("MainActivity", "UI Update - System: $hasSystemRequirements, Really active: $isReallyActive")
            
            if (isReallyActive) {
                // REALMENTE ATIVO: permissões + flag manual
                statusText.text = getString(R.string.protection_enabled_text)
                setupProtectionButton.text = getString(R.string.app_settings)
                verifyButton.text = "🔓 Desativar Proteção"
                verifyButton.visibility = android.view.View.VISIBLE
                deactivateButton.visibility = android.view.View.GONE
                
            } else if (hasSystemRequirements) {
                // SISTEMA PRONTO mas flag manual desativa
                statusText.text = "⚠️ Proteção Disponível\nPrime 'Ativar' para começar a bloquear"
                setupProtectionButton.visibility = android.view.View.VISIBLE
                setupProtectionButton.text = getString(R.string.setup_protection)
                verifyButton.text = "🔒 Ativar Proteção"
                verifyButton.visibility = android.view.View.VISIBLE
                deactivateButton.visibility = android.view.View.GONE
                
            } else {
                // FALTA CONFIGURAR SISTEMA
                val missing = mutableListOf<String>()
                if (!hasAllPermissions()) missing.add(getString(R.string.basic_permissions))
                if (!hasOverlayPermission()) missing.add(getString(R.string.overlay_apps))
                if (!isDefaultCallScreeningService()) missing.add(getString(R.string.call_screening))
                  
                statusText.text = getString(R.string.protection_disabled_text, missing.joinToString(", "))
                setupProtectionButton.visibility = android.view.View.VISIBLE
                setupProtectionButton.text = getString(R.string.setup_protection)
                verifyButton.text = getString(R.string.verify_and_activate)
                verifyButton.visibility = android.view.View.VISIBLE
                deactivateButton.visibility = android.view.View.GONE
            }
            
            // Estatísticas (sempre mostrar)
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
            
            // Iniciar serviço se estiver ativo
            if (isReallyActive && hasAllPermissions()) {
                startCallBlockingService()
            }
                
        } catch (e: Exception) {            Log.e("MainActivity", "Erro ao atualizar UI", e)
            logErrorToFile("updateUI", e)
            statusText.text = getString(R.string.interface_error)
            setupProtectionButton.text = getString(R.string.try_again)
            verifyButton.visibility = android.view.View.VISIBLE
            deactivateButton.visibility = android.view.View.GONE
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
            // Instead of blindly marking, re-check real status and inform the user
            val realConfigured = isDefaultCallScreeningService()
            if (realConfigured) {
                Log.d("MainActivity", "Call screening is actually configured")
                Toast.makeText(this, "✅ Call Screening realmente configurado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Ainda não é o app de telefone/triagem padrão", Toast.LENGTH_LONG).show()
                requestCallScreeningRoleOrDialer()
            }
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
                markCallScreeningAsConfigured()
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
                    Toast.makeText(this, "✅ Permissões básicas concedidas!", Toast.LENGTH_SHORT).show()
                    
                    // Se vem do guided setup, reabrir o diálogo
                    if (shouldReopenGuidedSetup) {
                        shouldReopenGuidedSetup = false
                        Handler(Looper.getMainLooper()).postDelayed({
                            showGuidedSetupDialog()
                        }, 500)
                    } else {
                        // Se não vem do guided setup, continuar com o fluxo normal
                        requestAllProtection()
                    }
                } else {
                    Log.w("MainActivity", "Algumas permissões foram negadas")
                    Toast.makeText(this, "Algumas permissões são necessárias para o funcionamento do app", Toast.LENGTH_LONG).show()
                    
                    // Se vem do guided setup, reabrir o diálogo mesmo com permissões negadas
                    if (shouldReopenGuidedSetup) {
                        shouldReopenGuidedSetup = false
                        Handler(Looper.getMainLooper()).postDelayed({
                            showGuidedSetupDialog()
                        }, 500)
                    }
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
            
            // Reopen guided setup dialog if returning from settings
            if (shouldReopenGuidedSetup) {
                shouldReopenGuidedSetup = false
                // Small delay to ensure UI is ready
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showGuidedSetupDialog()
                }, 500)
            }
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

    /**
     * Ativa/Desativa a proteção baseado no estado atual
     */
    private fun verifyAndActivateProtection() {
        try {
            Log.d("MainActivity", "=== TOGGLE PROTECTION ===")
            
            val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
            val currentlyEnabled = prefs.getBoolean("all_setup_completed", false)
            val hasSystemRequirements = hasAllProtection()
            
            Log.d("MainActivity", "Currently enabled: $currentlyEnabled")
            Log.d("MainActivity", "Has system requirements: $hasSystemRequirements")
            
            if (currentlyEnabled) {
                // Flag está ativa → SEMPRE pode desativar (mesmo sem configurações do sistema)
                Log.i("MainActivity", "🔓 Desativando proteção...")
                prefs.edit()
                    .putBoolean("all_setup_completed", false)
                    .putBoolean("call_screening_configured", false)
                    .apply()
                
                Toast.makeText(this, "🔓 Proteção DESATIVADA", Toast.LENGTH_SHORT).show()
                updateUI()
                return
            }
            
            if (!hasSystemRequirements) {
                // Sistema não está configurado e quer ativar - mostrar erro
                val missing = mutableListOf<String>()
                if (!hasAllPermissions()) missing.add(getString(R.string.basic_permissions))
                if (!hasOverlayPermission()) missing.add(getString(R.string.overlay_apps))
                if (!isDefaultCallScreeningService()) missing.add(getString(R.string.call_screening))
                
                val missingText = missing.joinToString(", ")
                Log.w("MainActivity", "❌ Sistema não configurado: $missingText")
                Toast.makeText(
                    this, 
                    "❌ Falta configurar: $missingText", 
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            
            // Se chegou aqui: flag inativa + sistema configurado → ATIVAR
            Log.i("MainActivity", "🔒 Ativando proteção...")
            prefs.edit()
                .putBoolean("call_screening_configured", true)
                .putBoolean("all_setup_completed", true)
                .apply()
            
            Toast.makeText(this, "🔒 Proteção ATIVADA! A bloquear chamadas desconhecidas", Toast.LENGTH_LONG).show()
            
            // Iniciar serviço
            if (hasAllPermissions()) {
                startCallBlockingService()
            }
            
            updateUI()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao toggle proteção", e)
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
                
    /**
     * Shows the guided setup dialog with ordered configuration steps
     */
    private fun showGuidedSetupDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_guided_setup, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Get buttons  
            val btnStep1: Button = dialogView.findViewById(R.id.btn_step1_basic_permissions)
            val btnStep2: Button = dialogView.findViewById(R.id.btn_step2_phone_app)
            val btnStep3: Button = dialogView.findViewById(R.id.btn_step3_return)

            // Update button states based on current permissions
            updateDialogButtonStates(btnStep1, btnStep2, btnStep3)

            // Step 1: Basic Permissions
            btnStep1.setOnClickListener {
                if (hasAllPermissions()) {
                    Toast.makeText(this, "✅ Permissões básicas já concedidas!", Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    shouldReopenGuidedSetup = true
                    requestPermissions()
                }
            }

            // Step 2: Phone App  
            btnStep2.setOnClickListener {
                Log.d("MainActivity", "Step 2 button clicked")
                try {
                    dialog.dismiss()
                    shouldReopenGuidedSetup = true
                    
                    // Show immediate feedback
                    Toast.makeText(this, "Abrindo configurações do telefone...", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Toast shown, checking if already configured...")
                    
                    if (isDefaultCallScreeningService()) {
                        Log.d("MainActivity", "Already configured as default phone app")
                        Toast.makeText(this, "✅ App de telefone já configurado!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    Log.d("MainActivity", "Not configured, attempting to open settings...")
                    // Use the EXACT implementation that worked before
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Log.d("MainActivity", "Android 10+ detected, trying intents...")
                        val intents = listOf(
                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                        
                        var opened = false
                        for ((index, intent) in intents.withIndex()) {
                            try {
                                Log.d("MainActivity", "Trying intent $index: ${intent.action}")
                                startActivity(intent)
                                Log.d("MainActivity", "Intent $index succeeded!")
                                opened = true
                                break
                            } catch (e: Exception) {
                                Log.w("MainActivity", "Intent $index failed: ${intent.action}", e)
                            }
                        }
                        
                        if (!opened) {
                            Log.e("MainActivity", "All intents failed!")
                            Toast.makeText(this, "❌ Não foi possível abrir configurações automaticamente", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.d("MainActivity", "Android < 10, trying app details...")
                        // Para Android < 10
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                            Log.d("MainActivity", "App details opened successfully")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to open app details", e)
                            Toast.makeText(this, "❌ Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in Step 2 click listener", e)
                    Toast.makeText(this, "❌ Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
                }
            }

            // Step 3: Return to App
            btnStep3.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing guided setup dialog", e)
            Toast.makeText(this, "Erro ao mostrar configuração: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Updates dialog button colors and text based on current permission states
     */
    private fun updateDialogButtonStates(btnStep1: Button, btnStep2: Button, btnStep3: Button) {
        try {
            // Step 1: Basic Permissions
            if (hasAllPermissions()) {
                btnStep1.setBackgroundColor(0xFF4CAF50.toInt()) // Green - completed
                btnStep1.text = "✅ ${getString(R.string.step1_basic_permissions)}"
                btnStep1.isEnabled = false // Disable if already granted
            } else {
                btnStep1.setBackgroundColor(0xFF2196F3.toInt()) // Blue - action needed
                btnStep1.text = getString(R.string.step1_basic_permissions)
                btnStep1.isEnabled = true
            }

            // Step 2: Phone App (automatically grants overlay permission)
            if (isDefaultCallScreeningService()) {
                btnStep2.setBackgroundColor(0xFF4CAF50.toInt()) // Green - completed
                btnStep2.text = "✅ ${getString(R.string.step2_phone_app)}"
                btnStep2.isEnabled = false // Disable if already granted
            } else {
                btnStep2.setBackgroundColor(0xFFFF9800.toInt()) // Orange - action needed
                btnStep2.text = getString(R.string.step2_phone_app)
                btnStep2.isEnabled = true
            }

            // Step 3: Return button (always enabled)
            btnStep3.setBackgroundColor(0xFF9E9E9E.toInt()) // Grey
            btnStep3.text = getString(R.string.step3_return)
            btnStep3.isEnabled = true

        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao atualizar estado dos botões", e)
        }
    }

    /**
     * Shows a dialog with deactivation options
     */
    private fun showDeactivationDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.deactivate_dialog_title))
            builder.setMessage(getString(R.string.deactivate_dialog_message))
            
            builder.setPositiveButton(getString(R.string.remove_phone_app)) { _, _ ->
                removePhoneAppConfiguration()
            }
            
            builder.setNegativeButton(getString(R.string.cancel), null)
            
            builder.create().show()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing deactivation dialog", e)
            Toast.makeText(this, "Erro ao mostrar opções de desativação: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a simplified instructions dialog explaining our 2-step process
     */
    private fun showSimpleInstructionsDialog() {
        try {
            val dialog = AlertDialog.Builder(this, R.style.WhiteBackgroundDialog)
                .setTitle(getString(R.string.simple_instructions_title))
                .setMessage(getString(R.string.simple_instructions_content))
                .setPositiveButton(getString(R.string.got_it), null)
                .setCancelable(true)
                .create()
            
            // Ensure white background
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
            dialog.show()
                
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing simple instructions dialog", e)
            Toast.makeText(this, "Erro ao mostrar instruções: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Removes the app as default phone app
     */
    private fun removePhoneAppConfiguration() {
        try {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_phone_app_title))
                .setMessage(getString(R.string.remove_phone_app_message))
                .setPositiveButton(getString(R.string.open_phone_settings)) { _, _ ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val intents = listOf(
                                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                },
                                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                },
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                            )
                            
                            var opened = false
                            for ((index, intent) in intents.withIndex()) {
                                try {
                                    startActivity(intent)
                                    opened = true
                                    break
                                } catch (e: Exception) {
                                    Log.w("MainActivity", "Intent $index failed", e)
                                }
                            }
                            
                            if (!opened) {
                                Toast.makeText(this, "❌ Não foi possível abrir configurações", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            // Android < 10
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                        
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error opening phone app settings", e)
                        Toast.makeText(this, "❌ Erro ao abrir configurações: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
                
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in removePhoneAppConfiguration", e)
            Toast.makeText(this, "❌ Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
