# 💸 ExpenseTracker

A modern Android expense tracking app built with **Jetpack Compose** and **Kotlin**. It automatically captures transactions from SMS bank alerts, lets you log expenses manually, set budgets, and track income — all secured behind biometric authentication.

---

## ✨ Features

- 📊 **Dashboard** — at-a-glance overview of your spending, income, and balance
- 🧾 **Transaction History** — full list of all expenses and income with category filters
- 📥 **Auto SMS Capture** — listens for bank/payment SMS alerts and automatically logs transactions (debit & credit) with merchant and category detection
- 📈 **Income Tracking** — record and monitor income sources separately
- 🎯 **Budget Management** — set monthly budgets and track remaining spend
- 🔒 **Biometric Lock** — optional fingerprint/face authentication to protect your data
- 🔔 **Notifications** — instant alerts when a new transaction is auto-captured
- 🗂️ **Smart Categorization** — automatically infers categories (Food, Transport, Shopping, Bills, Entertainment) from merchant names

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3 |
| UI | Jetpack Compose + Material 3 |
| Navigation | AndroidX Navigation 3 |
| Database | Room (SQLite) |
| Architecture | MVVM (ViewModel + Repository) |
| Async | Kotlin Coroutines |
| Auth | AndroidX Biometric |
| Min SDK | API 33 (Android 13) |
| Target SDK | API 36 (Android 16) |

---

## 🏗️ Project Structure

```
app/src/main/java/com/example/expensetracker/
├── MainActivity.kt              # App entry point, biometric auth, permissions
├── Navigation.kt                # Bottom nav bar + NavDisplay routing
├── NavigationKeys.kt            # Type-safe navigation keys
├── data/
│   ├── ExpenseRepository.kt     # Single source of truth for all data ops
│   └── local/
│       ├── AppDatabase.kt       # Room database definition
│       ├── ExpenseDao.kt        # DAO queries
│       ├── ExpenseEntity.kt     # Expense/income table entity
│       └── BudgetEntity.kt      # Budget table entity
├── receiver/
│   └── SmsReceiver.kt           # BroadcastReceiver — parses bank SMS alerts
├── service/
│   └── PayNotificationListener.kt  # Notification listener service
├── ui/
│   ├── dashboard/               # Home screen with summary cards & charts
│   ├── transactions/            # Transaction list screen
│   ├── income/                  # Income entry & overview screen
│   ├── budget/                  # Budget setup & tracking screen
│   ├── settings/                # App settings (biometric toggle, currency, etc.)
│   └── components/              # Shared Compose components (charts, etc.)
├── theme/                       # Material 3 color, typography, theme
└── utils/
    └── CurrencyFormatter.kt     # Locale-aware currency formatting
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Meerkat (2024.3) or later
- **JDK 17** (bundled with Android Studio)
- **Android SDK** — API 33 or higher
- A physical device or emulator running Android 13+

### 1. Clone the Repository

```bash
git clone https://github.com/sanjyahoo/ExpenseTracker.git
cd ExpenseTracker
```

### 2. Open in Android Studio

1. Launch **Android Studio**
2. Click **File → Open** and select the `ExpenseTracker` folder
3. Wait for Gradle sync to complete (this downloads all dependencies automatically)

### 3. Run the App

- Connect an Android device via USB with **Developer Options & USB Debugging** enabled, **or** create an emulator via **Device Manager** (API 33+)
- Click the ▶️ **Run** button or press `Shift + F10`

### 4. Grant Permissions

On first launch, the app will request the following permissions:

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Auto-capture bank transaction SMS alerts |
| `READ_SMS` | Parse received SMS content |
| `POST_NOTIFICATIONS` *(Android 13+)* | Show alerts when a transaction is captured |

> **Tip:** For SMS auto-capture to work, grant all permissions and ensure your bank sends transaction SMS alerts to your phone number.

---

## 🔒 Biometric Lock

The app supports optional fingerprint / face unlock:

1. Go to **Settings** tab inside the app
2. Toggle **Biometric Lock** on
3. On next launch, you'll be prompted to authenticate before the app opens

---

## 🔄 How SMS Auto-Capture Works

1. A bank sends a debit/credit SMS (e.g. *"Rs 500 debited from your account at Amazon"*)
2. `SmsReceiver` intercepts it via `android.provider.Telephony.SMS_RECEIVED`
3. The message is parsed for **amount**, **transaction type** (debit/credit), and **merchant**
4. The merchant name is matched against category keywords (Food, Transport, Shopping, etc.)
5. A new transaction is saved to the local Room database
6. A notification is shown — tap it to open and verify the entry

---

## 🏦 Building a Release APK

```bash
./gradlew assembleRelease
```

The APK will be output to:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

> **Note:** You'll need to sign the APK with a keystore before distributing it. See the [Android signing guide](https://developer.android.com/studio/publish/app-signing).

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is open source. Feel free to use, modify, and distribute it.
