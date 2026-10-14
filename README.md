# DayCount 🗓️

[![Android SDK](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple.svg)](https://developer.android.com/jetpack/compose)

**DayCount** is a modern, privacy-first Android utility app designed for precise date range calculations. Whether you need to count working days for a project, plan vacations, calculate event durations, or analyze weekday vs. weekend distributions, DayCount provides instant, inclusive results in constant time ($O(1)$).

---

## ✨ Features

- **Inclusive Date Range Counting**: Both start and end dates are included (e.g., Jan 1 to Jan 1 = 1 day).
- **Detailed Breakdowns**:
  - **Total Days**: Total elapsed calendar days.
  - **Weekdays & Weekend Days**: Automatic Mon–Fri vs. Sat–Sun classification.
  - **Percentage Split**: Visual percentage distribution of weekdays vs. weekends.
  - **Day-of-Week Distribution**: Exact count for each individual day of the week (Mondays, Tuesdays, etc.).
- **Quick Range Presets**: One-tap selection for Today, This Week, This Month, and This Year.
- **Range Shifting**: Effortlessly shift date ranges forward or backward by weeks or months.
- **Calculation History**: Save past calculations, re-open them with a single tap, filter, and manage history logs.
- **Share & Copy**: Quickly format and share calculation results as text.
- **Customizable Experience**:
  - Light, Dark, and Dynamic System Theme support.
  - Toggleable haptic feedback and smooth UI animations.
  - Custom date format options.
- **100% Offline & Private**: Zero permissions required. No internet access, tracking, ads, or analytics. All data stays local on device.

---

## 🛠️ Architecture & Tech Stack

DayCount is built following modern Android development best practices and MVVM / Clean Architecture principles:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3 (M3)](https://m3.material.io/)
- **Architecture**: MVVM + Repository Pattern with Kotlin `StateFlow` and `Coroutines`
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Data Persistence**: Offline JSON serialization via DataStore / KeyValueStore
- **Date Engine**: Constant-time calculation logic leveraging `java.time` APIs (`LocalDate`, `ChronoUnit`, `TemporalAdjusters`)
- **Testing**: Comprehensive Unit Test suite covering Domain Calculators, Repositories, ViewModels, Presets, and Formatter.

---

## 🧮 Calculation Engine Logic

The calculation logic runs in **$O(1)$ constant time**, avoiding inefficient day-by-day looping even for multi-year ranges:
1. Complete 7-day weeks contribute **5 weekdays** and **2 weekend days** each.
2. Only the remaining 0–6 leftover days are individually inspected starting from the range's start weekday.
3. Order of dates is automatically normalized (earlier to later).

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug / Meerkat or newer
- **JDK**: Java 11 or higher
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 37

### Build Commands
```bash
# Clone the repository
git clone https://github.com/abdullah-ateeq/DayCount.git
cd DayCount

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test
```

---

## 🔒 Privacy

DayCount respects user privacy.
- No network permissions requested (`android.permission.INTERNET` is omitted).
- All user history and preferences remain strictly on your local device.
