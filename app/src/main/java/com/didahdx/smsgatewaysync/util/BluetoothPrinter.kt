package com.didahdx.smsgatewaysync.util

import android.content.Context
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.printerlib.IPrintToPrinter
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.printerlib.printerWordMng
import com.didahdx.smsgatewaysync.printerlib.utils.printerFactory
import com.woosim.printer.WoosimCmd

class BluetoothPrinter(private val context: Context,message:String) : IPrintToPrinter {
    val message=message
    override fun printContent(prnMng: WoosimPrnMng) {
        val wordMng: printerWordMng = printerFactory.createPaperMng(context)
        prnMng.printStr(message, 1, WoosimCmd.ALIGN_CENTER)
//        prnMng.printStr("1-First line", 1, WoosimCmd.ALIGN_LEFT)
//        prnMng.printStr(wordMng.horizontalUnderline)
//        prnMng.printStr(
//            wordMng.autoWordWrap("2-Second line that is very very long line to check word wrap"),
//            1, WoosimCmd.ALIGN_LEFT
//        )
//        prnMng.printStr(wordMng.horizontalUnderline)
//        prnMng.printStr("3-Third line", 1, WoosimCmd.ALIGN_LEFT)
        prnMng.printNewLine()
//        prnMng.printStr("Footer", 1, WoosimCmd.ALIGN_CENTER)

        //You can also print a logo
        //prnMng.printBitmap("/sdcard/test/001.png", IBixolonCanvasConst.CMaxChars_2Inch);

        //Any finalization, you can call it here or any where in your running activity.
        printEnded(prnMng)
    }

    override fun printEnded(prnMng: WoosimPrnMng) {
        //Do any finalization you like after print ended.
        if (prnMng.printSucc()) {
            context.toast(R.string.print_succ)
        } else {
            context.toast( R.string.print_error)
        }
    }

}
