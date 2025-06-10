package com.safecallkids.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class CallBlockingService : android.app.Service() {
    
    private val TAG = "CallBlockingService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "safecall_channel"
    
    private val notificationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.safecallkids.app.UPDATE_NOTIFICATION") {
                updateNotification()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        
        // Registrar receiver para atualizar notificação
        val filter = IntentFilter("com.safecallkids.app.UPDATE_NOTIFICATION")
        registerReceiver(notificationUpdateReceiver, filter)
    }
    
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        return android.app.Service.START_STICKY // Reiniciar o serviço se for morto pelo sistema
    }
    
    override fun onBind(intent: android.content.Intent?): android.os.IBinder? {
        return null
    }
      override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // Desregistrar receiver
        try {
            unregisterReceiver(notificationUpdateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "SafecallKids Protection",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Serviço de proteção contra chamadas indesejadas"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        val intent = android.content.Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Obter estatísticas com tratamento de erro
        val prefs = getSharedPreferences("safecall_prefs", android.content.Context.MODE_PRIVATE)
        val blockedCount = prefs.getInt("blocked_calls_count", 0)
        
        val contactsCount = try {
            val contactsHelper = ContactsHelper(this)
            contactsHelper.getContactsCount()
        } catch (e: SecurityException) {
            Log.w(TAG, "No contacts permission for notification", e)
            0        } catch (e: Exception) {
            Log.e(TAG, "Error getting contacts count for notification", e)
            0
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafecallKids Ativo")
            .setContentText("Proteção ativa • $contactsCount contatos • $blockedCount bloqueadas")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }
    
    fun updateNotification() {
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}
