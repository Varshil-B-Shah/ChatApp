package com.example.chatapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val STORAGE_REQUEST_CODE = 23432
    private lateinit var uri: Uri
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("Users")
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
            startNextAnimation()
        }

        mBinding.textViewSignIn.setOnClickListener {
            startPreviousAnimation()
        }

        mBinding.textViewGoToProfile.setOnClickListener {
            startNextAnimation()
        }

        mBinding.textViewSignUp.setOnClickListener {
            startPreviousAnimation()
        }

        mBinding.profileImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                } else {
                    getImage()
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        STORAGE_REQUEST_CODE
                    )
                } else {
                    getImage()
                }
            }
        }

        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                uri = it.data?.data!!

                mBinding.profileImage.setImageURI(uri)
            }
        }

        AppwriteClient.init(applicationContext)
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
        val username = mBinding.signUpInputUsername.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(
                this,
                "You have to provide an email and a password to sign-in",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (username.isEmpty()) {
            Toast.makeText(
                this,
                "You should provide an username",
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

        if (password.length <= 6) {
            Toast.makeText(
                this,
                "Passwords should have at least 6 characters",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account Created", Toast.LENGTH_LONG).show()
                    if (this::uri.isInitialized) {
                        uploadProfileImage(username)
                    }
                } else {
                    Toast.makeText(this, "The account wasn't created", Toast.LENGTH_LONG).show()
                }
            }

    }

    private fun startNextAnimation() {
        mBinding.flipper.setInAnimation(this, android.R.anim.slide_in_left)
        mBinding.flipper.setOutAnimation(this, android.R.anim.slide_out_right)
        mBinding.flipper.showNext()
    }

    private fun startPreviousAnimation() {
        mBinding.flipper.setInAnimation(this, R.anim.slide_in_right)
        mBinding.flipper.setOutAnimation(this, R.anim.slide_out_left)
        mBinding.flipper.showPrevious()
    }

    private fun getImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getResult.launch(intent)
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            STORAGE_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getImage()
        } else {
            Toast.makeText(this@MainActivity, "Permission not granted", Toast.LENGTH_LONG).show()
        }
    }
    private fun uploadProfileImage(username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream from URI")

                val bytes = inputStream.use { it.readBytes() }

                val ext = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("webp") -> "webp"
                    else -> "jpg"
                }
                val filename = "profile_${FirebaseAuth.getInstance().currentUser!!.uid}.$ext"

                val file = InputFile.fromBytes(bytes = bytes, filename = filename, mimeType = mimeType)

                val response = AppwriteClient.storage.createFile(
                    bucketId = BuildConfig.APPWRITE_BUCKET_ID,
                    fileId = ID.unique(),
                    file = file
                )

                val fileId = response.id

                runOnUiThread {
                    saveUser(username, fileId)
                    Toast.makeText(this@MainActivity, "Upload successful", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Upload failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun saveUser(username: String, fileId: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val user = User(username, fileId, uid)
        usersRef.document(uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Account wasn't created", Toast.LENGTH_LONG).show()
            }
    }
}