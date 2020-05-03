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

fun ProgressBar.show(){
    visibility= View.VISIBLE
}

fun ProgressBar.hide(){
    visibility= View.GONE
}

fun TextView.show(){
    visibility= View.VISIBLE
}

fun TextView.hide(){
    visibility= View.GONE
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