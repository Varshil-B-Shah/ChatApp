# ChatApp - Modern Android Messaging Application

ChatApp is a robust, real-time messaging application for Android, built with Kotlin. It features a unique hybrid backend architecture that leverages the best of both Firebase and Appwrite to deliver a seamless and scalable chat experience.

## 🚀 Features

*   **Secure Authentication**: User sign-up and sign-in powered by Firebase Authentication.
*   **Real-time Messaging**: Instant text messaging stored and synchronized via Firebase Firestore.
*   **Media Sharing**: Send and receive images in chat, with efficient storage managed by Appwrite.
*   **User Profiles**: Customizable user profiles with profile pictures.
*   **Modern UI**: Clean and responsive detailed user interface with smooth animations and transitions.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **Platform**: Android SDK
*   **Authentication**: Firebase Auth
*   **Database**: Firebase Firestore
*   **Storage**: Appwrite Storage
*   **Image Loading**: Glide, CircularImageView
*   **Architecture Components**: ViewBinding, DataBinding, Coroutines
*   **Build System**: Gradle

## ⚙️ Setup & Configuration

To run this project locally, you will need to configure both Firebase and Appwrite.

### Prerequisites

*   Android Studio Ladybug or newer.
*   Java JDK 11+.

### Firebase Setup

1.  Create a project in the [Firebase Console](https://console.firebase.google.com/).
2.  Enable **Authentication** (Email/Password provider).
3.  Enable **Firestore Database**.
4.  Download the `google-services.json` file and place it in the `app/` directory.

### Appwrite Setup

1.  Set up an [Appwrite](https://appwrite.io/) instance (local or cloud).
2.  Create a new project and a storage bucket.
3.  Update the `build.gradle.kts` (Level: app) file with your Appwrite credentials:
    ```kotlin
    buildConfigField("String", "APPWRITE_ENDPOINT", "\"YOUR_ENDPOINT\"")
    buildConfigField("String", "APPWRITE_PROJECT_ID", "\"YOUR_PROJECT_ID\"")
    buildConfigField("String", "APPWRITE_BUCKET_ID", "\"YOUR_BUCKET_ID\"")
    ```

## 📸 Usage

1.  Launch the app on an emulator or physical device.
2.  Register a new account or sign in.
3.  Set up your profile picture.
4.  Start chatting!

