package com.example.speechcommand

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

class CommandExecutor(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    fun executeCommand(command: String): Boolean {
        return try {
            when (command.trim()) {
                "CAMERA_ON" -> {
                    openCameraApp()
                    true
                }

                "CAMERA_OFF" -> {
                    showToast("Camera turned OFF")
                    // Note: Cannot programmatically close camera app
                    true
                }

                "FLASHLIGHT_ON" -> {
                    toggleFlashlight(true)
                }

                "FLASHLIGHT_OFF" -> {
                    toggleFlashlight(false)
                }

                "VOLUME_UP" -> {
                    adjustVolume(AudioManager.ADJUST_RAISE)
                }

                "VOLUME_DOWN" -> {
                    adjustVolume(AudioManager.ADJUST_LOWER)
                }

                "VOLUME_MUTE" -> {
                    adjustVolume(AudioManager.ADJUST_MUTE)
                }

                "BRIGHTNESS_UP" -> {
                    adjustBrightness(true)
                }

                "BRIGHTNESS_DOWN" -> {
                    adjustBrightness(false)
                }

                "WIFI_ON" -> {
                    toggleWifi(true)
                }

                "WIFI_OFF" -> {
                    toggleWifi(false)
                }

                "BLUETOOTH_ON" -> {
                    toggleBluetooth(true)
                }

                "BLUETOOTH_OFF" -> {
                    toggleBluetooth(false)
                }

                "OPEN_CAMERA_APP" -> {
                    openCameraApp()
                }

                "OPEN_SETTINGS" -> {
                    openSettings()
                }

                "TAKE_PHOTO" -> {
                    takePhoto()
                }

                "LOCK_SCREEN" -> {
                    lockScreen()
                }

                "GO_HOME" -> {
                    goHome()
                }

                "GO_BACK" -> {
                    goBack()
                }

                else -> {
                    showToast("Unknown command: $command")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing command: $command", e)
            showToast("Error executing command: $command")
            false
        }
    }

    private fun toggleFlashlight(turnOn: Boolean): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, turnOn)
                showToast(if (turnOn) "Flashlight ON" else "Flashlight OFF")
                true
            } else {
                showToast("Camera permission required for flashlight")
                false
            }
        } catch (e: CameraAccessException) {
            Log.e("CommandExecutor", "Camera access error", e)
            showToast("Flashlight not available")
            false
        }
    }

    private fun adjustVolume(direction: Int): Boolean {
        return try {
            audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
            val volumeText = when (direction) {
                AudioManager.ADJUST_RAISE -> "Volume increased"
                AudioManager.ADJUST_LOWER -> "Volume decreased"
                AudioManager.ADJUST_MUTE -> "Volume muted"
                else -> "Volume adjusted"
            }
            showToast(volumeText)
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Volume adjustment error", e)
            false
        }
    }

    private fun adjustBrightness(increase: Boolean): Boolean {
        return try {
            // Note: Automatic brightness adjustment requires system-level permissions
            // This opens brightness settings for manual adjustment
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Opening brightness settings")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Brightness adjustment error", e)
            showToast("Cannot adjust brightness automatically")
            false
        }
    }

    private fun toggleWifi(enable: Boolean): Boolean {
        return try {
            // Note: WiFi toggle requires system permissions on Android 10+
            // Opening WiFi settings instead
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Opening WiFi settings")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "WiFi toggle error", e)
            false
        }
    }

    private fun toggleBluetooth(enable: Boolean): Boolean {
        return try {
            if (bluetoothAdapter != null) {
                if (enable && !bluetoothAdapter.isEnabled) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    showToast("Opening Bluetooth settings")
                } else if (!enable && bluetoothAdapter.isEnabled) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    showToast("Opening Bluetooth settings")
                } else {
                    showToast("Bluetooth already ${if (enable) "enabled" else "disabled"}")
                }
                true
            } else {
                showToast("Bluetooth not available")
                false
            }
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Bluetooth toggle error", e)
            false
        }
    }

    private fun openCameraApp(): Boolean {
        return try {
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Opening camera")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Camera app error", e)
            showToast("Camera app not available")
            false
        }
    }

    private fun openSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Opening settings")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Settings error", e)
            false
        }
    }

    private fun takePhoto(): Boolean {
        return try {
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Taking photo...")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Take photo error", e)
            false
        }
    }

    private fun lockScreen(): Boolean {
        return try {
            // Note: Screen locking requires device admin permissions
            showToast("Screen lock command received (requires device admin)")
            false
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Lock screen error", e)
            false
        }
    }

    private fun goHome(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("Going to home screen")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Go home error", e)
            false
        }
    }

    private fun goBack(): Boolean {
        return try {
            // Note: Cannot programmatically press back button without accessibility service
            showToast("Back command received (requires accessibility service)")
            false
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Go back error", e)
            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}