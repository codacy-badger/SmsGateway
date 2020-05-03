package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.didahdx.smsgatewaysync.R
import com.google.android.material.snackbar.Snackbar


fun Context.toast(message:String){
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
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

fun TextView.backgroundRed(){
    background =
        resources.getDrawable(R.drawable.item_background_red)
}

fun TextView.backgroundGreen(){
    background =
        resources.getDrawable(R.drawable.item_background_green)
}

fun View.snackbar(message: String){
    Snackbar.make(this,message,Snackbar.LENGTH_LONG).also {
        snackbar -> snackbar.setAction("Ok"){
        snackbar.dismiss()
    }
    }.show()
}