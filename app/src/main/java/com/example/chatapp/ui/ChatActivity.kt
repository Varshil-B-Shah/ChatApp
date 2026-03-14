package com.example.chatapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.AppwriteClient
import com.example.chatapp.BuildConfig
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.adaptor.MessagesAdaptor
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("Users")
    private val messagesRef: CollectionReference = db.collection("Messages")
    private lateinit var sendButton: Button
    private lateinit var editTextMessage: EditText
    private lateinit var messageAdaptor: MessagesAdaptor
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messages: MutableList<ChatMessage>
    private lateinit var currentUser: User
    private lateinit var uri: Uri
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val storageRequestCode = 78978

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        messagesRecyclerView = findViewById(R.id.message_recycler_view)
        sendButton = findViewById(R.id.send_message_button)
        editTextMessage = findViewById(R.id.input_message)

        initRecyclerView()
        getCurrentUser()

        sendButton.setOnClickListener {
            insertMessage()
            editTextMessage.text = null
        }

        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                uri = it.data?.data!!
                uploadImageAndSend()
            }
        }

        editTextMessage.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editTextMessage.right -
                            editTextMessage.compoundDrawables[DRAWABLE_RIGHT].bounds.width())
                ) {
                    editTextMessage.setText("")
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
                            requestPermission()
                        } else {
                            getImage()
                        }
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storageRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getImage()
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()

        messagesRef.orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                error?.let {
                    return@addSnapshotListener
                }

                snapshots?.let {
                    for (dc in it.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val message = dc.document.toObject(ChatMessage::class.java)
                                messageAdaptor.addMessage(message)  // replaces messages.add() + notifyItemInserted()
                                messagesRecyclerView.smoothScrollToPosition(messageAdaptor.itemCount - 1)
                            }

                            DocumentChange.Type.REMOVED -> {

                            }

                            DocumentChange.Type.MODIFIED -> {

                            }
                        }
                    }
                }
            }
    }

    private fun initRecyclerView() {
        messages = mutableListOf()
        messageAdaptor = MessagesAdaptor(this, messages)
        messagesRecyclerView.adapter = messageAdaptor
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.setHasFixedSize(true)
    }

    private fun getCurrentUser() {
        usersRef.whereEqualTo("id", FirebaseAuth.getInstance().currentUser?.uid)
            .get()
            .addOnSuccessListener {
                for (snapshot in it) {
                    currentUser = snapshot.toObject(User::class.java)
                }
            }
    }

    private fun insertMessage(imageId: String? = null) {
        val messageText = editTextMessage.text.toString()

        if (messageText.isEmpty() && imageId == null) return

        messagesRef.document()
            .set(ChatMessage(currentUser, messageText, imageId))

        editTextMessage.text = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                Intent(this@ChatActivity, MainActivity::class.java).also {
                    startActivity(it)
                }
                return true
            }
        }
        return false
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
            storageRequestCode
        )
    }

    private fun getImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getResult.launch(intent)
    }

    private fun uploadImageAndSend() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream")
                val bytes = inputStream.use { it.readBytes() }

                val ext = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("webp") -> "webp"
                    else -> "jpg"
                }
                val filename = "chat_img_${System.currentTimeMillis()}.$ext"
                val file =
                    InputFile.fromBytes(bytes = bytes, filename = filename, mimeType = mimeType)

                val response = AppwriteClient.storage.createFile(
                    bucketId = BuildConfig.APPWRITE_BUCKET_ID,
                    fileId = ID.unique(),
                    file = file
                )

                runOnUiThread {
                    insertMessage(imageId = response.id)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Image upload failed: ${e.stackTrace}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

}