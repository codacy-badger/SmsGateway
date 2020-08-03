package com.didahdx.smsgatewaysync.util

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.didahdx.smsgatewaysync.R
import com.google.android.material.snackbar.Snackbar


fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}


fun Context.toast(message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.show(): View {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
    return this
}

fun View.hide(): View {
    if (visibility != View.GONE) {
        visibility = View.GONE
    }
    return this
}

fun View.backgroundRed() {
    background =
        resources.getDrawable(R.drawable.item_background_red, null)
}

fun View.backgroundGreen() {
    background =
        resources.getDrawable(R.drawable.item_background_green, null)
}

fun View.backgroundGrey() {
    background =
        resources.getDrawable(R.color.grey, null)
}

//used to display alert dialog box
fun Context.showDialog(
    title: String, msg: String, postiveLabel: String,
    postiveOnClick: DialogInterface.OnClickListener,
    negativeLabel: String, negativeOnClick: DialogInterface.OnClickListener,
    isCancelable: Boolean
): AlertDialog {

    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setCancelable(isCancelable)
    builder.setMessage(msg)
    builder.setPositiveButton(postiveLabel, postiveOnClick)
    builder.setNegativeButton(negativeLabel, negativeOnClick)
    val alert = builder.create()
    alert.show()
    return alert
}


fun View.snackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).also { snackBar ->
        snackBar.setAction("Ok") {
            snackBar.dismiss()
        }
    }.show()

}