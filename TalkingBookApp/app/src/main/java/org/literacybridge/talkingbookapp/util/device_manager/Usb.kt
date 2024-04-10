/*
 * Copyright 2015 Umbrela Smart, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.literacybridge.talkingbookapp.util.device_manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.literacybridge.core.fs.TbFile
import org.literacybridge.core.tbdevice.TbDeviceInfo
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Paths


class Usb {
    enum class ConnectionMode {
        MASS_STORAGE,
        DFU
    }

    var connectionMode: ConnectionMode? = null
        private set

    var mUsbManager: UsbManager? = null
        private set

    var usbDevice: UsbDevice? = null
        private set

    var talkingBookDevice: TalkingBook? = null
        private set

    private var mConnection: UsbDeviceConnection? = null
    private var mInterface: UsbInterface? = null
    var deviceVersion = 0
        private set

    /* Callback Interface */
    interface OnUsbChangeListener {
        fun onUsbConnected()

        fun onUsbDisconnected()
    }

    fun setOnUsbChangeListener(l: OnUsbChangeListener?) {
        mOnUsbChangeListener = l
    }

    private var mOnUsbChangeListener: OnUsbChangeListener? = null

    /* Broadcast Receiver*/
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                            setDevice(device)
                            if (mOnUsbChangeListener != null) {
                                mOnUsbChangeListener!!.onUsbConnected()
                            }
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                synchronized(this) {
                    // Request permission for just attached USB Device if it matches the VID/PID
                    requestPermission(App.context)
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (usbDevice != null && usbDevice == device) {
                        release()
                        mOnUsbChangeListener?.onUsbDisconnected()
                    }
                }
            }
        }
    }

    /**
     * Force disconnection of Talking Book device by setting the device instance to null,
     * which tricks the apply into thinking no device is connected.
     * Android does not provide any API to unmount an external storage, the user has to
     * manually disconnect the device.
     */
    fun forceDisconnectDevice(){
        mOnUsbChangeListener?.onUsbDisconnected()
    }

    fun getmUsbReceiver(): BroadcastReceiver {
        return mUsbReceiver
    }

    fun setUsbManager(usbManager: UsbManager?) {
        mUsbManager = usbManager
    }

    fun requestPermission(context: Context) {
        // FLAG_MUTABLE intent flag has to be used in some API levels (Bug in Android?)
        // refer to https://stackoverflow.com/questions/73267829/androidstudio-usb-extra-permission-granted-returns-false-always
        // for more details
        var flag = PendingIntent.FLAG_IMMUTABLE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            flag = PendingIntent.FLAG_MUTABLE
        }

        val permissionIntent =
            PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION),
                flag
            )


        // First, check if the device is connected in mass storage mode
        val device = getUsbDevice(MASS_STORAGE_VENDOR_LIST, MASS_STORAGE_PRODUCT_LIST)
        if (device != null) {
            mUsbManager!!.requestPermission(device, permissionIntent)
            return
        }

        // If no device is found, look for devices connected in dfu mode
        getUsbDevice(DFU_VENDOR_LIST, DFU_PRODUCT_LIST)?.let {
            mUsbManager!!.requestPermission(it, permissionIntent)
        }
    }

    private fun getUsbDevice(vendorList: List<Int>, productList: List<Int>): UsbDevice? {
        val deviceList = mUsbManager!!.deviceList
        val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
        var device: UsbDevice

        Log.d(TAG, "Looking for new connected device $deviceIterator")

        while (deviceIterator.hasNext()) {
            device = deviceIterator.next()
            if (device.vendorId in vendorList && device.productId in productList) {
                Log.d(TAG, "Found device -> $device")

                return device
            }
        }
        return null
    }

    fun release(): Boolean {
        var isReleased = false
        if (mConnection != null) {
            isReleased = mConnection!!.releaseInterface(mInterface)
            mConnection!!.close()
            mConnection = null
        }

        return isReleased
    }

    private fun setDevice(device: UsbDevice) {
        Log.d(LOG_TAG, "permission granted --")
        Log.d(LOG_TAG, "Mass storage device $device")

        if (!mUsbManager!!.hasPermission(device)) {
            // Request permission from the user
            val pendingIntent = PendingIntent.getBroadcast(
                App.context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            mUsbManager!!.requestPermission(device, pendingIntent)

            return
        }

        usbDevice = device

        if (device.vendorId in MASS_STORAGE_VENDOR_LIST) {
            connectionMode = ConnectionMode.MASS_STORAGE
            mInterface = device.getInterface(MASS_STORAGE_INTERFACE)
        } else {
//            device.getKey()
            connectionMode = ConnectionMode.DFU
            mInterface = device.getInterface(DFU_INTERFACE)
        }

//        val connection = mUsbManager!!.openDevice(device)
//        if (connection != null && connection.claimInterface(mInterface, true)) {
            Log.i(TAG, "open SUCCESS")
//            mConnection = connection

            // get the bcdDevice version
//            val rawDescriptor = mConnection!!.rawDescriptors
//            deviceVersion = rawDescriptor[13].toInt() shl 8
//            deviceVersion = deviceVersion or rawDescriptor[12].toInt()
//            Log.i("USB", getDeviceInfo(device))

            // Create talking book instance
            val volumesMap: Map<String, MountedDevice> = getSecondaryMountedVolumesMap()
//            Log.d(TAG, "getSecondaryMountedVolumesMap: " + getSecondaryMountedVolumesMap().size())
//            val deviceBaseUri: Uri = Uri.parse(volumesMap.values.first<MountedDevice>().toString())

            val root = DocumentFile.fromFile(File(MASS_STORAGE_PATH))
            val externalStorage = Environment.getExternalStoragePublicDirectory(MASS_STORAGE_PATH)
            Log.d(LOG_TAG, externalStorage.absolutePath)
            val fs: TbFile = AndroidDocFile(Paths.get(MASS_STORAGE_PATH).toFile())
            talkingBookDevice = TalkingBook(
                fs,
                TbDeviceInfo.getSerialNumberFromFileSystem(fs),
                // TODO: get device label
//                device.getValue().mLabel,
                "Label",
                MASS_STORAGE_PATH
            )
//        } else {
//            Log.e(TAG, "open FAIL")
//            mConnection = null
//        }
    }

    val isConnected: Boolean
        get() = mConnection != null

    private fun getSecondaryMountedVolumesMap(): Map<String, MountedDevice> {
        val mStorageManager =
            App.context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumesMap: MutableMap<String, MountedDevice> = HashMap<String, MountedDevice>()

        try {
//            val volumes: Array<Any>
            val volumes: MutableList<StorageVolume> = mStorageManager.storageVolumes
//            volumes = getVolumeListMethod.invoke(mStorageManager)
            for (volume in volumes) {
                val getStateMethod: Method = volume.javaClass.getMethod("getState")
                val mState = getStateMethod.invoke(volume) as String
                val isPrimaryMethod: Method = volume.javaClass.getMethod("isPrimary")
                val mPrimary = isPrimaryMethod.invoke(volume) as Boolean
                if (!mPrimary && mState == "mounted") {
                    val getPathMethod: Method = volume.javaClass.getMethod("getPath")
                    val path = getPathMethod.invoke(volume) as String
                    val getUuidMethod: Method = volume.javaClass.getMethod("getUuid")
                    val uuid = getUuidMethod.invoke(volume) as String
                    val getUserLabelMethod: Method = volume.javaClass.getMethod("getUserLabel")
                    val userLabel = getUserLabelMethod.invoke(volume) as String
                    Log.d(
                        TAG,
                        "Found one mounted device: $uuid -> $volume, label=$userLabel"
                    )
                    if (uuid != null && path != null) {
                        volumesMap[uuid] = MountedDevice(userLabel, uuid, path)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to load list of mounted secondary storage devices")
        }
        return volumesMap
    }


    private class MountedDevice internal constructor(
        private val mLabel: String,
        private val mUuid: String,
        private val mPath: String
    )


    // FIXME: remove this function
    fun getDeviceInfo(device: UsbDevice?): String {
        if (device == null) return "No device found."
        val sb = StringBuilder()
        sb.append("Model: " + device.deviceName + "\n")
        sb.append("ID: " + device.deviceId + " (0x" + Integer.toHexString(device.deviceId) + ")" + "\n")
        sb.append("Class: " + device.deviceClass + "\n")
        sb.append("Subclass: " + device.deviceSubclass + "\n")
        sb.append("Protocol: " + device.deviceProtocol + "\n")
        sb.append("Vendor ID " + device.vendorId + " (0x" + Integer.toHexString(device.vendorId) + ")" + "\n")
        sb.append("Product ID: " + device.productId + " (0x" + Integer.toHexString(device.productId) + ")" + "\n")
        sb.append("Device Ver: 0x" + Integer.toHexString(deviceVersion) + "\n")
        sb.append("Interface count: " + device.interfaceCount + "\n")
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            sb.append("Interface: $usbInterface\n")
            sb.append("Endpoint Count: " + usbInterface.endpointCount + "\n")
            for (j in 0 until usbInterface.endpointCount) {
                val ep = usbInterface.getEndpoint(j)
                sb.append("Endpoint: $ep\n")
            }
        }
        return sb.toString()
    }

    /**
     * Performs a control transaction on endpoint zero for this device.
     * The direction of the transfer is determined by the request type.
     * If requestType & [android.hardware.usb.UsbConstants.USB_ENDPOINT_DIR_MASK] is
     * [android.hardware.usb.UsbConstants.USB_DIR_OUT], then the transfer is a write,
     * and if it is [android.hardware.usb.UsbConstants.USB_DIR_IN], then the transfer
     * is a read.
     *
     * @param requestType MSB selects direction, rest defines to whom request is addressed
     * @param request     DFU command ID
     * @param value       0 for commands, >0 for firmware blocks
     * @param index       often 0
     * @param buffer      buffer for data portion of transaction,
     * or null if no data needs to be sent or received
     * @param length      the length of the data to send or receive
     * @param timeout     50ms f
     * @return length of data transferred (or zero) for success,
     * or negative value for failure
     */
    fun controlTransfer(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        buffer: ByteArray?,
        length: Int,
        timeout: Int
    ): Int {
        synchronized(this) {
            return mConnection!!.controlTransfer(
                requestType,
                request,
                value,
                index,
                buffer,
                length,
                timeout
            )
        }
    }

    companion object {
        const val TAG = "$LOG_TAG: USB"

        /* USB DFU ID's (may differ by device) */
        val DFU_VENDOR_LIST = listOf(1155) // 0x0483
        val DFU_PRODUCT_LIST = listOf(57105) // 0xDF11
        const val DFU_INTERFACE = 3

        const val USB_VENDOR_ID = 1155 // VID while in DFU mode 0x0483
        const val USB_PRODUCT_ID = 57105 // PID while in DFU mode 0xDF11

        // Mass storage device ID's
        val MASS_STORAGE_VENDOR_LIST = listOf(49745)
        val MASS_STORAGE_PRODUCT_LIST = listOf(21570)
        const val MASS_STORAGE_PATH = "/storage/1234-5678"
        const val MASS_STORAGE_INTERFACE = 0

        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"


        @Volatile
        private var instance: Usb? = null // Volatile modifier is necessary

        fun getInstance() =
            instance ?: synchronized(this) { // synchronized to avoid concurrency problem
                instance ?: Usb().also { instance = it }
            }
    }


    class TalkingBook(
        val root: TbFile,
        val serialNumber: String,
        val deviceLabel: String,
        private val path: String
    )
}
