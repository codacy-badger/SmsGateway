package com.didahdx.smsgatewaysync.utilities

import android.os.Environment
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Date


class printMessage {
    lateinit var pdfFile: File

    fun createPdf(message:String): File{
        val directoryFolder=File("${Environment.getExternalStorageDirectory()}/SmsGatewaySync")

        if (!directoryFolder.exists()){
            directoryFolder.mkdirs()
        }

        val pattern = "\\s+".toRegex()
        val pdfName= "${Date().toString()
            .replace(":","_")
            .replace("+","_")
            .replace(pattern,"_")}.pdf"
        pdfFile=File(directoryFolder.absolutePath,pdfName)
        val outputStream=FileOutputStream(pdfFile)
        val document=Document(PageSize.A4)
        val table= PdfPTable(1)
        table.defaultCell.horizontalAlignment=Element.ALIGN_CENTER
        table.defaultCell.fixedHeight=250.0f
        table.totalWidth=PageSize.A4.width/4
        table.widthPercentage=100.0f
        table.defaultCell.verticalAlignment=Element.ALIGN_MIDDLE

        table.addCell(message)

        PdfWriter.getInstance(document,outputStream)
        document.open()
        document.add(table)
        document.close()

        return pdfFile
    }


}