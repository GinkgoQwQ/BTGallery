package com.example.bluetoothimagetransfer.data

import android.bluetooth.BluetoothDevice
import java.io.File

data class DeviceItem(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)

data class ImageItem(
    val file: File,
    val name: String
)