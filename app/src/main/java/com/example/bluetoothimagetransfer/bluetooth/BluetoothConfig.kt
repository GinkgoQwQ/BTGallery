package com.example.bluetoothimagetransfer.bluetooth

import java.util.UUID

object BluetoothConfig {
    const val TAG = "BluetoothImageTransfer"
    const val SERVICE_NAME = "BluetoothImageTransfer"
    val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
}