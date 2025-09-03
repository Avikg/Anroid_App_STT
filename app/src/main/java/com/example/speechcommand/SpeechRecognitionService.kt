package com.example.speechcommand

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SpeechRecognitionService : Service() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isRecognizerListening = false // Flag to track recognizer state
    private var isServiceDestroyed = false // Flag to track service lifecycle for restart logic

    override fun onCreate() {
        super.onCreate()
        Log.d("SpeechService", "Service onCreate")
        setupSpeechRecognizer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding, so return null
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable partial results if needed
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SpeechService", "Ready for speech")
                    isRecognizerListening = true
                }

                override fun onBeginningOfSpeech() {
                    Log.d("SpeechService", "Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // You can monitor audio levels here if needed
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Raw audio buffer, not typically needed for simple command recognition
                }

                override fun onEndOfSpeech() {
                    Log.d("SpeechService", "Speech ended")
                    isRecognizerListening = false
                    // After speech ends, you might want to restart listening if it was a short pause
                    // or if no results were definitive.
                    // However, onError and onResults will handle restarting.
                }

                override fun onError(error: Int) {
                    val errorMessage = getErrorText(error)
                    Log.e("SpeechService", "Speech error: $errorMessage (code: $error)")
                    isRecognizerListening = false
                    // Restart listening after a short delay only if the service is still running
                    if (!isServiceDestroyed) {
                        android.os.Handler(mainLooper).postDelayed({
                            startListening()
                        }, 1000) // Delay before restarting
                    }
                }

                override fun onResults(results: Bundle?) {
                    Log.d("SpeechService", "onResults")
                    isRecognizerListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0] // Get the most confident match
                        Log.d("SpeechService", "Recognized: $command")
                        processCommand(command)
                    }

                    // Continue listening if the service is not destroyed
                    if (!isServiceDestroyed) {
                        android.os.Handler(mainLooper).postDelayed({
                            startListening()
                        }, 500) // Short delay before restarting
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // You can process partial results here for real-time feedback
                    // val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    // if (!partialMatches.isNullOrEmpty()) {
                    //     Log.d("SpeechService", "Partial: ${partialMatches[0]}")
                    // }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Reserved for future use
                }
            })
        } else {
            Log.e("SpeechService", "Speech recognition not available on this device.")
            stopSelf() // Stop the service if recognition is not available
        }
    }

    private fun startListening() {
        if (!isServiceDestroyed && ::speechRecognizer.isInitialized && !isRecognizerListening) {
            try {
                Log.d("SpeechService", "Attempting to start listening...")
                speechRecognizer.startListening(speechRecognizerIntent)
            } catch (e: SecurityException) {
                Log.e("SpeechService", "SecurityException starting listening: ${e.message}")
                // This can happen if permissions are missing or revoked.
                // You might want to notify the user or stop the service.
                stopSelf()
            } catch (e: Exception) {
                Log.e("SpeechService", "Exception starting listening: ${e.message}")
                // Handle other potential errors during startListening
            }
        } else if (isServiceDestroyed) {
            Log.d("SpeechService", "Not starting listening, service is destroyed.")
        } else if (!::speechRecognizer.isInitialized) {
            Log.d("SpeechService", "Not starting listening, speech recognizer not initialized.")
        } else if (isRecognizerListening) {
            Log.d("SpeechService", "Not starting listening, already listening.")
        }
    }

    private fun processCommand(command: String) {
        val processedCommand = parseNaturalLanguageCommand(command.lowercase(Locale.getDefault()))
        if (processedCommand.isNotEmpty()) {
            saveCommandToFile(processedCommand)
            Log.i("SpeechService", "Command processed and saved: $processedCommand")
            // Here you would typically send a broadcast or use other IPC
            // mechanisms to inform other parts of your app about the command.
            // For example:
            // val intent = Intent("com.example.speechcommand.ACTION_COMMAND")
            // intent.putExtra("command", processedCommand)
            // sendBroadcast(intent)
        } else {
            Log.d("SpeechService", "Command not recognized or empty: '$command'")
        }
    }

    private fun parseNaturalLanguageCommand(speech: String): String {
        Log.d("SpeechService", "Parsing command: '$speech'")
        return when {
            // Camera commands
            speech.contains("camera on") || speech.contains("turn on camera") ||
                    speech.contains("switch on camera") || speech.contains("start camera") -> "CAMERA_ON"

            speech.contains("camera off") || speech.contains("turn off camera") ||
                    speech.contains("switch off camera") || speech.contains("stop camera") -> "CAMERA_OFF"

            // Flashlight commands
            speech.contains("flashlight on") || speech.contains("torch on") ||
                    speech.contains("turn on flashlight") || speech.contains("switch on light") -> "FLASHLIGHT_ON"

            speech.contains("flashlight off") || speech.contains("torch off") ||
                    speech.contains("turn off flashlight") || speech.contains("switch off light") -> "FLASHLIGHT_OFF"

            // Volume commands
            speech.contains("volume up") || speech.contains("increase volume") -> "VOLUME_UP"
            speech.contains("volume down") || speech.contains("decrease volume") -> "VOLUME_DOWN"
            speech.contains("mute") || speech.contains("volume off") -> "VOLUME_MUTE"

            // Screen commands
            speech.contains("brightness up") || speech.contains("increase brightness") -> "BRIGHTNESS_UP"
            speech.contains("brightness down") || speech.contains("decrease brightness") -> "BRIGHTNESS_DOWN"

            // Connectivity commands
            speech.contains("wifi on") || speech.contains("turn on wifi") -> "WIFI_ON"
            speech.contains("wifi off") || speech.contains("turn off wifi") -> "WIFI_OFF"
            speech.contains("bluetooth on") || speech.contains("turn on bluetooth") -> "BLUETOOTH_ON"
            speech.contains("bluetooth off") || speech.contains("turn off bluetooth") -> "BLUETOOTH_OFF"

            // App commands
            speech.contains("open camera app") -> "OPEN_CAMERA_APP"
            speech.contains("open settings") -> "OPEN_SETTINGS"
            speech.contains("take photo") || speech.contains("capture photo") ||
                    speech.contains("take picture") || speech.contains("capture picture") -> "TAKE_PHOTO"


            // System commands
            speech.contains("lock screen") || speech.contains("lock phone") -> "LOCK_SCREEN"
            speech.contains("home") || speech.contains("go home") -> "GO_HOME"
            speech.contains("back") || speech.contains("go back") -> "GO_BACK"

            else -> "" // Return empty string if no command is matched
        }
    }

    private fun saveCommandToFile(command: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "$timestamp: $command\n"

            // Use internal storage for app-specific files
            val file = File(filesDir, "speech_commands.txt")
            FileWriter(file, true).use { writer -> // Use 'use' for automatic resource management
                writer.append(logEntry)
            }
            Log.d("SpeechService", "Command saved to file: $command")
        } catch (e: IOException) {
            Log.e("SpeechService", "Error saving command to file", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SpeechService", "Service onStartCommand")
        isServiceDestroyed = false // Reset flag when service (re)starts
        startListening()
        // START_STICKY will restart the service if it's killed by the system,
        // and the last intent will not be redelivered.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceDestroyed = true // Set flag when service is being destroyed
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening() // Explicitly stop listening
            speechRecognizer.destroy()
            Log.d("SpeechService", "SpeechRecognizer destroyed")
        }
        isRecognizerListening = false
        Log.d("SpeechService", "Service onDestroy")
    }

    // Helper function to get human-readable error messages
    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Other client side errors"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            else -> "Unknown speech error"
        }
    }
}
