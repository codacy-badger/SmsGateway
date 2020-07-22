package com.didahdx.smsgatewaysync.presentation.authetication

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.Validation
import com.didahdx.smsgatewaysync.utilities.toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_forgot_password.*


/**
 * A simple [Fragment] subclass.
 */
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password), View.OnClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_reset_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == btn_reset_password) {

            if (validateEmail()) {
                sendResetPassowrdEmail()
            }
        }

    }


    //email validation
    private fun validateEmail(): Boolean {
        val email: String =
            edit__text_forgot_email.editText?.text.toString().trim()
        return if (email.isNotEmpty()) {
            if (Validation.isValidEmailId(email)) {
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

    private fun sendResetPassowrdEmail() {
        val email: String =
            edit__text_forgot_email.editText?.text.toString().trim()

        FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    requireContext().toast("Check your Email to Reset your password ")
                    activity?.onBackPressed()

                } else {
                    requireContext().toast("Try again password not reset ")
                }
            }
    }
}
