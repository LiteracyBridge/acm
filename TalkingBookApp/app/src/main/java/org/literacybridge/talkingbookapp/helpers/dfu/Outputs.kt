package org.literacybridge.talkingbookapp.helpers.dfu

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Philip on 26/05/2015.
 *
 * This class assume that the user has already created the gpio pins and set their direction
 * inside of the init.sun7i.rc script file.
 * The class must have 'other' write permission to access the gpio 'value' parameter
 */
object Outputs {
    private const val TAG = "Umbrela Outputs"
    private const val bootValuePath = "/sys/class/gpio_sw/PD1/data"
    private const val resetValuePath = "/sys/class/gpio_sw/PD0/data"
    private val outBoot = File(bootValuePath)
    private val outReset = File(resetValuePath)
    fun enterDfuMode() {
        try {
            setReset()
            setBoot()
            clearReset()
            Log.i(TAG, "entered DFU mode successful")
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
            // e.printStackTrace();
        }
    }

    fun enterNormalMode() {
        try {
            setReset()
            clearReset()
            clearBoot()
            Log.i(TAG, "entered Normal mode successful")
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
            // e.printStackTrace();
        }
    }

    fun leaveDfuMode() {
        try {
            setReset()
            clearBoot()
            clearReset()
            Log.i(TAG, "exited DFU mode successful")
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
            // e.printStackTrace();
        }
    }

    @Throws(IOException::class)
    private fun setReset() {
        val stream = FileOutputStream(outReset)
        stream.write('0'.code) // this is active-low
        stream.close()
    }

    @Throws(IOException::class)
    private fun clearReset() {
        val stream = FileOutputStream(outReset)
        stream.write('1'.code)
        stream.close()
    }

    @Throws(IOException::class)
    private fun setBoot() {
        val stream = FileOutputStream(outBoot)
        stream.write('1'.code) // this is active-high
        stream.close()
    }

    @Throws(IOException::class)
    private fun clearBoot() {
        val stream = FileOutputStream(outBoot)
        stream.write('0'.code)
        stream.close()
    }
}
