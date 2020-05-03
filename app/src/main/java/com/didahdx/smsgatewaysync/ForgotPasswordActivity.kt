package com.didahdx.smsgatewaysync

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.didahdx.smsgatewaysync.utilities.Validation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_forgot_password.*


class ForgotPasswordActivity : AppCompatActivity() , View.OnClickListener{

    val validation= Validation()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        btn_reset_password.setOnClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    //email validation
    private fun validateEmail(): Boolean {
        val email: String =
            edit__text_forgot_email.editText?.text.toString().trim()
        return if (email.isNotEmpty()) {
            if (validation.isValidEmailId(email)) {
                edit__text_forgot_email.error = null
                true
            } else {
                edit__text_forgot_email.error = "Invalid email address"
                false
            }
        } else {
            edit__text_forgot_email.error = "Field can not be empty"
            false
        }
    }

    override fun onClick(v: View?) {
        if(v==btn_reset_password){

            if (validateEmail()){
                sendResetPassowrdEmail()
            }
        }

    }

    private fun sendResetPassowrdEmail() {
        val email: String =
            edit__text_forgot_email.editText?.text.toString().trim()

            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this,
                            "Check your Email to Reset your password ", Toast.LENGTH_LONG)
                            .show()

                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Try again password not reset ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

}
