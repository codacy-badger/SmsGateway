package com.didahdx.smsgatewaysync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.didahdx.smsgatewaysync.utilities.Validation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val validation = Validation()
    lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        text_view_sign_up.setOnClickListener(this)
        text_view_forgot_password.setOnClickListener(this)
        button_login.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        if (v == text_view_sign_up) {
            startActivity(Intent(this, SignUpActivity::class.java))
        } else if (v == text_view_forgot_password) {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        } else if (v == button_login) {

            if (validateEmail() && validatePassword()) {
                loginUser()
            }

        }
    }


    private fun validatePassword(): Boolean {
        val email: String =
            edit_text_sign_up_password.editText?.text.toString().trim()
        return if (email.isNotEmpty()) {
            true
        } else {
            edit_text_sign_up_email.error = "Field can not be empty"
            false
        }

    }

    //email validation
    private fun validateEmail(): Boolean {
        val email: String =
            edit_text_sign_up_email.editText?.text.toString().trim()
        return if (email.isNotEmpty()) {
            if (validation.isValidEmailId(email)) {
                edit_text_sign_up_email.error = null
                true
            } else {
                edit_text_sign_up_email.error = "Invalid email address"
                false
            }
        } else {
            edit_text_sign_up_email.error = "Field can not be empty"
            false
        }
    }

    override fun onStart() {
        super.onStart()

        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

    }

    private fun loginUser() {
        val email: String =
            edit_text_sign_up_email.editText?.text.toString().trim()
        val password: String =
            edit_text_sign_up_password.editText?.text.toString().trim()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this
            ) { task ->
                if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(
                        this,
                        "signInWithEmail:success", Toast.LENGTH_SHORT
                    ).show()
                    val user: FirebaseUser? = auth.currentUser
                    if (user != null) {
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    }
                } else { // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this, "Authentication failed. "
                                + task.exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}
