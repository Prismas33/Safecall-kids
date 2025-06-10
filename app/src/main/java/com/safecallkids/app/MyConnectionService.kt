package com.safecallkids.app

import android.telecom.Connection
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.ConnectionRequest

class MyConnectionService : ConnectionService() {
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        // Retorna uma conexão nula, pois o app não faz chamadas reais
        return null
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        // Retorna uma conexão nula, pois o app não faz chamadas reais
        return null
    }
}
