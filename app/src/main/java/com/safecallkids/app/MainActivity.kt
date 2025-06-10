package com.safecallkids.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
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
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        checkPermissions()
        updateUI()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        contactsCount = findViewById(R.id.contactsCount)
        blockedCount = findViewById(R.id.blockedCount)
        enableButton = findViewById(R.id.enableButton)
        
        enableButton.setOnClickListener {
            if (hasAllPermissions()) {
                openAppSettings()
            } else {
                requestPermissions()
            }
        }
    }
    
    private fun checkPermissions() {
        if (hasAllPermissions()) {
            startCallBlockingService()
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }
    
    private fun startCallBlockingService() {
        val serviceIntent = Intent(this, CallBlockingService::class.java)
        startForegroundService(serviceIntent)
    }
    
    private fun updateUI() {
        if (hasAllPermissions()) {
            statusText.text = getString(R.string.protection_enabled)
            enableButton.text = "Configurações do App"
            
            // Atualizar contadores (implementar posteriormente)
            val contactsHelper = ContactsHelper(this)
            val contactsNum = contactsHelper.getContactsCount()
            contactsCount.text = "Contatos carregados: $contactsNum"
            
            // Recuperar número de chamadas bloqueadas do SharedPreferences
            val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
            val blocked = prefs.getInt("blocked_calls_count", 0)
            blockedCount.text = "Chamadas bloqueadas: $blocked"
        } else {
            statusText.text = getString(R.string.protection_disabled)
            enableButton.text = getString(R.string.grant_permissions)
            contactsCount.text = "Contatos carregados: 0"
            blockedCount.text = "Chamadas bloqueadas: 0"
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (hasAllPermissions()) {
                startCallBlockingService()
            }
            updateUI()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
