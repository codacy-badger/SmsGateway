package com.didahdx.smsgatewaysync.utilities

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

object BarcodeGenerator {

    fun getBarcode(text: String): Bitmap? {
        var bitmap: Bitmap?=null
        val multiFormatWriter = MultiFormatWriter();
        try {
            val bitMatrix: BitMatrix =
                multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            val barcodeEncoder = BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);

        } catch (e: WriterException) {
            e.printStackTrace();
        }

        return bitmap
    }

}