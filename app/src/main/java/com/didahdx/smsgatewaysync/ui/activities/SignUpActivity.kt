package com.didahdx.smsgatewaysync.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.UsersInfo
import com.didahdx.smsgatewaysync.utilities.Validation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity(), View.OnClickListener {
    val validation= Validation()
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        btn_sign_up.setOnClickListener(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        if(v==btn_sign_up){
            validateEmail()
            validateName()
            validatePhone()
            validatePassword()

            if(validateEmail()&& validateName()&& validatePhone()&& validatePassword()){
                signUpUser()
            }
        }
    }


    //email validation
    private fun validateEmail(): Boolean {
        val email: String =
            edit_text_email.editText?.text.toString().trim()
        return if (email.isNotEmpty()) {
            if (validation.isValidEmailId(email)) {
                edit_text_email.error = null
                true
            } else {
                edit_text_email.error = "Invalid email address"
                false
            }
        } else {
            edit_text_email.error = "Field can not be empty"
            false
        }
    }

    //name validation
    private fun validateName(): Boolean {
        val name: String =
            edit_text_name.editText?.text.toString().trim()
        return if (name.isEmpty()) {
            edit_text_name.error = "Field can not be empty"
            false
        } else {
            edit_text_name.error = null
            true
        }
    }

    //phone validation
    private fun validatePhone(): Boolean {
        val phone: String =
            edit_text_phoneNumber.editText?.text.toString().trim()
        return if (phone.isEmpty()) {
            edit_text_phoneNumber.error = "Field can not be empty"
            false
        } else {
            edit_text_phoneNumber.error = null
            true
        }
    }

    //validate password
    private fun validatePassword(): Boolean {
        val password: String =
            edit_text_password_sign.editText?.text.toString().trim()
        val confirmPassword: String =
            edit_text_confirm_password_sign.editText?.text
                .toString().trim()
        if (password.isEmpty()) {
            edit_text_password_sign.error = "Field can not be empty"
        } else {
            edit_text_password_sign.error = null
        }
        if (confirmPassword.isEmpty()) {
            edit_text_confirm_password_sign.error = "Field can not be empty"
        } else {
            edit_text_confirm_password_sign.error = null
        }
        return if (password.isNotEmpty() && confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                edit_text_confirm_password_sign.error = null
                edit_text_password_sign.error = null
                true
            } else {
                edit_text_confirm_password_sign.error = "Passwords do not match"
                edit_text_password_sign.error = "Passwords do not match"
                false
            }
        } else {
            false
        }
    }

    private fun signUpUser() {
        val email: String =
            edit_text_email.editText?.text.toString().trim()
        val password: String =
            edit_text_password_sign.editText?.text.toString().trim()
        val name: String =
            edit_text_name.editText?.text.toString().trim()
        val phone: String =
            edit_text_phoneNumber.editText?.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(
                        this@SignUpActivity,
                        "Sign Up Success", Toast.LENGTH_SHORT
                    ).show()
                    val user: FirebaseUser? = auth.currentUser
                    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
                    var myRef: DatabaseReference? = null
                    if (user != null) {
                        myRef = database
                            .getReference("users")
                            .child(user.uid)
                        val info = UsersInfo(name, phone, email)
                        myRef.setValue(info)
                    }
                    finish()
                } else { // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this@SignUpActivity,
                        "Authentication failed. " + task.exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
    }

}