package com.didahdx.smsgatewaysync.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.Validation
import com.didahdx.smsgatewaysync.utilities.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 */
class LoginFragment : Fragment(R.layout.fragment_login), View.OnClickListener {
    private val validation = Validation()
    lateinit var auth: FirebaseAuth
    lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        navController = Navigation.findNavController(view)
        text_view_sign_up.setOnClickListener(this)
        text_view_forgot_password.setOnClickListener(this)
        button_login.setOnClickListener(this)
    }


    override fun onClick(v: View?) {
        if (v == text_view_sign_up) {
            navController.navigate(R.id.action_loginFragment_to_signUpFragment)
        } else if (v == text_view_forgot_password) {
            navController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        } else if (v == button_login) {

            if (validateEmail() && validatePassword()) {
                loginUser()
//                checkSmsPermission()
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
//                    == PackageManager.PERMISSION_GRANTED
//                ) {
//
//                }
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
            navController?.navigate(R.id.action_loginFragment_to_mainActivity)
            requireActivity().onBackPressed()

        }

    }

    private fun loginUser() {
        val email: String = edit_text_sign_up_email.editText?.text.toString().trim()
        val password: String = edit_text_sign_up_password.editText?.text.toString().trim()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity as Activity
        ) { task ->  if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
            requireContext().toast("signInWithEmail:success")
            val user: FirebaseUser? = auth.currentUser
            if (user != null) {
                navController?.navigate(R.id.action_loginFragment_to_mainActivity)
                requireActivity().onBackPressed()
            }
        } else { // If sign in fails, display a message to the user.
            requireContext().toast("Authentication failed. ${task.exception}")
        }

        }


    }

}
