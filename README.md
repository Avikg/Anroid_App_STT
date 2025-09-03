# Speech Command Controller

A powerful Android application that converts natural language voice commands into device actions. Simply speak commands like "turn on camera" or "flashlight on" and the app will save them to a text file for sequential execution.

![Speech Command Controller](https://img.shields.io/badge/Android-Voice%20Control-brightgreen)
![API Level](https://img.shields.io/badge/API-23%2B-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-purple)

## 🚀 Features

### 🎤 Advanced Speech Recognition
- **Real-time speech-to-text** conversion using Android's built-in recognition
- **Natural language processing** - understands various ways to say the same command
- **Continuous listening** capability with background service
- **Multi-language support** (follows system locale)

### 📝 Smart Command Processing
- **Intelligent parsing** of natural speech patterns
- **Command standardization** - converts speech to executable actions
- **File-based storage** with timestamps for command history
- **Persistent command queue** survives app restarts

### ⚡ Device Control
- **Camera control** - Turn on/off, open camera app, take photos
- **Flashlight control** - Toggle device flashlight
- **Volume management** - Adjust volume up/down, mute
- **Connectivity** - WiFi and Bluetooth toggles
- **System actions** - Open settings, navigate home
- **Brightness control** - Adjust screen brightness

### 🔧 Smart Execution
- **Sequential command execution** from saved text file
- **Real-time execution feedback** with status updates
- **Error handling** with success/failure reporting
- **Threaded execution** prevents UI blocking
- **Command history** display with recent activities

## 📱 Screenshots

| Main Interface | Command History | Execution Status |
|:---:|:---:|:---:|
| ![Main UI](docs/screenshot1.png) | ![History](docs/screenshot2.png) | ![Execution](docs/screenshot3.png) |

## 🎯 Supported Voice Commands

### 📹 Camera Commands
- `"camera on"` / `"turn on camera"` / `"switch on camera"`
- `"camera off"` / `"turn off camera"` / `"switch off camera"`
- `"take photo"` / `"capture photo"` / `"take picture"`
- `"open camera app"`

### 🔦 Flashlight Commands
- `"flashlight on"` / `"torch on"` / `"turn on flashlight"`
- `"flashlight off"` / `"torch off"` / `"turn off flashlight"`

### 🔊 Audio Commands
- `"volume up"` / `"increase volume"`
- `"volume down"` / `"decrease volume"`
- `"mute"` / `"volume off"`

### 🌐 Connectivity Commands
- `"wifi on"` / `"turn on wifi"`
- `"wifi off"` / `"turn off wifi"`
- `"bluetooth on"` / `"turn on bluetooth"`
- `"bluetooth off"` / `"turn off bluetooth"`

### ⚙️ System Commands
- `"open settings"`
- `"brightness up"` / `"increase brightness"`
- `"brightness down"` / `"decrease brightness"`
- `"go home"` / `"home"`
- `"go back"` / `"back"`

## 🛠️ Installation

### Prerequisites
- **Android Studio** Arctic Fox (2020.3.1) or later
- **Android SDK** API 23+ (Android 6.0+)
- **Kotlin** 1.9.22 or compatible version
- **Device with microphone** for speech recognition

### Quick Setup
1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/speech-command-controller.git
   cd speech-command-controller
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync dependencies**
   - Click "Sync Project with Gradle Files" when prompted
   - Wait for all dependencies to download

4. **Build and run**
   - Connect your Android device or start an emulator
   - Click the green "Run" button (▶️)

### Manual Installation
1. Download the [latest APK](https://github.com/Avikg/Anroid_App_STT/releases)
2. Enable "Install from Unknown Sources" in device settings
3. Install the APK file
4. Grant necessary permissions when prompted

## 🔧 Configuration

### Required Permissions
The app automatically requests these permissions:
- **🎤 RECORD_AUDIO** - For speech recognition
- **📷 CAMERA** - For flashlight and camera control
- **📶 ACCESS_WIFI_STATE** - For WiFi status monitoring
- **🔵 BLUETOOTH** - For Bluetooth control

### Optional Setup
- **Background Service**: Enable for continuous listening
- **Battery Optimization**: Disable for better performance
- **Accessibility Service**: For advanced system control (future feature)

## 🚦 Usage

### Basic Operation
1. **Launch the app** and grant required permissions
2. **Tap "🎤 Start Listening"** when ready to give commands
3. **Speak clearly**: Say commands like "flashlight on" or "camera on"
4. **View confirmation**: Check that commands appear in "Command History"
5. **Execute commands**: Tap "▶️ Read & Execute" to run saved commands

### Advanced Features
- **Debug File Status**: Tap "🔍 Debug File Status" to check saved commands
- **Clear Commands**: Use "🗑️ Clear All Commands" to reset command history
- **Batch Execution**: Record multiple commands before executing them sequentially

### File Management
Commands are saved to internal storage:
- **File location**: `app_data/speech_commands.txt`
- **Format**: `[timestamp] COMMAND_NAME`
- **Persistence**: Commands survive app restarts

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/example/speechcommand/
├── MainActivity.kt              # Main UI and speech recognition
├── SpeechRecognitionService.kt  # Background listening service
├── CommandExecutor.kt           # Device command execution
└── FileManager.kt              # File operations and storage
```

### Key Components
- **🎤 Speech Recognition**: Android SpeechRecognizer API
- **🎨 UI Framework**: Jetpack Compose with Material 3
- **📁 Storage**: Internal file system with text-based format
- **🔧 Command Processing**: Pattern matching with natural language support
- **⚙️ Device Control**: Android system APIs for hardware control

### Design Patterns
- **MVVM Architecture** for clean separation of concerns
- **Repository Pattern** for data management
- **Observer Pattern** for real-time UI updates
- **Command Pattern** for action execution

## 🔍 Technical Details

### Dependencies
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.material3:material3")

// Camera and Media
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
```

### Performance Optimizations
- **Efficient threading** for command execution
- **Memory management** for speech recognition
- **Battery optimization** with smart listening cycles
- **Minimal UI updates** for smooth performance

### Security Features
- **Local processing** - no data sent to external servers
- **Permission-based access** to device features
- **Secure file storage** in app-private directory

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Getting Started
1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** and test thoroughly
4. **Commit changes**: `git commit -m 'Add amazing feature'`
5. **Push to branch**: `git push origin feature/amazing-feature`
6. **Open a Pull Request**

### Contribution Guidelines
- Follow **Kotlin coding standards**
- Add **comprehensive tests** for new features
- Update **documentation** for API changes
- Ensure **backward compatibility**

### Areas for Contribution
- 🌍 **Multi-language support**
- 🤖 **AI-powered command recognition**
- 🔊 **Voice feedback system**
- 📱 **Smart home integration**
- 🎯 **Custom wake words**

## 🐛 Troubleshooting

### Common Issues

#### Speech Recognition Not Working
- ✅ **Check microphone permission** in device settings
- ✅ **Ensure internet connection** (Google services requirement)
- ✅ **Update Google app** if speech recognition fails
- ✅ **Speak clearly and slowly** for better recognition

#### Commands Not Executing
- ✅ **Verify permissions** for camera, audio, etc.
- ✅ **Check command format** - use exact phrases from supported list
- ✅ **Test individual commands** before batch execution
- ✅ **Review execution logs** in debug output

#### App Performance Issues
- ✅ **Restart the app** to clear memory
- ✅ **Clear command history** if file becomes too large
- ✅ **Check available storage** space
- ✅ **Update Android System WebView**

### Debug Tools
- **🔍 Debug File Status**: Check saved command count
- **📊 Logcat Output**: View detailed error messages
- **🧪 Test Commands**: Use provided sample commands

## 🔮 Roadmap

### Version 2.0 (Coming Soon)
- [ ] **AI-Enhanced Recognition** - Machine learning for better accuracy
- [ ] **Custom Commands** - User-defined voice shortcuts
- [ ] **Smart Home Integration** - Control IoT devices
- [ ] **Voice Feedback** - Audio confirmation of actions

### Version 3.0 (Future)
- [ ] **Multi-Device Control** - Command multiple phones/tablets
- [ ] **Cloud Synchronization** - Sync commands across devices
- [ ] **Workflow Automation** - Complex command sequences
- [ ] **Voice Authentication** - Speaker recognition for security

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Speech Command Controller

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## 🙏 Acknowledgments

- **Android Speech Recognition API** for robust voice processing
- **Jetpack Compose** for modern UI development
- **Material Design 3** for beautiful, accessible interfaces
- **Open Source Community** for inspiration and best practices

## 📞 Support

### Get Help
- 📖 **Documentation**: Check this README and inline code comments
- 🐛 **Bug Reports**: Open an issue on GitHub
- 💡 **Feature Requests**: Suggest new features via GitHub Issues
- 💬 **Discussions**: Join our GitHub Discussions

### Contact Information
- **Developer**: Avik
- **GitHub**: [@Avikg](https://github.com/Avikg)
- **Project Link**: [https://github.com/Avikg/Anroid_App_STT](https://github.com/Avikg/Anroid_App_STT)

---

**⭐ If this project helped you, please consider giving it a star on GitHub!**

Made with ❤️ for the Android community
