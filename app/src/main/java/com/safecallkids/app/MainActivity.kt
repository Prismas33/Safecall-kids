package com.safecallkids.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var contactsCount: TextView
    private lateinit var blockedCount: TextView
    private lateinit var enableButton: Button
    
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
      private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Resultado do defaultDialerLauncher: ${result.resultCode}")
        
        // Aguardar um pouco para o sistema processar a mudança
        Thread.sleep(1000)
        
        // Forçar verificação do status
        if (isDefaultDialer()) {
            Log.d("MainActivity", "Agora somos o discador padrão!")
            Toast.makeText(this, "SafecallKids agora é o app de telefone padrão!", Toast.LENGTH_LONG).show()
        } else {
            Log.w("MainActivity", "Ainda não somos o discador padrão")
            Toast.makeText(this, "Configure manualmente: Configurações > Apps > Apps padrão > App de telefone", Toast.LENGTH_LONG).show()
        }
        
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        updateUI()
    }
      private fun initViews() {
        statusText = findViewById(R.id.statusText)
        contactsCount = findViewById(R.id.contactsCount)
        blockedCount = findViewById(R.id.blockedCount)
        enableButton = findViewById(R.id.enableButton)
        
        enableButton.setOnClickListener {
            if (hasAllProtection()) {
                openAppSettings()
            } else {
                requestAllProtection()
            }
        }
        
        // Clique longo para diagnóstico
        enableButton.setOnLongClickListener {
            runDiagnostic()
            true
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }    private fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                val defaultPackage = telecomManager.defaultDialerPackage
                val isDefault = packageName == defaultPackage
                
                Log.d("MainActivity", "=== VERIFICAÇÃO DISCADOR PADRÃO ===")
                Log.d("MainActivity", "Nosso package: $packageName")
                Log.d("MainActivity", "Discador padrão atual: $defaultPackage")
                Log.d("MainActivity", "É o padrão: $isDefault")
                
                // Verificar se temos PhoneAccount registrado
                try {
                    val phoneAccounts = telecomManager.callCapablePhoneAccounts
                    val hasPhoneAccount = phoneAccounts.any { 
                        it.componentName.packageName == packageName 
                    }
                    Log.d("MainActivity", "Tem PhoneAccount registrado: $hasPhoneAccount")
                    Log.d("MainActivity", "Total de contas: ${phoneAccounts.size}")
                    
                    // Lista todas as contas para debug
                    phoneAccounts.forEachIndexed { index, account ->
                        Log.d("MainActivity", "Conta $index: ${account.componentName.packageName}")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao listar PhoneAccounts", e)
                }
                
                isDefault
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao verificar discador padrão", e)
                false
            }
        } else {
            Log.d("MainActivity", "Android < M, assumindo verdadeiro")
            true
        }
    }
    
    private fun registerPhoneAccount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                val phoneAccountHandle = PhoneAccountHandle(
                    ComponentName(this, MyConnectionService::class.java),
                    "SafecallKids"
                )
                
                val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "SafecallKids")
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setHighlightColor(ContextCompat.getColor(this, R.color.purple_500))
                    .build()
                
                telecomManager.registerPhoneAccount(phoneAccount)
                Log.d("MainActivity", "Phone account registered successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error registering phone account", e)
                Toast.makeText(this, "Erro ao registrar conta de telefone: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun hasAllProtection(): Boolean {
        return hasAllPermissions() && hasOverlayPermission() && isDefaultDialer()
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        }
    }    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Log.d("MainActivity", "=== SOLICITANDO DISCADOR PADRÃO ===")
                
                // Primeiro registrar a conta de telefone
                registerPhoneAccount()
                
                // Aguardar um pouco para o registro processar
                Thread.sleep(500)
                
                // Verificar se já é o discador padrão
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                val currentDefault = telecomManager.defaultDialerPackage
                
                Log.d("MainActivity", "Discador atual antes da solicitação: $currentDefault")
                
                if (packageName == currentDefault) {
                    Log.d("MainActivity", "App já é o discador padrão")
                    updateUI()
                    return
                }
                
                // Verificar se o intent está disponível
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                
                if (intent.resolveActivity(packageManager) != null) {
                    Log.d("MainActivity", "Lançando intent para mudança de discador padrão")
                    defaultDialerLauncher.launch(intent)
                } else {
                    Log.e("MainActivity", "Intent de mudança de discador não disponível")
                    Toast.makeText(this, "Sistema não suporta mudança de discador padrão", Toast.LENGTH_LONG).show()
                    
                    // Tentar abrir configurações manualmente
                    openDefaultAppsSettings()
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao solicitar discador padrão", e)
                Toast.makeText(this, "Erro ao solicitar app de telefone padrão: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Tentar abrir configurações como fallback
                openDefaultAppsSettings()
            }
        }
    }
    
    private fun startCallBlockingService() {
        try {
            val serviceIntent = Intent(this, CallBlockingService::class.java)
            startForegroundService(serviceIntent)
            Log.d("MainActivity", "Call blocking service started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting service", e)
            Toast.makeText(this, "Erro ao iniciar serviço: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {
        try {
            if (hasAllProtection()) {
                statusText.text = getString(R.string.protection_enabled)
                enableButton.text = "Configurações do App"
                
                // Carregar contatos com tratamento de erro
                try {
                    val contactsHelper = ContactsHelper(this)
                    val contactsNum = contactsHelper.getContactsCount()
                    contactsCount.text = "Contatos carregados: $contactsNum"
                } catch (e: SecurityException) {
                    Log.w("MainActivity", "Permissão de contatos negada", e)
                    contactsCount.text = "Contatos carregados: permissão negada"
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao carregar contatos", e)
                    contactsCount.text = "Contatos carregados: erro"
                }
                
                // Carregar contador de chamadas bloqueadas
                try {
                    val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
                    val blocked = prefs.getInt("blocked_calls_count", 0)
                    blockedCount.text = "Chamadas bloqueadas: $blocked"
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao acessar preferências", e)
                    blockedCount.text = "Chamadas bloqueadas: erro"
                }
                
                // Iniciar serviço se ainda não estiver rodando
                if (hasAllPermissions()) {
                    startCallBlockingService()
                }
                
            } else {
                val missing = mutableListOf<String>()
                if (!hasAllPermissions()) missing.add("Permissões básicas")
                if (!hasOverlayPermission()) missing.add("Sobrepor apps")
                if (!isDefaultDialer()) missing.add("App de telefone padrão")
                
                statusText.text = "Proteção Desativada\nFaltando: ${missing.joinToString(", ")}"
                enableButton.text = getString(R.string.grant_permissions)
                contactsCount.text = "Contatos carregados: 0"
                blockedCount.text = "Chamadas bloqueadas: 0"
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao atualizar UI", e)
            statusText.text = "Erro na interface"
            enableButton.text = "Tentar novamente"
        }
    }
    
    private fun requestAllProtection() {
        try {
            // Solicitar permissões básicas
            if (!hasAllPermissions()) {
                requestPermissions()
                return // Aguardar resultado das permissões
            }
            
            // Solicitar overlay permission
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return
            }
            
            // Solicitar para ser discador padrão
            if (!isDefaultDialer()) {
                requestDefaultDialer()
                return
            }
            
            // Se chegou aqui, todas as permissões estão concedidas
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
    
    private fun openDefaultAppsSettings() {
        try {
            Log.d("MainActivity", "Tentando abrir configurações de apps padrão")
            
            // Tentar diferentes intents para abrir configurações de apps padrão
            val intents = listOf(
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
                Intent(Settings.ACTION_APPLICATION_SETTINGS)
            )
            
            for (intent in intents) {
                try {
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        Toast.makeText(this, "Procure por 'App de telefone' e selecione SafecallKids", Toast.LENGTH_LONG).show()
                        return
                    }
                } catch (e: Exception) {
                    Log.w("MainActivity", "Intent não funcionou: ${intent.action}", e)
                }
            }
            
            // Fallback: abrir configurações gerais
            startActivity(Intent(Settings.ACTION_SETTINGS))
            Toast.makeText(this, "Vá em Aplicações > Apps padrão > App de telefone", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao abrir configurações", e)
            Toast.makeText(this, "Não foi possível abrir configurações", Toast.LENGTH_SHORT).show()
        }
    }
      private fun checkAndFixDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                
                // Verificar se temos PhoneAccount mas não somos o discador padrão
                val phoneAccounts = telecomManager.callCapablePhoneAccounts
                val hasPhoneAccount = phoneAccounts.any { 
                    it.componentName.packageName == packageName 
                }
                val isDefault = packageName == telecomManager.defaultDialerPackage
                
                Log.d("MainActivity", "=== DIAGNÓSTICO DISCADOR ===")
                Log.d("MainActivity", "Tem PhoneAccount: $hasPhoneAccount")
                Log.d("MainActivity", "É discador padrão: $isDefault")
                
                // Se não temos PhoneAccount, registrar
                if (!hasPhoneAccount) {
                    Log.d("MainActivity", "PhoneAccount não encontrado, registrando...")
                    registerPhoneAccount()
                    
                    // Aguardar e verificar novamente
                    Thread.sleep(1000)
                    val phoneAccountsAfter = telecomManager.callCapablePhoneAccounts
                    val hasPhoneAccountAfter = phoneAccountsAfter.any { 
                        it.componentName.packageName == packageName 
                    }
                    Log.d("MainActivity", "PhoneAccount após registro: $hasPhoneAccountAfter")
                }
                
                if (hasPhoneAccount && !isDefault) {
                    Log.w("MainActivity", "Temos PhoneAccount mas não somos o discador padrão!")
                    
                    // Verificar novamente após um pequeno delay
                    Thread.sleep(2000)
                    val isDefaultAfterDelay = packageName == telecomManager.defaultDialerPackage
                    Log.d("MainActivity", "Status após delay: $isDefaultAfterDelay")
                    
                    if (!isDefaultAfterDelay) {
                        Toast.makeText(this, "Para funcionar corretamente, defina SafecallKids como app de telefone padrão", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("MainActivity", "Status corrigido após delay!")
                        updateUI()
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro no diagnóstico do discador", e)
            }
        }
    }

    private fun runDiagnostic() {
        Log.d("MainActivity", "=== DIAGNÓSTICO COMPLETO ===")
        
        try {
            // Verificar permissões básicas
            REQUIRED_PERMISSIONS.forEach { permission ->
                val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                Log.d("MainActivity", "Permissão $permission: $granted")
            }
            
            // Verificar overlay
            val overlay = hasOverlayPermission()
            Log.d("MainActivity", "Overlay permission: $overlay")
            
            // Verificar discador padrão com detalhes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                val defaultPackage = telecomManager.defaultDialerPackage
                val isDefault = packageName == defaultPackage
                
                Log.d("MainActivity", "Package atual: $packageName")
                Log.d("MainActivity", "Discador padrão: $defaultPackage")
                Log.d("MainActivity", "É o padrão: $isDefault")
                
                // Listar todas as contas telefônicas
                try {
                    val phoneAccounts = telecomManager.callCapablePhoneAccounts
                    Log.d("MainActivity", "Total de PhoneAccounts: ${phoneAccounts.size}")
                    
                    phoneAccounts.forEachIndexed { index, account ->
                        val pkg = account.componentName.packageName
                        val cls = account.componentName.className
                        Log.d("MainActivity", "Conta $index: $pkg / $cls")
                    }
                    
                    val ourAccount = phoneAccounts.find { it.componentName.packageName == packageName }
                    Log.d("MainActivity", "Nossa conta encontrada: ${ourAccount != null}")
                    
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao listar contas", e)
                }
            }
            
            Toast.makeText(this, "Diagnóstico completo - veja os logs", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro no diagnóstico", e)
            Toast.makeText(this, "Erro no diagnóstico: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            Log.d("MainActivity", "Resultado das permissões recebido")
            
            // Verificar se todas as permissões foram concedidas
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Log.d("MainActivity", "Todas as permissões concedidas")
                // Continuar com overlay e discador padrão se necessário
                requestAllProtection()
            } else {
                Log.w("MainActivity", "Algumas permissões foram negadas")
                Toast.makeText(this, "Algumas permissões são necessárias para o funcionamento do app", Toast.LENGTH_LONG).show()
            }
            
            updateUI()
        }
    }    override fun onResume() {
        super.onResume()
        
        // Aguardar um pouco antes de verificar o status para dar tempo ao sistema
        Thread.sleep(500)
        
        updateUI()
        
        // Verificar e tentar corrigir problemas com discador padrão
        checkAndFixDefaultDialer()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "MainActivity destruída")
    }
}
