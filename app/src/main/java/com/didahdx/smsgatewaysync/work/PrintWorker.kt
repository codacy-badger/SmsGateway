package com.didahdx.smsgatewaysync.work

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.utilities.KEY_TASK_PRINT
import com.didahdx.smsgatewaysync.utilities.PREF_PRINTER_NAME
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeoutException

class PrintWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    val context=appContext
    var bluetoothAdapter: BluetoothAdapter? = null
    var socket: BluetoothSocket? = null
    var bluetoothDevice: BluetoothDevice? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null
    var workerThread: Thread? = null
    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0
    @Volatile
    var stopWorker = false
    var value = ""
    private lateinit var sharedPreferences: SharedPreferences

    override suspend fun doWork(): Result {
        try {
            val data = inputData
            val message = data.getString(KEY_TASK_PRINT)
            if (message != null) {
                intentPrint(message)
            }
        } catch (e: HttpException) {
            e.printStackTrace()
            return Result.retry()
        } catch (e: TimeoutException) {
            e.printStackTrace()
            return Result.retry()
        }catch (e: Exception) {
            context.toast("Worker $e ${e.localizedMessage}")
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }

    suspend fun intentPrint(messageBody: String) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val buffer: ByteArray = messageBody.toByteArray()
        val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
        printHeader[3] = buffer.size.toByte()
        InitPrinter()
        if (printHeader.size > 128) {
            value = "\nValue is more than 128 size\n"
            CoroutineScope(Dispatchers.Main).launch {
                context.toast(value)
            }
        } else {
            try {
                outputStream?.write(messageBody.toByteArray())
                outputStream?.close()
                socket?.close()
            } catch (ex: java.lang.Exception) {
                value = "$ex\nExcep IntentPrint \n"
                CoroutineScope(Dispatchers.Main).launch {
                    context.toast(value)
                }
            }
        }
    }

    private suspend fun InitPrinter() {
       val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() as BluetoothAdapter
        try {
//            if (!bluetoothAdapter?.isEnabled!!) {
//                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(enableBluetooth, 0)
//            }
            val printerName = sharedPreferences.getString(PREF_PRINTER_NAME, "")
            if (!(printerName != null && printerName.isNotEmpty())) {
                return
            }
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices?.size!! > 0) {
                for (device in pairedDevices) {
                    if (device.name == printerName) //Note, you will need to change this to match the name of your device
                    {
                        bluetoothDevice = device
                        break
                    }
                }
                val uuid =
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID
                val m: Method = bluetoothDevice!!.javaClass.getMethod(
                    "createRfcommSocket", *arrayOf<Class<*>?>(
                        Int::class.javaPrimitiveType
                    )
                )
                socket = m?.invoke(bluetoothDevice, 1) as BluetoothSocket?
                bluetoothAdapter?.cancelDiscovery()
                socket?.connect()
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream
                beginListenForData()
            } else {
                value = "No Devices found"
                CoroutineScope(Dispatchers.Main).launch {
                    context.toast(value)
                }
                return
            }
        } catch (ex: java.lang.Exception) {
            value = "$ex\n InitPrinter \n"
            CoroutineScope(Dispatchers.Main).launch {
               context.toast(value)
            }
        }

    }


    private suspend fun beginListenForData() {
        try {
            val handler = Handler()

            // this is the ASCII code for a newline character
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)
            workerThread = Thread(Runnable {
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable = inputStream?.available()
                        if (bytesAvailable != null && bytesAvailable > 0) {
                            val packetBytes = ByteArray(bytesAvailable)
                            inputStream!!.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.size
                                    )

                                    // specify US-ASCII encoding
                                    val data = String(encodedBytes, Charsets.US_ASCII)
                                    readBufferPosition = 0

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post(Runnable { Timber.d(data) })
                                } else {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            })
            workerThread?.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}