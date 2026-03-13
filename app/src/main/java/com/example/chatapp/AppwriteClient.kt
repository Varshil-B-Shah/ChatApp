package com.example.chatapp

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteClient {

    lateinit var client: Client
    lateinit var storage: Storage

    fun init(context: Context) {

        client = Client(context)
            .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
            .setProject(BuildConfig.APPWRITE_PROJECT_ID)

        storage = Storage(client)
    }
}