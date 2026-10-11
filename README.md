# Contacts Cleaner

**Contacts Cleaner** is a modern, minimalistic Android utility that analyzes, classifies, cleans, and syncs contacts. Built with Kotlin, Jetpack Compose, and following Clean Architecture principles.

## Features

- 📱 **WhatsApp Contact Detection** - Identify contacts that don't have WhatsApp accounts
- 🗑️ **Junk Contact Cleaner** - Remove invalid or low-quality contacts
- 🔄 **Duplicate Detection** - Find and merge duplicate contacts
- 📁 **CSV/TXT Import** - Import contacts from raw files
- ☁️ **Contacts Sync** - Save cleaned contacts through Android's Contacts Provider (Premium)
- 💳 **Paywall System** - Freemium model with premium features

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: Clean Architecture (Domain, Data, UI layers)
- **Dependency Injection**: Hilt
- **Async**: Coroutines + Flow
- **Navigation**: Navigation Compose
- **Billing**: Google Play Billing Library v6
- **Contacts Integration**: Android Contacts Provider; account-backed contacts may be synchronized by Android and the account's sync provider

## Project Structure

```
com.cleancontacts.app/
├── domain/
│   ├── model/           # Data models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business logic use cases
├── data/
│   ├── detector/        # Junk & Duplicate detection algorithms
│   ├── repository/      # Repository implementations
│   └── source/          # Data sources (ContactsProvider, etc.)
├── ui/
│   ├── dashboard/       # Main dashboard screen
│   ├── results/         # Scan results display
│   ├── paywall/         # Premium upgrade screen
│   ├── navigation/      # Navigation graph
│   └── theme/           # WhatsApp Green theme + Typography
└── di/                  # Hilt dependency injection modules
```

## Design

- **Color Scheme**: WhatsApp Green (#25D366) as primary color
- **UI Style**: Apple-inspired minimalistic design
- **Typography**: Clean, modern font hierarchy
- **Components**: Rounded cards, smooth transitions, soft shadows

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 8 or higher
- Android SDK with minimum API 26 (Android 8.0)

### Build Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

### Permissions Required

- `READ_CONTACTS` - To scan and analyze device contacts
- `WRITE_CONTACTS` - To delete, merge, and save contacts
- `INTERNET` - For network-dependent app features

## Architecture

The app follows Clean Architecture principles:

### Domain Layer
- Pure Kotlin models and interfaces
- Business logic in use cases
- No Android dependencies

### Data Layer  
- Repository implementations
- Junk and duplicate detection algorithms
- Android Contacts Provider integration for reading and managing device contacts

### UI Layer
- Jetpack Compose screens
- ViewModels with StateFlow
- Hilt-powered dependency injection

## Premium Features

The app implements a freemium model:

**Free:**
- Scan contacts
- Preview results (WhatsApp, Junk, Duplicates)

**Premium (Unlocks):**
- Delete junk contacts
- Merge duplicates
- Save imported contacts
- Save cleaned contacts through Android's Contacts Provider

## License

This project is for educational and demonstration purposes.

## Contact

For questions or feedback, please open an issue on GitHub.
