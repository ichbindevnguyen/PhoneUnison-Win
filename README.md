# PhoneUnison Windows

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)

**PhoneUnison Windows** is a desktop application that connects your Android phone to your Windows PC. Seamlessly sync notifications, messages, calls, and files between devices.

## ✨ Features

- 🔔 **Notifications Sync** - See all phone notifications on your PC
- 💬 **SMS/MMS** - Send and receive text messages from your computer
- 📞 **Phone Calls** - Get notified of incoming calls and control them
- 📋 **Clipboard Sync** - Copy on phone, paste on PC (and vice versa)
- 📁 **File Transfer** - Drag and drop files between devices
- 🎨 **Beautiful UI** - KDE Breeze-inspired themes with smooth animations
- 🔒 **Secure** - AES-256-GCM encrypted communication

## 💻 Requirements

- Windows 10/11 (64-bit)
- Java 21+ (bundled with installer or install separately)

## 🚀 Getting Started

### Download

Download the latest release from [Releases](https://github.com/ichbindevnguyen/PhoneUnison-Win/releases)

### Build from Source

```bash
git clone https://github.com/ichbindevnguyen/PhoneUnison-Win.git
cd PhoneUnison-Win
mvn clean package
```

## 🔗 Pairing with Phone

1. Install [PhoneUnison Android](https://github.com/ichbindevnguyen/PhoneUnison-Android) on your phone
2. Open both apps on the same WiFi network
3. Click "Pair Device" on Windows
4. Scan the QR code with your phone or enter the code manually
5. Done! Your devices are now connected.

## 📁 Project Structure

```
PhoneUnison-Win/
├── src/main/java/com/phoneunison/desktop/
│   ├── Main.java                        # Entry point
│   ├── PhoneUnisonApp.java              # JavaFX Application
│   ├── config/
│   │   ├── AppConfig.java               # App configuration
│   │   └── PairedDevice.java            # Paired device model
│   ├── network/
│   │   ├── BasicFileUploadHandler.java  # HTTP file upload
│   │   └── UDPDiscoveryService.java     # Device discovery
│   ├── protocol/
│   │   ├── Message.java                 # Message class
│   │   └── MessageHandler.java          # Protocol handler
│   ├── services/
│   │   ├── ClipboardService.java        # Clipboard sync
│   │   ├── ConnectionService.java       # WebSocket server
│   │   └── FileUploadService.java       # File sending
│   ├── ui/
│   │   ├── MainWindow.java              # Main window
│   │   ├── PairingDialog.java           # Pairing dialog
│   │   ├── ThemeManager.java            # Theme handling
│   │   ├── TrayManager.java             # System tray
│   │   └── views/
│   │       ├── CallsView.java           # Dialpad & calls
│   │       ├── FilesView.java           # File transfer
│   │       ├── MessagesView.java        # SMS messaging
│   │       ├── NotificationsView.java   # Notifications
│   │       └── SettingsView.java        # Settings
│   └── utils/
│       ├── CryptoUtils.java             # Encryption
│       └── QRCodeGenerator.java         # QR code generation
├── src/main/resources/
│   └── styles/                          # Theme CSS files
│       ├── kde-breeze.css
│       ├── kde-breeze-light.css
│       └── catppuccin-*.css
├── pom.xml
└── README.md
```

## 🎨 Themes

PhoneUnison supports multiple themes:
- **Light** - KDE Breeze Light inspired
- **Dark** - KDE Breeze Dark inspired
- **Catppuccin** - Catppuccin Mocha palette

Change theme in Settings tab.

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| UI Framework | JavaFX 21 |
| Build Tool | Maven |
| WebSocket | Java-WebSocket |
| QR Code | ZXing |
| Logging | SLF4J + Logback |

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**iBDN** - [GitHub Profile](https://github.com/ichbindevnguyen)

## 🔗 Related

- [PhoneUnison Android](https://github.com/ichbindevnguyen/PhoneUnison-Android) - Android companion app

---

Made with ❤️ by iBDN
