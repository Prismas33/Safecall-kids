package com.safecallkids.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class CallBlockingService : Service() {
    
    private val TAG = "CallBlockingService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "safecall_channel"
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        return START_STICKY // Reiniciar o serviço se for morto pelo sistema
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafecallKids Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Serviço de proteção contra chamadas indesejadas"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Obter estatísticas
        val prefs = getSharedPreferences("safecall_prefs", MODE_PRIVATE)
        val blockedCount = prefs.getInt("blocked_calls_count", 0)
        
        val contactsHelper = ContactsHelper(this)
        val contactsCount = contactsHelper.getContactsCount()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafecallKids Ativo")
            .setContentText("Proteção ativa • $contactsCount contatos • $blockedCount bloqueadas")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }
    
    fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}
