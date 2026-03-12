package com.example.chatapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mBinding.signInButton.setOnClickListener {
            signIn()
        }

        mBinding.signUpButton.setOnClickListener {
            createAccount()
        }

        mBinding.textViewRegister.setOnClickListener {
            mBinding.flipper.setInAnimation(this, android.R.anim.slide_in_left)
            mBinding.flipper.setOutAnimation(this, android.R.anim.slide_out_right)
            mBinding.flipper.showNext()
        }

        mBinding.textViewSignIn.setOnClickListener {
            mBinding.flipper.setInAnimation(this, R.anim.slide_in_right)
            mBinding.flipper.setOutAnimation(this, R.anim.slide_out_left)
            mBinding.flipper.showPrevious()
        }
    }

    private fun signIn() {
        val email = mBinding.signInInputEmail.editText?.text.toString().trim()
        val password = mBinding.singInInputPassword.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                "You have to provide an email and a password to sign-in",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "User Signed in successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this,
                        "Couldn't sign in \nSomething went wrong ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun createAccount() {
        val email = mBinding.signUpInputEmail.text.toString().trim()
        val password = mBinding.signUpInputPassword.text.toString().trim()
        val confirmPassword = mBinding.signUpInputConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(
                this,
                "You have to provide an email and a password to sign-in",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(
                this,
                "Passwords don't match",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account Created", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "The account wasn't created", Toast.LENGTH_LONG).show()
                }
            }
    }
}