package com.example.speechcommand

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager

import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraManager: CameraManager
    private var isFlashlightOn = false

    // State variables
    private var isListening by mutableStateOf(false)
    private var recognizedText by mutableStateOf("")
    private var commandHistory by mutableStateOf(listOf<String>())
    // private var lastExecutedCommands by mutableStateOf(listOf<String>()) // Not currently used, consider removing if not needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        setupPermissions()
        setupSpeechRecognizer()
        loadCommandHistory()

        setContent {
            SpeechCommandApp()
        }
    }

    private fun setupPermissions() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission required for speech recognition", Toast.LENGTH_LONG).show()
            }
        }

        // Request RECORD_AUDIO permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        // Also request CAMERA permission if needed for flashlight, ideally at a more relevant point
        // but can be requested here too.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // You might want a separate launcher or handle the result for CAMERA permission
            // For simplicity, using the same launcher here.
            // Consider using ActivityResultContracts.RequestMultiplePermissions() for cleaner multiple permission handling.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Beginning of speech")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                Log.d("SpeechRecognizer", "End of speech")
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown speech error"
                }
                Log.e("SpeechRecognizer", "Error: $errorMessage (code: $error)")
                Toast.makeText(this@MainActivity, "Speech Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    Log.d("SpeechRecognizer", "Recognized: $command")
                    recognizedText = command
                    processCommand(command)
                } else {
                    Log.d("SpeechRecognizer", "No matches found")
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer.startListening(speechRecognizerIntent)
                Log.d("SpeechRecognizer", "Started listening")
            } else {
                Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
                Log.e("SpeechRecognizer", "Recognition not available")
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun processCommand(command: String) {
        val processedCommand = parseNaturalLanguageCommand(command.lowercase(Locale.getDefault()))
        if (processedCommand.isNotEmpty()) {
            saveCommandToFile(processedCommand)
            commandHistory = listOf(processedCommand) + commandHistory.take(19) // Prepend and keep last 20
            Toast.makeText(this, "Command registered: $processedCommand", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unknown command: \"$command\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseNaturalLanguageCommand(speech: String): String {
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

            // WiFi commands
            speech.contains("wifi on") || speech.contains("turn on wifi") -> "WIFI_ON"
            speech.contains("wifi off") || speech.contains("turn off wifi") -> "WIFI_OFF"

            // Bluetooth commands
            speech.contains("bluetooth on") || speech.contains("turn on bluetooth") -> "BLUETOOTH_ON"
            speech.contains("bluetooth off") || speech.contains("turn off bluetooth") -> "BLUETOOTH_OFF"

            // App commands
            speech.contains("open camera app") -> "OPEN_CAMERA_APP"
            speech.contains("open settings") -> "OPEN_SETTINGS"
            speech.contains("take photo") || speech.contains("capture photo") -> "TAKE_PHOTO"

            else -> ""
        }
    }

    private fun saveCommandToFile(command: String) {
        try {
            val file = File(filesDir, "speech_commands.txt")
            FileWriter(file, true).use { writer -> // Use 'use' for auto-closing
                writer.append("${Date()}: $command\n")
            }
            Log.d("FileIO", "Command saved: $command")
        } catch (e: IOException) {
            Log.e("FileIO", "Error saving command", e)
            Toast.makeText(this, "Error saving command", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCommandHistory() {
        try {
            val file = File(filesDir, "speech_commands.txt")
            if (file.exists()) {
                commandHistory = file.readLines().takeLast(20) // Show last 20 commands
                Log.d("FileIO", "Command history loaded: ${commandHistory.size} items")
            } else {
                Log.d("FileIO", "Command history file does not exist.")
            }
        } catch (e: IOException) {
            Log.e("FileIO", "Error loading command history", e)
        }
    }

    private fun readAndExecuteCommands() {
        try {
            val file = File(filesDir, "speech_commands.txt")
            if (file.exists()) {
                val lines = file.readLines()
                val commandsToExecute = lines.mapNotNull { line ->
                    // Extract command from "timestamp: COMMAND" format
                    line.substringAfter(": ", "").trim().takeIf { it.isNotEmpty() }
                }

                if (commandsToExecute.isNotEmpty()) {
                    // lastExecutedCommands = commandsToExecute // Uncomment if you need to store this
                    executeCommands(commandsToExecute)
                } else {
                    Toast.makeText(this, "No valid commands found in the file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No commands file found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("FileIO", "Error reading commands file", e)
            Toast.makeText(this, "Error reading commands file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeCommands(commands: List<String>) {
        if (commands.isEmpty()) {
            Toast.makeText(this, "No commands to execute.", Toast.LENGTH_SHORT).show()
            return
        }
        commands.forEachIndexed { index, command ->
            Log.d("ExecuteCommand", "Executing: $command")
            executeCommand(command)
            if (index < commands.size - 1) { // Avoid sleep after the last command
                try {
                    Thread.sleep(500) // Small delay between commands
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt() // Restore interrupted status
                    Log.e("ExecuteCommand", "Thread sleep interrupted", e)
                    Toast.makeText(this, "Execution interrupted", Toast.LENGTH_SHORT).show()
                    return@forEachIndexed // Exit loop if interrupted
                }
            }
        }
        Toast.makeText(this, "Executed ${commands.size} commands", Toast.LENGTH_SHORT).show()
    }

    private fun executeCommand(command: String) {
        // Most of these commands require specific permissions (CAMERA, ACCESS_WIFI_STATE,
        // CHANGE_WIFI_STATE, BLUETOOTH, BLUETOOTH_ADMIN, WRITE_SETTINGS for brightness)
        // and platform-specific implementations.
        // The toasts are placeholders for actual functionality.
        when (command) {
            "CAMERA_ON" -> Toast.makeText(this, "Camera turned ON (Simulated)", Toast.LENGTH_SHORT).show()
            "CAMERA_OFF" -> Toast.makeText(this, "Camera turned OFF (Simulated)", Toast.LENGTH_SHORT).show()
            "FLASHLIGHT_ON" -> toggleFlashlight(true)
            "FLASHLIGHT_OFF" -> toggleFlashlight(false)
            "VOLUME_UP" -> Toast.makeText(this, "Volume increased (Simulated)", Toast.LENGTH_SHORT).show()
            "VOLUME_DOWN" -> Toast.makeText(this, "Volume decreased (Simulated)", Toast.LENGTH_SHORT).show()
            "VOLUME_MUTE" -> Toast.makeText(this, "Volume muted (Simulated)", Toast.LENGTH_SHORT).show()
            "BRIGHTNESS_UP" -> Toast.makeText(this, "Brightness increased (Simulated)", Toast.LENGTH_SHORT).show()
            "BRIGHTNESS_DOWN" -> Toast.makeText(this, "Brightness decreased (Simulated)", Toast.LENGTH_SHORT).show()
            "WIFI_ON" -> Toast.makeText(this, "WiFi enabled (Simulated)", Toast.LENGTH_SHORT).show()
            "WIFI_OFF" -> Toast.makeText(this, "WiFi disabled (Simulated)", Toast.LENGTH_SHORT).show()
            "BLUETOOTH_ON" -> Toast.makeText(this, "Bluetooth enabled (Simulated)", Toast.LENGTH_SHORT).show()
            "BLUETOOTH_OFF" -> Toast.makeText(this, "Bluetooth disabled (Simulated)", Toast.LENGTH_SHORT).show()
            "OPEN_CAMERA_APP" -> {
                try {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE")
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No app can handle opening the camera.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ExecuteCommand", "Error opening camera app", e)
                    Toast.makeText(this, "Camera app not available", Toast.LENGTH_SHORT).show()
                }
            }
            "OPEN_SETTINGS" -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No app can handle opening settings.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ExecuteCommand", "Error opening settings", e)
                    Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show()
                }
            }
            "TAKE_PHOTO" -> Toast.makeText(this, "Taking photo... (Simulated)", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "Unknown command: $command", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFlashlight(turnOn: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101) // 101 is a request code
            Toast.makeText(this, "Camera permission needed for flashlight.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            // Check if the device has a flash unit
            val hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            if (!hasFlash) {
                Toast.makeText(this, "Device does not have a flashlight.", Toast.LENGTH_SHORT).show()
                return
            }

            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isFlashlightOn = turnOn
                Toast.makeText(this, if (turnOn) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Flashlight not available on any camera.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: CameraAccessException) {
            Log.e("Flashlight", "Camera access error", e)
            Toast.makeText(this, "Could not access camera for flashlight.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { // Catch other potential exceptions
            Log.e("Flashlight", "Error toggling flashlight", e)
            Toast.makeText(this, "Error controlling flashlight.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCommandsFile() {
        try {
            val file = File(filesDir, "speech_commands.txt")
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("FileIO", "Commands file cleared.")
                } else {
                    Log.w("FileIO", "Failed to delete commands file.")
                }
            }
            commandHistory = listOf() // Clear in-memory history
            Toast.makeText(this, "Commands cleared", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("FileIO", "Error clearing commands", e)
            Toast.makeText(this, "Error clearing commands", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Methods for the Debug Button ---
    data class FileStats(val totalLines: Int, val totalCommands: Int)

    private fun getFileStats(): FileStats {
        try {
            val file = File(filesDir, "speech_commands.txt")
            if (file.exists()) {
                val lines = file.readLines()
                val commandCount = lines.mapNotNull { line ->
                    line.substringAfter(": ", "").trim().takeIf { it.isNotEmpty() }
                }.size
                return FileStats(totalLines = lines.size, totalCommands = commandCount)
            }
        } catch (e: IOException) {
            Log.e("DebugFile", "Error getting file stats", e)
        }
        return FileStats(0, 0)
    }

    // getCommandsOnly() is not directly used in the toast,
    // but it's good to have if you want to log them or inspect further.
    private fun getCommandsOnlyFromFile(): List<String> {
        try {
            val file = File(filesDir, "speech_commands.txt")
            if (file.exists()) {
                return file.readLines().mapNotNull { line ->
                    line.substringAfter(": ", "").trim().takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: IOException) {
            Log.e("DebugFile", "Error getting commands only from file", e)
        }
        return emptyList()
    }
    // --- End of Methods for the Debug Button ---

    @Composable
    fun SpeechCommandApp() {
        // Cast LocalContext to MainActivity to access its methods/properties
        val activity = LocalContext.current as MainActivity

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Speech Command Controller",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isListening) "🎤 Listening..." else "🎤 Ready to listen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recognizedText.isNotEmpty()) {
                        Text(
                            text = "Last recognized: \"$recognizedText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { activity.startListening() }, // Call method on activity instance
                    enabled = !isListening,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("🎤 Start Listening")
                }

                Button(
                    onClick = { activity.readAndExecuteCommands() }, // Call method on activity instance
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("▶️ Read & Execute")
                }
            }

            Button(
                onClick = { activity.clearCommandsFile() }, // Call method on activity instance
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("🗑️ Clear All Commands")
            }

            Button(
                onClick = {
                    val stats = activity.getFileStats() // Call method on activity instance
                    // val commandsOnly = activity.getCommandsOnlyFromFile() // Also available
                    Toast.makeText(activity, // Use activity as context
                        "File: ${stats.totalLines} lines, ${stats.totalCommands} commands ready",
                        Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("🔍 Debug File Status")
            }

            Text(
                text = "Command History (Last 20)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (commandHistory.isEmpty()) {
                Text(
                    "No commands in history yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true // Shows newest items at the top if you prepend
                ) {
                    items(commandHistory) { commandWithTimestamp -> // Iterate through the already reversed list
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = commandWithTimestamp,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) { // CAMERA permission request code
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted for flashlight.", Toast.LENGTH_SHORT).show()
                // Optionally, you could automatically try to turn on flashlight if it was the last action
            } else {
                Toast.makeText(this, "Camera permission denied for flashlight.", Toast.LENGTH_SHORT).show()
            }
        }
        // You might have other request codes if you request more permissions.
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        Log.d("MainActivity", "onDestroy called, speech recognizer destroyed.")
    }
}
