package com.didahdx.smsgatewaysync.printerlib.utils

import android.content.Context
import com.didahdx.smsgatewaysync.utilities.SpUtil
import com.woosim.printer.WoosimCmd

object PrefMng {
    private const val PREF_DEV_ADDR = "PrefMng.PREF_DEVADDR"
    private const val PREF_PRINTER = "PrefMng.PREF_PRINTER"
    const val PRN_WOOSIM_SELECTED = 1
    const val PRN_BIXOLON_SELECTED = 2
    const val PRN_OTHER_PRINTERS_SELECTED = 3
    const val PRN_RONGTA_SELECTED = 4

    @JvmStatic
    fun getActivePrinter(context: Context): Int {
        return SpUtil.getPreferenceInt(context, PREF_PRINTER, PRN_WOOSIM_SELECTED)
    }

    fun saveActivePrinter(context: Context, printerName: Int) {
        SpUtil.setPreferenceInt(context,PREF_PRINTER, printerName)
    }

    @JvmStatic
    fun saveDeviceAddr(context: Context, newAddr: String) {
        SpUtil.setPreferenceString(context,PREF_DEV_ADDR, newAddr)
    }

    /**
     * You can use the getDeviceAddr method to bypass DeviceListActivity.
     * @param context
     * @return If the return value is an empty string, it means no printer Bluetooth
     * address already is saved. In this case you MUST first run DeviceListActivity.
     * If the return value is not empty then you can bypass loading DeviceListActivity.
     * The best place to save the Bluetooth address is in the IPrintToPrinter.printEnded
     * method when the print operation is ended successfully.
     */
    fun getDeviceAddr(context: Context): String {
        return SpUtil.getPreferenceString(context,PREF_DEV_ADDR, "")
    }

    @JvmStatic
    fun getBoldPrinting(contx: Context?): Boolean {
        return false
    }/*Based on the installed font on the device you can return
          WoosimCmd.CT_IRAN_SYSTEM or other code tables.*/ //It also supports English.

    /**
     * This method is specific to the Woosim printers only. In other words,
     * you can choose which font to use on the Woosim printers.
     * @return The code table for printing.
     */
    @JvmStatic
    val woosimCodeTbl: Int
        get() =/*Based on the installed font on the device you can return
              WoosimCmd.CT_IRAN_SYSTEM or other code tables.*/
            WoosimCmd.CT_ARABIC_FARSI //It also supports English.
}