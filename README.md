# GPS Tracking and Step Counter App

This Android application is designed to track the user's location using GPS and step count via a step counter sensor. It visualizes the trajectory on a map, displays real-time step count, calculates the area and perimeter of the traveled path, and stores the user's tracking data for future reference.

## Features:
- **Real-Time GPS Tracking**: Tracks the user's location and displays it on the map in real time.
- **Step Counter**: Uses the device's step counter sensor to track the number of steps.
- **Map Features**: Supports both satellite and normal map views, and the ability to zoom into the user's current location.
- **Path Drawing**: Draws the path on the map as the user moves.
- **Area and Perimeter Calculation**: Calculates the area and perimeter of the traveled path.
- **History Storage**: Saves tracking history in a local file for future reference.
- **Permissions Handling**: Requests necessary permissions from the user for location, step counter, and notifications.
- **Background Notification**: Sends a notification when the background service is active.
- **User Interface**: Provides a user-friendly interface with buttons to start/stop tracking, switch map layers, and view tracking statistics.

## Required Permissions:
- **Location Permissions**: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
- **Sensor Permissions**: ACTIVITY_RECOGNITION
- **Notification Permission**: POST_NOTIFICATIONS

## Setup and Requirements:
1. **Map API Key**: You need to provide an API key from Amap (Gaode Map) for location services.
2. **Device Compatibility**: The app requires a device with a built-in step counter sensor (Sensor.TYPE_STEP_COUNTER).

## How to Use:
1. **Start Tracking**: Tap the "Start" button to begin tracking your location and counting steps.
   - The app will ask for the necessary permissions if not already granted.
   - GPS location tracking starts, and the step counter begins counting.
2. **Stop Tracking**: Tap the "Stop" button to end tracking and calculate the area and perimeter of the path.
3. **Zoom to Current Location**: Tap the "Zoom" button to zoom in on your current location on the map.
4. **Switch Map Layers**: Tap the "Layer" button to toggle between the satellite and normal map views.
5. **Change Theme**: Tap the "Theme" button to switch between day and night modes.
6. **View Statistics**: Tap the "Stats" button to view the statistics of the tracked paths.
7. **Replay Path**: Tap the "Replay" button to replay your tracked path on the map.

## Notifications:
- The app will prompt for notification permissions to send background notifications regarding the tracking status.

## Saving and Viewing History:
- The app saves the area, perimeter, and step count of each tracked session to a local file (`history.json`).
- The saved history can be accessed for later reference or exported.

## Code Structure:
- **MainActivity**: The main activity handles UI interactions, permissions requests, map setup, sensor setup, and tracking logic.
- **TrackingService**: A background service that handles location tracking and step counting when the app is not in the foreground.
- **StatsActivity**: Displays tracking statistics such as total area, perimeter, and steps for previous sessions.

## Development Setup:
1. Clone or download the project.
2. Add your Amap API key in `MainActivity.java` or configure it through the app's settings.
3. Build the app in Android Studio and run it on a compatible Android device.

## Troubleshooting:
- If location services are not enabled, the app will prompt you to enable them.
- If the device does not support the step counter sensor, a warning will be shown.

## Known Issues:
- **Battery Optimizations**: Some devices may restrict the app's background activity due to battery optimization settings. You may need to manually disable battery optimizations for the app.

## License:
This project is open source and available for modification under the MIT License.

---

Feel free to ask any questions or report issues on the project's repository. Happy tracking!
