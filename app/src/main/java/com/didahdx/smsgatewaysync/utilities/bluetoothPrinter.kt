package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.widget.Toast
import com.didahdx.smsgatewaysync.R
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.RawPrintable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.data.printer.DefaultPrinter
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback

class bluetoothPrinter : PrintingCallback {
    var context:Context?=null
    var printing: Printing? = null


    fun printText(message: String,context : Context, appName:String) {
        this.context=context
        initPrinter()
        val printables = ArrayList<Printable>()
        printables.add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

        //print header
        printables.add(
            TextPrintable.Builder()
                .setText(appName)
                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
                .setAlignment(DefaultPrinter.ALIGNMENT_LEFT)
                .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
                .setNewLinesAfter(1)
                .build()
        )

        //print body
        printables.add(
            TextPrintable.Builder()
                .setText(message)
                .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
                .setAlignment(DefaultPrinter.ALIGNMENT_LEFT)
                .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
                .build()
        )


        printing?.print(printables)
    }

    private fun initPrinter() {
        if (printing != null) {
            printing?.printingCallback = this
        }

        if (Printooth.hasPairedPrinter()){
            printing= Printooth.printer()
        }else{
            Toast.makeText(context,"Connect to printer",Toast.LENGTH_LONG).show()
        }

        if (printing!=null){
            printing?.printingCallback=this
        }
    }






    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
        Toast.makeText(context,"Connecting to printer", Toast.LENGTH_SHORT).show()
    }

    override fun connectionFailed(error: String) {
        Toast.makeText(context,"Connecting to printer failed $error", Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: String) {
        Toast.makeText(context,"Error $error", Toast.LENGTH_SHORT).show()
    }

    override fun onMessage(message: String) {
        Toast.makeText(context,"Message $message", Toast.LENGTH_SHORT).show()
    }

    override fun printingOrderSentSuccessfully() {
        Toast.makeText(context,"Order sent to printer", Toast.LENGTH_SHORT).show()
    }

    /***************************************************************************************************************************/

}