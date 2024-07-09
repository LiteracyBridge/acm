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
package org.literacybridge.androidtbloader.util.device_manager

import android.nfc.FormatException
import android.os.Environment
import android.util.Log
import org.literacybridge.androidtbloader.App
import org.literacybridge.androidtbloader.R
import org.literacybridge.androidtbloader.util.Constants.Companion.LOG_TAG
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

class Dfu(private val deviceVid: Int, private val devicePid: Int) {
    private val dfuFile: DfuFile
    private var usb: Usb? = null
    private var deviceVersion = 0 //STM bootloader version
    private val listeners: MutableList<DfuListener> = ArrayList()

    interface DfuListener {
        fun onStatusMsg(msg: String?)

        /**
         * Called when programing is completed successfully
         */
        fun onProgramingCompleted()

    }

    private fun onStatusMsg(msg: String) {
        for (listener in listeners) {
            listener.onStatusMsg(msg)
        }
    }

    /**
     * Called when programing is completed successfully
     */
    private fun onProgramingCompleted() {
        for (listener in listeners) {
            listener.onProgramingCompleted()
        }
    }

    fun setListener(listener: DfuListener?) {
        requireNotNull(listener) { "Listener is null" }
        listeners.add(listener)
    }

    fun setUsb(usb: Usb?) {
        this.usb = usb
        Log.d(LOG_TAG, "Device found $usb");

        if (usb != null) {
            deviceVersion = usb.deviceVersion
        }
    }

    /* One-Click Programming Method to fully flash the connected device
         This will try everything that it can do to program, if it throws execptions
         it failed on something it cannot fix.
  */
    @Throws(Exception::class)
    fun programFirmware(filePath: String?): Boolean {
        val MAX_ALLOWED_RETRIES = 5

        openFile(filePath)
        verifyFile()
//        checkCompatibility()

        if (isDeviceProtected) {
            Log.i(TAG, "Device is protected")
            Log.i(TAG, "Removing Read Protection")
            removeReadProtection()
            Log.i(TAG, "Device is resetting")
            return false // device will reset
        }

        for (i in MAX_ALLOWED_RETRIES + 1 downTo 1) {
            if (isDeviceBlank) break
            if (i == 1) {
                throw Exception("Cannot Mass Erase, REPLACE UNIT!")
            }
            Log.i(TAG, "Device not blank, erasing")
            massErase()
        }

        writeImage()

        for (i in MAX_ALLOWED_RETRIES + 1 downTo 1) {
            if (isWrittenImageOk) {
                Log.i(TAG, "Writing Option Bytes, will self-reset")
                val selectOptions =
                    OPT_RDP_OFF or OPT_WDG_SW or OPT_nRST_STOP or OPT_nRST_STDBY or OPT_BOR_1 // todo in production, OPT_RDP_1 must be set instead of OPT_RDP_OFF
                writeOptionBytes(selectOptions) // will reset device
                break
            }
            if (i == 1) {
                throw Exception("Cannot Write successfully, REPLACE UNIT!")
            }
            Log.i(TAG, "Verification failed, retry")
            massErase()
            writeImage()
        }
        return true
    }

    @get:Throws(Exception::class)
    private val isDeviceBlank: Boolean
        get() {
            val readContent = ByteArray(dfuFile.elementLength)
            readImage(readContent)
            val read = ByteBuffer.wrap(readContent) // wrap whole array
            val hash = read.hashCode()
            return dfuFile.elementLength == Math.abs(hash)
        }

    @get:Throws(Exception::class)
    private val isWrittenImageOk: Boolean
        // similar to verify()
        get() {
            val deviceFirmware = ByteArray(dfuFile.elementLength)
            val startTime = System.currentTimeMillis()
            readImage(deviceFirmware)
            // create byte buffer and compare content
            val fileFw = ByteBuffer.wrap(
                dfuFile.file,
                ELEMENT1_OFFSET,
                dfuFile.elementLength
            ) // set offset and limit of firmware
            val deviceFw = ByteBuffer.wrap(deviceFirmware) // wrap whole array
            val result = fileFw == deviceFw
            Log.i(TAG, "Verified completed in " + (System.currentTimeMillis() - startTime) + " ms")
            return result
        }

    private fun massErase() {
        if (!isUsbConnected) return
        val dfuStatus = DfuStatus()
        val startTime = System.currentTimeMillis() // note current time
        try {
            do {
                clearStatus()
                getStatus(dfuStatus)
            } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
            if (isDeviceProtected) {
                removeReadProtection()
                onStatusMsg("Read Protection removed. Device resets...Wait until it   re-enumerates ") // XXX This will reset the device
                return
            }
            massEraseCommand() // sent erase command request
            getStatus(dfuStatus) // initiate erase command, returns 'download busy' even if invalid address or ROP
            val pollingTime = dfuStatus.bwPollTimeout // note requested waiting time
            do {
                /* wait specified time before next getStatus call */
                Thread.sleep(pollingTime.toLong())
                clearStatus()
                getStatus(dfuStatus)
            } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
            onStatusMsg("Mass erase completed in " + (System.currentTimeMillis() - startTime) + " ms")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: Exception) {
            onStatusMsg(e.toString())
        }
    }

    fun fastOperations() {
        if (!isUsbConnected) return
        val dfuStatus = DfuStatus()
        val configBytes = ByteArray(4)
        try {
            if (isDeviceProtected) {
                onStatusMsg("Device is Read-Protected...First Mass Erase")
                return
            }
            readDeviceFeature(configBytes)
            if (configBytes[0].toInt() != 0x03) {
                configBytes[0] = 0x03
                download(configBytes, 2)
                getStatus(dfuStatus)
                getStatus(dfuStatus)
                while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
                    clearStatus()
                    getStatus(dfuStatus)
                }
                onStatusMsg("Fast Operations set (Parallelism x32)")
            } else {
                onStatusMsg("Fast Operations was already set (Parallelism x32)")
            }
        } catch (e: Exception) {
            onStatusMsg(e.toString())
        }
    }

    fun program() {
        val MAX_ALLOWED_RETRIES = 5
        if (!isUsbConnected) return

        openFile()
        verifyFile()
        checkCompatibility()

        if (isDeviceProtected) {
            Log.i(LOG_TAG, "Device is protected")
            Log.i(LOG_TAG, "Removing Read Protection")
            removeReadProtection()
            Log.i(LOG_TAG, "Device is resetting")
            return  // device will reset
        }

//        for (i in MAX_ALLOWED_RETRIES + 1 downTo 1) {
//            if (isDeviceBlank) break
//            if (i == 1) {
//                throw Exception("Cannot Mass Erase, REPLACE UNIT!")
//            }
//            Log.i(LOG_TAG, "Device not blank, erasing")
//            massErase()
//        }

        writeImage()

        for (i in MAX_ALLOWED_RETRIES + 1 downTo 1) {
            if (isWrittenImageOk) {
                Log.i(TAG, "Writing Option Bytes, will self-reset")
                val selectOptions =
                    OPT_RDP_OFF or OPT_WDG_SW or OPT_nRST_STOP or OPT_nRST_STDBY or OPT_BOR_1 // todo in production, OPT_RDP_1 must be set instead of OPT_RDP_OFF
                writeOptionBytes(selectOptions) // will reset device
                onProgramingCompleted()
                break
            }
            if (i == 1) {
                throw Exception("Cannot Write successfully, REPLACE UNIT!")
            }
            Log.i(TAG, "Verification failed, retry")
            massErase()
            writeImage()
        }
    }

    fun verify() {
        if (!isUsbConnected) return
        try {
            if (isDeviceProtected) {
                onStatusMsg("Device is Read-Protected...First Mass Erase")
                return
            }
            if (dfuFile.file.isNotEmpty()) {
                openFile()
                verifyFile()
                checkCompatibility()
            }
            val deviceFirmware = ByteArray(dfuFile.elementLength)
            readImage(deviceFirmware)

            // create byte buffer and compare content
            val fileFw = ByteBuffer.wrap(
                dfuFile.file,
                ELEMENT1_OFFSET,
                dfuFile.elementLength
            ) // set offset and limit of firmware
            val deviceFw = ByteBuffer.wrap(deviceFirmware) // wrap whole array

            // fixme: this is failing
//            if (fileFw.equals(deviceFw)) {        // compares type, length, content
//                onStatusMsg("device firmware equals file firmware");
//            } else {
//                onStatusMsg("device firmware does not equals file firmware");
//            }
        } catch (e: Exception) {
            e.printStackTrace()
            onStatusMsg(e.toString())
        }
    }

    private val isUsbConnected: Boolean
        // check if usb device is active
        get() {
            if (usb != null && usb!!.isConnected) {
                return true
            }
            onStatusMsg("No device connected")
            return false
        }

    fun leaveDfuMode() {
        try {
            detach(mInternalFlashStartAddress)
        } catch (e: Exception) {
            e.printStackTrace()
            onStatusMsg(e.toString())
        }
    }

    @Throws(Exception::class)
    private fun removeReadProtection() {
        val dfuStatus = DfuStatus()
        unProtectCommand()
        getStatus(dfuStatus)
        if (dfuStatus.bState.toInt() != STATE_DFU_DOWNLOAD_BUSY) {
            throw Exception("Failed to execute unprotect command")
        }
        usb!!.release() // XXX device will self-reset
        Log.i(TAG, "USB was released")
    }

    @Throws(Exception::class)
    private fun readDeviceFeature(configBytes: ByteArray) {
        val dfuStatus = DfuStatus()
        do {
            clearStatus()
            getStatus(dfuStatus)
        } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
        setAddressPointer(-0x10000)
        getStatus(dfuStatus)
        getStatus(dfuStatus)
        if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
            throw Exception("Fast Operations not supported")
        }
        while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
            clearStatus()
            getStatus(dfuStatus)
        }
        upload(configBytes, configBytes.size, 2)
        getStatus(dfuStatus)
        while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
            clearStatus()
            getStatus(dfuStatus)
        }
    }

    @Throws(Exception::class)
    private fun writeImage() {
        val address = dfuFile.elementStartAddress // flash start address
        val fileOffset = ELEMENT1_OFFSET // index offset of file
        val blockSize = dfuFile.maxBlockSize // max block size
        val Block = ByteArray(blockSize)
        val NumOfBlocks = dfuFile.elementLength / blockSize
        var blockNum: Int
        blockNum = 0
        while (blockNum < NumOfBlocks) {
            System.arraycopy(dfuFile.file, blockNum * blockSize + fileOffset, Block, 0, blockSize)
            // send out the block to device
            writeBlock(address, Block, blockNum)
            blockNum++
        }
        // check if last block is partial
        var remainder = dfuFile.elementLength - blockNum * blockSize
        if (remainder > 0) {
            System.arraycopy(dfuFile.file, blockNum * blockSize + fileOffset, Block, 0, remainder)
            // Pad with 0xFF so our CRC matches the ST Bootloader and the ULink's CRC
            while (remainder < Block.size) {
                Block[remainder++] = 0xFF.toByte()
            }
            // send out the block to device
            writeBlock(address, Block, blockNum)
        }
    }

    @Throws(Exception::class)
    private fun readImage(deviceFw: ByteArray) {
        val dfuStatus = DfuStatus()
        val maxBlockSize = dfuFile.maxBlockSize
        val startAddress = dfuFile.elementStartAddress
        val block = ByteArray(maxBlockSize)
        var nBlock: Int
        var remLength = deviceFw.size
        val numOfBlocks = remLength / maxBlockSize
        do {
            clearStatus()
            getStatus(dfuStatus)
        } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
        setAddressPointer(startAddress)
        getStatus(dfuStatus) // to execute
        getStatus(dfuStatus) //to verify
        if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
            throw Exception("Start address not supported")
        }


        // will read full and last partial blocks ( NOTE: last partial block will be read with maxkblocksize)
        nBlock = 0
        while (nBlock <= numOfBlocks) {
            while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {        // todo if fails, maybe stop reading
                clearStatus()
                getStatus(dfuStatus)
            }
            upload(block, maxBlockSize, nBlock + 2)
            getStatus(dfuStatus)
            if (remLength >= maxBlockSize) {
                remLength -= maxBlockSize
                System.arraycopy(block, 0, deviceFw, nBlock * maxBlockSize, maxBlockSize)
            } else {
                System.arraycopy(block, 0, deviceFw, nBlock * maxBlockSize, remLength)
            }
            nBlock++
        }
    }

    // this can be used if the filePath is known to .dfu file
    @Throws(Exception::class)
    private fun openFile(filePath: String?) {
        if (filePath == null) {
            throw FileNotFoundException("No file selected")
        }
        val myFile = File(filePath)
        if (!myFile.exists()) {
            throw FileNotFoundException("Cannot find: $myFile")
        }
        if (!myFile.canRead()) {
            throw FormatException("Cannot open: $myFile")
        }
        dfuFile.filePath = myFile.toString()
        dfuFile.file = ByteArray(myFile.length().toInt())
        //convert file into byte array
        val fileInputStream = FileInputStream(myFile)
        val readLength = fileInputStream.read(dfuFile.file)
        fileInputStream.close()
        if (readLength.toLong() != myFile.length()) {
            throw IOException("Could Not Read File")
        }
    }

    @Throws(Exception::class)
    private fun openFile() {
        val extDownload: File
        var myFilePath: String? = null
        var myFileName: String? = null
        val myFile: File

        // TODO: read from program spec first. Then fallback to included dfu image
        if (Environment.getExternalStorageState() != null) // todo not sure if this works
        {
            extDownload = File(Environment.getExternalStorageDirectory().toString() + "/Download/")
            if (extDownload.exists()) {
                val files = extDownload.list()
                // todo support multiple dfu files in dir
                if (files.size > 0) {   // will select first dfu file found in dir
                    for (file in files) {
                        if (file.endsWith(".dfu")) {
                            myFilePath = extDownload.toString()
                            myFileName = file
                            break
                        }
                    }
                }
            }
        }
        if (myFileName == null) throw Exception("No .dfu file found in Download Folder")

        val fileInputStream: InputStream = App.context.resources.openRawResource(R.raw.firmware_image)
//        dfuFile.filePath = myFile.toString()
        dfuFile.file = fileInputStream.readBytes()

        //convert file into byte array
//        fileInputStream = FileInputStream(myFile)
//        fileInputStream.read(dfuFile.file)
        fileInputStream.close()
    }

    @Throws(Exception::class)
    private fun verifyFile() {

        // todo for now i expect the file to be not corrupted
        val length = dfuFile.file.size
        var crcIndex = length - 4
        var crc = 0
        crc = crc or (dfuFile.file[crcIndex++].toInt() and 0xFF)
        crc = crc or (dfuFile.file[crcIndex++].toInt() and 0xFF shl 8)
        crc = crc or (dfuFile.file[crcIndex++].toInt() and 0xFF shl 16)
        crc = crc or (dfuFile.file[crcIndex].toInt() and 0xFF shl 24)
        // do crc check
        if (crc != calculateCRC(dfuFile.file)) {
            throw FormatException("CRC Failed")
        }

        // Check the prefix
        val prefix = String(dfuFile.file, 0, 5)
        if (prefix.compareTo("DfuSe") != 0) {
            throw FormatException("File signature error")
        }

        // check dfuSe Version
        if (dfuFile.file[5].toInt() != 1) {
            throw FormatException("DFU file version must be 1")
        }

        // Check the suffix
        val suffix = String(dfuFile.file, length - 8, 3)
        if (suffix.compareTo("UFD") != 0) {
            throw FormatException("File suffix error")
        }
        if (dfuFile.file[length - 5].toInt() != 16 || dfuFile.file[length - 10].toInt() != 0x1A || dfuFile.file[length - 9].toInt() != 0x01) {
            throw FormatException("File number error")
        }

        // Now check the target prefix, we assume there is only one target in the file
        val target = String(dfuFile.file, 11, 6)
        if (target.compareTo("Target") != 0) {
            throw FormatException("Target signature error")
        }
        if (0 != dfuFile.file[TARGET_NAME_START].toInt()) {
            val tempName = String(dfuFile.file, TARGET_NAME_START, TARGET_NAME_MAX_END)
            val foundNullAt = tempName.indexOf(0.toChar())
            dfuFile.TargetName = tempName.substring(0, foundNullAt)
        } else {
            throw FormatException("No Target Name Exist in File")
        }
        Log.i(TAG, "Firmware Target Name: " + dfuFile.TargetName)
        dfuFile.TargetSize = dfuFile.file[TARGET_SIZE].toInt() and 0xFF
        dfuFile.TargetSize =
            dfuFile.TargetSize or (dfuFile.file[TARGET_SIZE + 1].toInt() and 0xFF shl 8)
        dfuFile.TargetSize =
            dfuFile.TargetSize or (dfuFile.file[TARGET_SIZE + 2].toInt() and 0xFF shl 16)
        dfuFile.TargetSize =
            dfuFile.TargetSize or (dfuFile.file[TARGET_SIZE + 3].toInt() and 0xFF shl 24)
        Log.i(TAG, "Firmware Target Size: " + dfuFile.TargetSize)
        dfuFile.NumElements = dfuFile.file[TARGET_NUM_ELEMENTS].toInt() and 0xFF
        dfuFile.NumElements =
            dfuFile.NumElements or (dfuFile.file[TARGET_NUM_ELEMENTS + 1].toInt() and 0xFF shl 8)
        dfuFile.NumElements =
            dfuFile.NumElements or (dfuFile.file[TARGET_NUM_ELEMENTS + 2].toInt() and 0xFF shl 16)
        dfuFile.NumElements =
            dfuFile.NumElements or (dfuFile.file[TARGET_NUM_ELEMENTS + 3].toInt() and 0xFF shl 24)
        Log.i(TAG, "Firmware Num of Elements: " + dfuFile.NumElements)
        if (dfuFile.NumElements > 1) {
            throw FormatException("Do not support multiple Elements inside Image")
            /*  If you get this error, that means that the C-compiler IDE is treating the Reset Vector ISR
                and the data ( your code) as two separate elements.
                This problem has been observed with The Atollic TrueStudio V5.5.2
                The version of Atollic that works with this is v5.3.0
                The version of DfuSe FileManager is v3.0.3
                Refer to ST document UM0391 for more details on DfuSe format
             */
        }

        // Get Element Flash start address and size
        dfuFile.elementStartAddress = dfuFile.file[285].toInt() and 0xFF
        dfuFile.elementStartAddress =
            dfuFile.elementStartAddress or (dfuFile.file[286].toInt() and 0xFF shl 8)
        dfuFile.elementStartAddress =
            dfuFile.elementStartAddress or (dfuFile.file[287].toInt() and 0xFF shl 16)
        dfuFile.elementStartAddress =
            dfuFile.elementStartAddress or (dfuFile.file[288].toInt() and 0xFF shl 24)
        dfuFile.elementLength = dfuFile.file[289].toInt() and 0xFF
        dfuFile.elementLength = dfuFile.elementLength or (dfuFile.file[290].toInt() and 0xFF shl 8)
        dfuFile.elementLength = dfuFile.elementLength or (dfuFile.file[291].toInt() and 0xFF shl 16)
        dfuFile.elementLength = dfuFile.elementLength or (dfuFile.file[292].toInt() and 0xFF shl 24)
        if (dfuFile.elementLength < 512) {
            throw FormatException("Element Size is too small")
        }

        // Get VID, PID and version number
        dfuFile.VID = dfuFile.file[length - 11].toInt() and 0xFF shl 8
        dfuFile.VID = dfuFile.VID or (dfuFile.file[length - 12].toInt() and 0xFF)
        dfuFile.PID = dfuFile.file[length - 13].toInt() and 0xFF shl 8
        dfuFile.PID = dfuFile.PID or (dfuFile.file[length - 14].toInt() and 0xFF)
        dfuFile.BootVersion = dfuFile.file[length - 15].toInt() and 0xFF shl 8
        dfuFile.BootVersion = dfuFile.BootVersion or (dfuFile.file[length - 16].toInt() and 0xFF)
    }

    @Throws(Exception::class)
    private fun checkCompatibility() {
        if (devicePid != dfuFile.PID || deviceVid != dfuFile.VID) {
            throw FormatException("PID/VID Miss match")
        }
        deviceVersion = usb!!.deviceVersion

        // give warning and continue on
        if (deviceVersion != dfuFile.BootVersion) {
            onStatusMsg(
                "Warning: Device BootVersion: " + Integer.toHexString(deviceVersion) +
                        "\tFile BootVersion: " + Integer.toHexString(dfuFile.BootVersion) + "\n"
            )
        }
        if (dfuFile.elementStartAddress != mInternalFlashStartAddress) { // todo: this will fail with images for other memory sections, other than Internal Flash
            throw FormatException("Firmware does not start at beginning of internal flash")
        }

        // todo: implement for stm32F411
//        if (deviceSizeLimit() < 0) {
//            throw new Exception("Error: Could Not Retrieve Internal Flash String");
//        }
        if (dfuFile.elementStartAddress + dfuFile.elementLength >= mInternalFlashStartAddress + mInternalFlashSize) {
            throw FormatException("Firmware image too large for target")
        }
        when (deviceVersion) {
            0x011A, 0x0200 -> dfuFile.maxBlockSize = 1024
            0x2100, 0x2200 -> dfuFile.maxBlockSize = 2048
            else -> throw Exception("Error: Unsupported bootloader version")
        }
        Log.i(TAG, "Firmware ok and compatible")
    }

    // todo this is limited to stm32f405RG and will fail for other future chips.
    private fun deviceSizeLimit(): Int {   // retrieves and compares the Internal Flash Memory Size  and compares to constant string
        val bmRequest = 0x80 // IN, standard request to usb device
        val bRequest = 0x06.toByte() // USB_REQ_GET_DESCRIPTOR
        val wLength = 127.toByte() // max string size
        val descriptor = ByteArray(wLength.toInt())

        /* This method can be used to retrieve any memory location size by incrementing the wValue in the defined range.
            ie. Size of: Internal Flash,  Option Bytes, OTP Size, and Feature location
         */
        val wValue = 0x0304 // possible strings range from 0x304-0x307
        val len = usb!!.controlTransfer(
            bmRequest,
            bRequest.toInt(),
            wValue,
            0,
            descriptor,
            wLength.toInt(),
            500
        )
        if (len < 0) {
            return -1
        }
        val decoded = String(descriptor, Charset.forName("UTF-16LE"))
        return if (decoded.contains(mInternalFlashString)) {
            mInternalFlashSize // size of stm32f405RG
        } else {
            -1
        }
    }

    @Throws(Exception::class)
    private fun writeBlock(address: Int, block: ByteArray, blockNumber: Int) {
        val dfuStatus = DfuStatus()
        do {
            clearStatus()
            getStatus(dfuStatus)
        } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
        if (0 == blockNumber) {
            setAddressPointer(address)
            getStatus(dfuStatus)
            getStatus(dfuStatus)
            if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
                throw Exception("Start address not supported")
            }
        }
        do {
            clearStatus()
            getStatus(dfuStatus)
        } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
        download(block, blockNumber + 2)
        getStatus(dfuStatus) // to execute
        if (dfuStatus.bState.toInt() != STATE_DFU_DOWNLOAD_BUSY) {
            throw Exception("error when downloading, was not busy ")
        }
        getStatus(dfuStatus) // to verify action
        if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
            throw Exception("error when downloading, did not perform action")
        }
        while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
            clearStatus()
            getStatus(dfuStatus)
        }
    }

    @Throws(Exception::class)
    private fun detach(Address: Int) {
        val dfuStatus = DfuStatus()
        getStatus(dfuStatus)
        while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
            clearStatus()
            getStatus(dfuStatus)
        }
        // Set the command pointer to the new application base address
        setAddressPointer(Address)
        getStatus(dfuStatus)
        while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
            clearStatus()
            getStatus(dfuStatus)
        }
        // Issue the DFU detach command
        leaveDfu()
        try {
            getStatus(dfuStatus)
            clearStatus()
            getStatus(dfuStatus)
        } catch (e: Exception) {
            // if caught, ignore since device might have disconnected already
        }
    }

    @get:Throws(Exception::class)
    private val isDeviceProtected: Boolean
        private get() {
            val dfuStatus = DfuStatus()
            var isProtected = false
            do {
                clearStatus()
                getStatus(dfuStatus)
            } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
            setAddressPointer(mInternalFlashStartAddress)
            getStatus(dfuStatus) // to execute
            getStatus(dfuStatus) // to verify
            if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
                isProtected = true
            }
            while (dfuStatus.bState.toInt() != STATE_DFU_IDLE) {
                clearStatus()
                getStatus(dfuStatus)
            }
            return isProtected
        }

    @Throws(Exception::class)
    fun writeOptionBytes(options: Int) {
        val dfuStatus = DfuStatus()
        do {
            clearStatus()
            getStatus(dfuStatus)
        } while (dfuStatus.bState.toInt() != STATE_DFU_IDLE)
        setAddressPointer(mOptionByteStartAddress)
        getStatus(dfuStatus)
        getStatus(dfuStatus)
        if (dfuStatus.bState.toInt() == STATE_DFU_ERROR) {
            throw Exception("Option Byte Start address not supported")
        }
        Log.i(TAG, "writing options: 0x" + Integer.toHexString(options))
        val buffer = ByteArray(2)
        buffer[0] = (options and 0xFF).toByte()
        buffer[1] = (options shr 8 and 0xFF).toByte()
        download(buffer)
        getStatus(dfuStatus) // device will reset
    }

    @Throws(Exception::class)
    private fun massEraseCommand() {
        val buffer = ByteArray(1)
        buffer[0] = 0x41
        download(buffer)
    }

    @Throws(Exception::class)
    private fun unProtectCommand() {
        val buffer = ByteArray(1)
        buffer[0] = 0x92.toByte()
        download(buffer)
    }

    @Throws(Exception::class)
    private fun setAddressPointer(Address: Int) {
        val buffer = ByteArray(5)
        buffer[0] = 0x21
        buffer[1] = (Address and 0xFF).toByte()
        buffer[2] = (Address shr 8 and 0xFF).toByte()
        buffer[3] = (Address shr 16 and 0xFF).toByte()
        buffer[4] = (Address shr 24 and 0xFF).toByte()
        download(buffer)
    }

    @Throws(Exception::class)
    private fun leaveDfu() {
        download(null)
    }

    @Throws(Exception::class)
    private fun getStatus(status: DfuStatus) {
        val buffer = ByteArray(6)
        val length = usb!!.controlTransfer(
            DFU_RequestType or USB_DIR_IN,
            DFU_GETSTATUS,
            0,
            0,
            buffer,
            6,
            500
        )
        if (length < 0) {
            throw Exception("USB Failed during getStatus")
        }
        status.bStatus = buffer[0] // state during request
        status.bState = buffer[4] // state after request
        status.bwPollTimeout = buffer[3].toInt() and 0xFF shl 16
        status.bwPollTimeout = status.bwPollTimeout or (buffer[2].toInt() and 0xFF shl 8)
        status.bwPollTimeout = status.bwPollTimeout or (buffer[1].toInt() and 0xFF)
    }

    @Throws(Exception::class)
    private fun clearStatus() {
        val length = usb!!.controlTransfer(DFU_RequestType, DFU_CLRSTATUS, 0, 0, null, 0, 0)
        if (length < 0) {
            throw Exception("USB Failed during clearStatus")
        }
    }

    // use for commands
    @Throws(Exception::class)
    private fun download(data: ByteArray?) {
        val len = usb!!.controlTransfer(DFU_RequestType, DFU_DNLOAD, 0, 0, data, data!!.size, 50)
        if (len < 0) {
            throw Exception("USB Failed during command download")
        }
    }

    // use for firmware download
    @Throws(Exception::class)
    private fun download(data: ByteArray, nBlock: Int) {
        val len = usb!!.controlTransfer(DFU_RequestType, DFU_DNLOAD, nBlock, 0, data, data.size, 0)
        if (len < 0) {
            throw Exception("USB failed during firmware download")
        }
    }

    @Throws(Exception::class)
    private fun upload(data: ByteArray, length: Int, blockNum: Int) {
        val len = usb!!.controlTransfer(
            DFU_RequestType or USB_DIR_IN,
            DFU_UPLOAD,
            blockNum,
            0,
            data,
            length,
            100
        )
        if (len < 0) {
            throw Exception("USB comm failed during upload")
        }
    }

    // stores the result of a GetStatus DFU request
    private inner class DfuStatus {
        var bStatus: Byte = 0 // state during request
        var bwPollTimeout = 0 // minimum time in ms before next getStatus call should be made
        var bState: Byte = 0 // state after request
    }

    // holds all essential information for the Dfu File
    private inner class DfuFile {
        var filePath: String? = null
        var file: ByteArray = byteArrayOf()
        var PID = 0
        var VID = 0
        var BootVersion = 0
        var maxBlockSize = 1024
        var elementStartAddress = 0
        var elementLength = 0
        var TargetName: String? = null
        var TargetSize = 0
        var NumElements = 0
    }

    init {
        dfuFile = DfuFile()
    }

    companion object {
        private const val TAG = "Dfu"
        private const val USB_DIR_OUT = 0
        private const val USB_DIR_IN = 128 //0x80
        private const val DFU_RequestType = 0x21 // '2' => Class request ; '1' => to interface
        private const val STATE_IDLE = 0x00
        private const val STATE_DETACH = 0x01
        private const val STATE_DFU_IDLE = 0x02
        private const val STATE_DFU_DOWNLOAD_SYNC = 0x03
        private const val STATE_DFU_DOWNLOAD_BUSY = 0x04
        private const val STATE_DFU_DOWNLOAD_IDLE = 0x05
        private const val STATE_DFU_MANIFEST_SYNC = 0x06
        private const val STATE_DFU_MANIFEST = 0x07
        private const val STATE_DFU_MANIFEST_WAIT_RESET = 0x08
        private const val STATE_DFU_UPLOAD_IDLE = 0x09
        private const val STATE_DFU_ERROR = 0x0A
        private const val STATE_DFU_UPLOAD_SYNC = 0x91
        private const val STATE_DFU_UPLOAD_BUSY = 0x92

        // DFU Commands, request ID code when using controlTransfers
        private const val DFU_DETACH = 0x00
        private const val DFU_DNLOAD = 0x01
        private const val DFU_UPLOAD = 0x02
        private const val DFU_GETSTATUS = 0x03
        private const val DFU_CLRSTATUS = 0x04
        private const val DFU_GETSTATE = 0x05
        private const val DFU_ABORT = 0x06
        const val ELEMENT1_OFFSET = 293 // constant offset in file array where image data starts
        const val TARGET_NAME_START = 22
        const val TARGET_NAME_MAX_END = 276
        const val TARGET_SIZE = 277
        const val TARGET_NUM_ELEMENTS = 281

        // Device specific parameters
        const val mInternalFlashString =
            "@Internal Flash  /0x08000000/04*016Kg,01*064Kg,07*128Kg" // STM32F405RG, 1MB Flash, 192KB SRAM
        const val mInternalFlashSize = 1048575
        const val mInternalFlashStartAddress = 0x08000000
        const val mOptionByteStartAddress = 0x1FFFC000
        private const val OPT_BOR_1 = 0x08
        private const val OPT_BOR_2 = 0x04
        private const val OPT_BOR_3 = 0x00
        private const val OPT_BOR_OFF = 0x0C
        private const val OPT_WDG_SW = 0x20
        private const val OPT_nRST_STOP = 0x40
        private const val OPT_nRST_STDBY = 0x80
        private const val OPT_RDP_OFF = 0xAA00
        private const val OPT_RDP_1 = 0x3300
        private fun calculateCRC(FileData: ByteArray): Int {
            var crc = -1
            for (i in 0 until FileData.size - 4) {
                crc = CRC_TABLE[crc xor FileData[i].toInt() and 0xff] xor (crc ushr 8)
            }
            return crc
        }

        private val CRC_TABLE = intArrayOf(
            0x00000000, 0x77073096, -0x11f19ed4, -0x66f6ae46, 0x076dc419, 0x706af48f,
            -0x169c5acb, -0x619b6a5d, 0x0edb8832, 0x79dcb8a4, -0x1f2a16e2, -0x682d2678,
            0x09b64c2b, 0x7eb17cbd, -0x1847d2f9, -0x6f40e26f, 0x1db71064, 0x6ab020f2,
            -0xc468eb8, -0x7b41be22, 0x1adad47d, 0x6ddde4eb, -0xb2b4aaf, -0x7c2c7a39,
            0x136c9856, 0x646ba8c0, -0x29d0686, -0x759a3614, 0x14015c4f, 0x63066cd9,
            -0x5f0c29d, -0x72f7f20b, 0x3b6e20c8, 0x4c69105e, -0x2a9fbe1c, -0x5d988e8e,
            0x3c03e4d1, 0x4b04d447, -0x2df27a03, -0x5af54a95, 0x35b5a8fa, 0x42b2986c,
            -0x2444362a, -0x534306c0, 0x32d86ce3, 0x45df5c75, -0x2329f231, -0x542ec2a7,
            0x26d930ac, 0x51de003a, -0x3728ae80, -0x402f9eea, 0x21b4f4b5, 0x56b3c423,
            -0x30456a67, -0x47425af1, 0x2802b89e, 0x5f058808, -0x39f3264e, -0x4ef416dc,
            0x2f6f7c87, 0x58684c11, -0x3e9ee255, -0x4999d2c3, 0x76dc4190, 0x01db7106,
            -0x672ddf44, -0x102aefd6, 0x71b18589, 0x06b6b51f, -0x60401b5b, -0x17472bcd,
            0x7807c9a2, 0x0f00f934, -0x69f65772, -0x1ef167e8, 0x7f6a0dbb, 0x086d3d2d,
            -0x6e9b9369, -0x199ca3ff, 0x6b6b51f4, 0x1c6c6162, -0x7a9acf28, -0xd9dffb2,
            0x6c0695ed, 0x1b01a57b, -0x7df70b3f, -0xaf03ba9, 0x65b0d9c6, 0x12b7e950,
            -0x74414716, -0x3467784, 0x62dd1ddf, 0x15da2d49, -0x732c830d, -0x42bb39b,
            0x4db26158, 0x3ab551ce, -0x5c43ff8c, -0x2b44cf1e, 0x4adfa541, 0x3dd895d7,
            -0x5b2e3b93, -0x2c290b05, 0x4369e96a, 0x346ed9fc, -0x529877ba, -0x259f4730,
            0x44042d73, 0x33031de5, -0x55f5b3a1, -0x22f28337, 0x5005713c, 0x270241aa,
            -0x41f4eff0, -0x36f3df7a, 0x5768b525, 0x206f85b3, -0x46992bf7, -0x319e1b61,
            0x5edef90e, 0x29d9c998, -0x4f2f67de, -0x3828574c, 0x59b33d17, 0x2eb40d81,
            -0x4842a3c5, -0x3f459353, -0x12477ce0, -0x65404c4a, 0x03b6e20c, 0x74b1d29a,
            -0x152ab8c7, -0x622d8851, 0x04db2615, 0x73dc1683, -0x1c9cf4ee, -0x6b9bc47c,
            0x0d6d6a3e, 0x7a6a5aa8, -0x1bf130f5, -0x6cf60063, 0x0a00ae27, 0x7d079eb1,
            -0xff06cbc, -0x78f75c2e, 0x1e01f268, 0x6906c2fe, -0x89da8a3, -0x7f9a9835,
            0x196c3671, 0x6e6b06e7, -0x12be48a, -0x762cd420, 0x10da7a5a, 0x67dd4acc,
            -0x6462091, -0x71411007, 0x17b7be43, 0x60b08ed5, -0x29295c18, -0x5e2e6c82,
            0x38d8c2c4, 0x4fdff252, -0x2e44980f, -0x5943a899, 0x3fb506dd, 0x48b2364b,
            -0x27f2d426, -0x50f5e4b4, 0x36034af6, 0x41047a60, -0x209f103d, -0x579820ab,
            0x316e8eef, 0x4669be79, -0x349e4c74, -0x43997ce6, 0x256fd2a0, 0x5268e236,
            -0x33f3886b, -0x44f4b8fd, 0x220216b9, 0x5505262f, -0x3a45c442, -0x4d42f4d8,
            0x2bb45a92, 0x5cb36a04, -0x3d280059, -0x4a2f30cf, 0x2cd99e8b, 0x5bdeae1d,
            -0x649b3d50, -0x139c0dda, 0x756aa39c, 0x026d930a, -0x63f6f957, -0x14f1c9c1,
            0x72076785, 0x05005713, -0x6a40b57e, -0x1d4785ec, 0x7bb12bae, 0x0cb61b38,
            -0x6d2d7165, -0x1a2a41f3, 0x7cdcefb7, 0x0bdbdf21, -0x792c2d2c, -0xe2b1dbe,
            0x68ddb3f8, 0x1fda836e, -0x7e41e933, -0x946d9a5, 0x6fb077e1, 0x18b74777,
            -0x77f7a51a, -0xf09590, 0x66063bca, 0x11010b5c, -0x709a6101, -0x79d5197,
            0x616bffd3, 0x166ccf45, -0x5ff51d88, -0x28f22d12, 0x4e048354, 0x3903b3c2,
            -0x5898d99f, -0x2f9fe909, 0x4969474d, 0x3e6e77db, -0x512e95b6, -0x2629a524,
            0x40df0b66, 0x37d83bf0, -0x564351ad, -0x2144613b, 0x47b2cf7f, 0x30b5ffe9,
            -0x42420de4, -0x35453d76, 0x53b39330, 0x24b4a3a6, -0x452fc9fb, -0x3228f96d,
            0x54de5729, 0x23d967bf, -0x4c9985d2, -0x3b9eb548, 0x5d681b02, 0x2a6f2b94,
            -0x4bf441c9, -0x3cf3715f, 0x5a05df1b, 0x2d02ef8d
        )
    }
}
