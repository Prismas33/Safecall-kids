package com.safecallkids.app

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Activity "vazia" para tornar o app elegível como discador padrão.
 * Não exibe UI, apenas finaliza imediatamente.
 */
class DialerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Se chamado como discador, apenas mostra uma mensagem e fecha
        val data: Uri? = this.intent?.data
        if (data != null && data.scheme == "tel") {
            Toast.makeText(this, "SafecallKids não faz chamadas, apenas protege!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
