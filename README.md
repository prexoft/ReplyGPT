# AI smart notification responder

An AI-powered Android notification responder built using Kotlin, Android Studio, and the Gemini API.

This app listens to incoming Android notifications, processes their content in real time, and generates intelligent AI-based replies dynamically.

## Features

* Real-time notification monitoring
* Android Notification Listener Service integration
* AI-generated smart replies using Gemini API
* Dynamic text generation
* Modern Android UI built with Kotlin
* Background service handling
* Notification permission management
* Clean and beginner-friendly project structure

## Tech Stack

* Kotlin
* Android Studio
* Gemini API
* Android SDK
* Notification Listener Service

## How It Works

1. The app requests notification access permission.
2. Android Notification Listener Service captures incoming notifications.
3. Notification content is extracted and processed.
4. The text is sent to the Gemini API.
5. Gemini generates an intelligent contextual response.
6. The generated reply is sent to user using RemoteInput.

## What You’ll Learn

This project is useful for learning:

* Android app development with Kotlin
* Creating modern Android UI
* Working with Android permissions
* Using Notification Listener Services
* Handling real-time notification events
* Integrating AI APIs into Android apps
* Generating dynamic AI-powered text content
* Structuring AI-based Android applications

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/prexoft/ReplyGPT.git
```

### 2. Open in Android Studio

* Open Android Studio
* Select “Open Project”
* Choose the cloned repository

### 3. Add Gemini API Key

Create a local configuration file or add your API key securely:

```kotlin
const val GEMINI_API_KEY = "YOUR_API_KEY"
```

Never expose your real API key publicly.

### 4. Enable Notification Access

On your Android device:

* Open Settings
* Go to Notification Access
* Enable permission for this app

### 5. Run the App

Connect your Android device or emulator and run the project from Android Studio.

## Future Improvements

* Support for multiple AI providers
* Voice response generation
* Reply customization settings
* On-device AI support

## Disclaimer

This project is for educational purposes only. Be careful when handling user notifications and sensitive data.

## Video Tutorial

Check out the full tutorial on YouTube:
[Tutorial](https://youtu.be/ECn4JTt_Vr8)

## License

MIT License
