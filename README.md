# DayWiseHub 📱

A modern, cross-platform productivity and wellness app built with **Kotlin Multiplatform** that helps users track their daily tasks, mood, water intake, and personal missions across iOS and Android platforms.

## 🚀 Features

- **Cross-Platform**: Single codebase for iOS and Android
- **Daily Task Management**: Create, track, and complete daily tasks
- **Mood Tracking**: Log and visualize your daily emotional state
- **Water Intake Monitoring**: Stay hydrated with daily water tracking
- **Mission System**: Set and achieve personal goals with milestones
- **Diary & Planning**: Personal journaling and daily planning tools
- **Weather Integration**: Real-time weather data via OpenWeather API
- **Local Database**: Persistent data storage with SQLDelight
- **Modern UI**: Beautiful, responsive design with Compose Multiplatform

## 🛠 Tech Stack

### Core Technologies
- **Kotlin Multiplatform**: Shared business logic across platforms
- **Compose Multiplatform**: Declarative UI framework
- **SQLDelight**: Type-safe database access
- **Koin**: Dependency injection
- **Kotlinx Serialization**: JSON serialization
- **Kotlinx Coroutines**: Asynchronous programming

### Platform-Specific
- **Android**: Native Android components and Material Design
- **iOS**: SwiftUI integration and native iOS features

## 📱 Screenshots

*[Add screenshots of your app here]*

## 🏗 Project Structure

```
daywisehub/
├── shared/                    # Shared Kotlin Multiplatform code
│   ├── src/commonMain/       # Common business logic
│   ├── src/androidMain/      # Android-specific implementations
│   └── src/iosMain/          # iOS-specific implementations
├── androidApp/               # Android application
├── iosApp/                   # iOS application
└── gradle/                   # Build configuration
```

### Key Components

- **Database Layer**: SQLDelight with type-safe queries
- **Repository Pattern**: Clean data access layer
- **ViewModel Architecture**: MVVM pattern with shared ViewModels
- **API Integration**: OpenWeather API for weather data
- **UI Components**: Reusable Compose components

## 🚀 Getting Started

### Prerequisites

- **Kotlin**: 1.9.0 or higher
- **Android Studio**: Latest version with Kotlin Multiplatform support
- **Xcode**: Latest version (for iOS development)
- **JDK**: 17 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/daywisehub.git
   cd daywisehub
   ```

2. **Open in Android Studio**
   - Open the project in Android Studio
   - Sync Gradle files
   - Build the project

3. **Run on Android**
   ```bash
   ./gradlew androidApp:installDebug
   ```

4. **Run on iOS**
   - Open `iosApp/iosApp.xcodeproj` in Xcode
   - Select your target device/simulator
   - Build and run

### Development Setup

1. **Shared Module Development**
   - Edit code in `shared/src/commonMain/`
   - Changes automatically sync to both platforms

2. **Platform-Specific Code**
   - Android: `shared/src/androidMain/`
   - iOS: `shared/src/iosMain/`

3. **Database Schema Changes**
   - Modify SQL files in `shared/src/commonMain/sqldelight/`
   - Run `./gradlew generateSqlDelightInterface` to regenerate

## 🗄 Database Schema

The app uses SQLDelight with the following main entities:

- **Users**: User profiles and authentication
- **TodayTask**: Daily task management
- **TodayMood**: Mood tracking entries
- **WaterIntake**: Hydration tracking
- **Mission**: Long-term goals and missions
- **Milestone**: Mission progress tracking

## 🔧 Configuration

### Environment Variables

Create a `local.properties` file in the root directory:

```properties
# Weather API Configuration
OPENWEATHER_API_KEY=your_api_key_here
```

### API Keys

- **OpenWeather API**: Get your free API key from [OpenWeather](https://openweathermap.org/api)

## 📊 Architecture

The app follows clean architecture principles:

```
UI Layer (Compose Multiplatform)
    ↓
ViewModel Layer (Shared ViewModels)
    ↓
Repository Layer (Data Access)
    ↓
Database/API Layer (SQLDelight + HTTP)
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run Android tests
./gradlew androidApp:test

# Run shared module tests
./gradlew shared:test
```

## 📦 Building

### Android APK
```bash
./gradlew androidApp:assembleRelease
```

### iOS Archive
- Use Xcode to archive the iOS app
- Follow standard iOS distribution process

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Use meaningful commit messages
- Add tests for new features
- Update documentation as needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Kotlin Multiplatform** team for the amazing cross-platform framework
- **JetBrains Compose** for the modern UI toolkit
- **OpenWeather** for weather data API

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/daywisehub/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/daywisehub/discussions)

---

**Built with ❤️ using Kotlin Multiplatform**

*Star this repository if you find it helpful! ⭐* 