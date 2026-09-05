# SmartSolarMicrogrid

SmartSolarMicrogrid is a comprehensive solution designed to manage and monitor solar energy microgrids. The system consists of three main components: a mobile application for end-users, a web dashboard for administrators, and a robust backend API to handle data and business logic.

## 🚀 Technologies Used

- **Android (Mobile App)**: Native Android development (Kotlin/Java) utilizing modern AndroidX libraries.
- **Web (Dashboard)**: A fast, modern frontend built with Vite and Node.js.
- **Backend (API)**: A RESTful API built with .NET (ASP.NET Core).

---

## 🛠️ How to Run the Project

Each component of the project runs independently. Follow the instructions below to get each part up and running.

### 1. Backend (ASP.NET Core API)

The backend serves as the central data hub for both the web and mobile applications.

**Prerequisites:** .NET SDK installed on your machine.

**Steps to run:**
1. Open your terminal.
2. Navigate to the backend directory:
   ```bash
   cd backend/SmartSolarMicrogrid.API
   ```
3. Run the application:
   ```bash
   dotnet run
   ```
*(The API will typically start on `http://localhost:5000` or `https://localhost:5001`. You can check the terminal output for the exact URL).*

---

### 2. Web (Vite Frontend)

The web dashboard provides a user interface for monitoring and management.

**Prerequisites:** Node.js and npm installed on your machine.

**Steps to run:**
1. Open your terminal.
2. Navigate to the web directory:
   ```bash
   cd web/smart-solar-web
   ```
3. Install the dependencies (only needed the first time):
   ```bash
   npm install
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```
*(The web app will typically be accessible at `http://localhost:5173`).*

---

### 3. Android (Mobile App)

The Android app provides on-the-go access to solar microgrid data.

**Prerequisites:** Android Studio installed on your machine, along with an Android Emulator or a physical device.

**Steps to run (Recommended Method):**
1. Open **Android Studio**.
2. Select **File > Open** and choose the `android` folder located in this repository (`SmartSolarMicrogrid/android`).
3. Allow Gradle to sync the project.
4. Click the green **Run 'app'** button (▶) in the top toolbar to launch the app on your emulator or connected device.

**Steps to run (Command Line):**
If you prefer building via the terminal, navigate to the `android` folder and run:
```bash
cd android
./gradlew installDebug
```
*(Note: On Windows, use `gradlew.bat installDebug` instead).*

---

## 🔒 Environment Variables

Template `.env` files are provided in each directory (`backend`, `web`, `android`). 
- **Web**: Uses `VITE_` prefixed variables.
- **Backend**: Ready for `DotNetEnv` or standard configuration usage.
- **Android**: Variables can be set for emulator connections (e.g., `10.0.2.2`).

*Note: Environment variables are ignored by Git to protect sensitive information.*
