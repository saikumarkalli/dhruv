# Future Implementations & Roadmaps

This document serves as an ongoing tracker for architectural scaffolds and features that have been planned but require dedicated future sessions to implement fully. This ensures continuity and acts as an easy reminder for complex system integrations.

## ⏰ Time Tools: Advanced Alarm System

**Current State:**
The Data Layer (Room DB `AlarmEntity`, `AlarmDao`) and UI Layer (`AlarmScreen`, `AlarmViewModel`) have been successfully implemented and integrated into the app. However, the OS-level scheduling has been deliberately scaffolded to be implemented in a dedicated phase due to Android background execution complexity.

**Pending Implementation Details:**

To complete the Alarm functionality, the following system-level integrations must be built:

### 1. AlarmManager & Exact Scheduling
- **Implementation**: Utilize `AlarmManager.setExactAndAllowWhileIdle()` to schedule alarms precisely.
- **Android 14+ Considerations**: Must handle `SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM` permissions. If denied by the OS or User, the app must gracefully degrade or prompt the user via the `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` intent.

### 2. Broadcast Receiver (`AlarmReceiver`)
- **Implementation**: Create a `BroadcastReceiver` that catches the intent fired by the `AlarmManager`.
- **Responsibility**: Immediately hand off the work to a Foreground Service and acquire a `WakeLock` to ensure the CPU doesn't sleep before the service boots.

### 3. Foreground Service (`AlarmService`)
- **Implementation**: A service running with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` or `SPECIAL_USE`.
- **Responsibility**: 
  - Play the alarm tone using `MediaPlayer`.
  - Vibrate the device.
  - Post a High-Priority, Max-Importance Notification (`NotificationManager.IMPORTANCE_HIGH`) so it interrupts the user.

### 4. Full-Screen Intent & Math Puzzle (`MathPuzzleActivity` / Compose Equivalent)
- **Implementation**: The notification built by the Foreground Service must include a `setFullScreenIntent()`.
- **Responsibility**: This intent launches an Activity that displays over the lock screen (`showWhenLocked=true`, `turnScreenOn=true`). 
- **Math Dismissal**: The UI must generate a randomized math puzzle based on the `AlarmEntity.puzzleDifficulty`. The user must solve it to stop the `AlarmService` and dismiss the alarm.

### 5. Boot Completion Handling
- **Implementation**: Register for the `android.intent.action.BOOT_COMPLETED` broadcast.
- **Responsibility**: When the device reboots, query the Room Database for all enabled alarms and re-schedule them with the `AlarmManager`, since Android clears all scheduled alarms upon reboot.
