# Testing Guide for WiFiHomeView

This document outlines how to verify the functionality of the WiFiHomeView app, both automatically (Unit Tests) and manually (Integration Testing).

## 1. Unit Tests (Logic Verification)
These verify the core business logic (Device naming, IP calculation) works correctly without needing a physical device.

### Running Tests
1.  Open **Android Studio**.
2.  Open the **Project View** (Left Pane).
3.  Navigate to `app/src/test/java/com/openbyt91dev/wifihomeview/`.
4.  Right-click the folder and select **Run 'Tests in 'wifihomeview''**.

### What gets tested?
-   **`DeviceTest.kt`**: Ensures "Friendly Names" are prioritized correctly (Custom > mDNS > UPnP > ...).
-   **`NetworkUtilsTest.kt`**: Verifies that given an IP/Subnet (e.g., `192.168.1.5/24`), it generates the correct list of 254 IPs to scan.

## 2. Manual Integration Testing (Physical Device Required)
Since network scanning relies on real Wi-Fi hardware, you must test on a physical Android phone.

### Prerequisites
-   A physical Android device (Android 10+ recommended).
-   Connected to a Wi-Fi network with other devices (e.g., your home network).

### Test Case 1: Active Scan
1.  Launch the App.
2.  Grant **Location Permission** (Required for Wi-Fi SSID/BSSID access).
3.  Tap **"Scan"**.
4.  **Verify:**
    -   [ ] A list of devices appears.
    -   [ ] Your own phone appears in the list.
    -   [ ] Devices have names like "Living Room TV" (if supported via mDNS/UPnP).

### Test Case 2: "New Device" Detection
1.  Scan the network once. All devices will have a **RED "NEW" BADGE**.
2.  Tap the **Checkmark** on one device to mark it as known.
3.  **Verify:** The red badge disappears.
4.  **Verify:** Rename a device (tap Edit). The badge should disappear and the new name should persist.
5.  Close the app (swipe away) and reopen.
6.  **Verify:** The changes persist.

### Test Case 3: Background Notifications
*Note: Background work runs every ~15 minutes minimum.*

1.  Connect a *new* device to your Wi-Fi (e.g., turn on a game console).
2.  Wait ~20 minutes (or trigger manually via App Inspection).
3.  **Verify:** You receive a system notification: "New Device Detected: [Device Name]".

### Debugging Tips
-   **No Devices Found?** Check if you are on a "Guest Network" (Client Isolation enabled).
-   **Slow Scan?** The parallel scanner processes 50 IPs at a time. A full /24 scan takes ~10-20 seconds.
-   **Missing Names?** Some routers block mDNS/multicast. Try a different network.
