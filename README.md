#Vaidik Mantra Jap (Android App)

Vaidik Mantra Jap is an Android application for focused mantra chanting and count tracking, designed with a simple and distraction-free interface.

Features
Mantra chanting counter with persistent tracking
Offline storage using Room Database
Cloud sync with Firebase Realtime Database
Smooth performance using Kotlin Coroutines
Secure release setup with proper signing and build configuration
Optimized production build with R8/ProGuard handling
Tech Stack
Kotlin
Android SDK
Room Database
Firebase Realtime Database
Kotlin Coroutines
Gradle (KTS)
Architecture & Approach
MVVM-based structure for separation of concerns
Repository pattern for data handling (local + cloud)
Coroutine-based async operations for non-blocking UI
Firebase integration for real-time data sync
Room for reliable offline-first experience
Key Highlights
Handles both offline and online data consistency
Production-ready release setup (signing, build variants, ProGuard rules)
Proper handling of Firebase serialization and mapping
Efficient background processing using structured concurrency
Setup
Clone the repository
git clone https://github.com/your-username/vaidik-mantra-jap.git
Open in Android Studio
Add your Firebase configuration (google-services.json)
Sync Gradle and run the app
Future Improvements
Jetpack Compose UI migration
User authentication
Offline-first sync strategy improvement
Usage analytics and insights
Note

This project demonstrates real-world Android development experience including local storage, cloud sync, and production release handling.
