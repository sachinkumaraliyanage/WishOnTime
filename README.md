# WishOnTime 📱

WishOnTime is a modern, high-performance Android application designed for efficient mass SMS scheduling. Built with the latest Android 15 (API 36) standards, it features a sleek Material 3 user interface with Dynamic Color support, ensuring a personalized experience that matches your device's theme.

## ✨ Features

- **Mass SMS Scheduling**: Send messages to multiple recipients with a built-in 2-second delay to comply with carrier standards and prevent blocking.
- **Dynamic Contact Management**: 
    - **Include List**: Specify exactly who should receive your message.
    - **Exclude List**: Send to everyone except specific contacts.
    - Real-time deduplication based on normalized phone numbers.
- **Material 3 Design**: 
    - Full support for **Material You (Dynamic Colors)**.
    - Modern card-based layout with smooth scrolling.
    - Adaptive and Monochrome app icons for a consistent look on Android 13+.
- **Reliable Background Processing**: Uses an Android Foreground Service (`DATA_SYNC`) to ensure message delivery even when the app is in the background.
- **Real-time Tracking**: Monitor the progress of your scheduled tasks through a persistent notification.

## 🛠️ Tech Stack

- **Platform**: Android 15 (Target SDK 36)
- **Language**: Java
- **UI Framework**: Material 3 (M3)
- **Components**: 
    - `CoordinatorLayout` & `NestedScrollView` for fluid UI.
    - `RecyclerView` for efficient list management.
    - `MaterialCardView` for modern container styling.
    - `SmsManager` for core messaging logic.

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync the Gradle files.
4. Run the app on an Android 12+ device to experience Dynamic Colors.

## 👨‍💻 Developer

**Sachin Kumara Liyanage**  
Founder at **TechBird Solutions**

Connect with me:
- 🌐 [Website](https://techbirdssolutions.com/)
- 📘 [Facebook](https://www.facebook.com/sachinkumaraliyanage)
- 🐙 [GitHub](https://github.com/sachinkumaraliyanage)

## 📄 License

This project is developed by TechBird Solutions. MIT License.
