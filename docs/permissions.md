# Android Permissions

WiFiHomeView requests permissions only for local network visibility and optional notifications.

| Permission | Why it is needed |
|---|---|
| `INTERNET` | Allows the app to communicate with devices on the local network during discovery and identification. |
| `ACCESS_NETWORK_STATE` | Lets the app detect whether the device is connected to a network. |
| `ACCESS_WIFI_STATE` | Lets the app read Wi-Fi connection state needed for local network context. |
| `CHANGE_WIFI_MULTICAST_STATE` | Allows multicast discovery used by protocols such as mDNS and SSDP on the local network. |
| `ACCESS_FINE_LOCATION` | Android requires location permission for some Wi-Fi network details such as SSID/BSSID on certain versions. WiFiHomeView does not use GPS tracking. |
| `ACCESS_COARSE_LOCATION` | Required by some Android versions for Wi-Fi-related network information. |
| `POST_NOTIFICATIONS` | Allows optional alerts when a newly seen device appears during background checks on Android 13 and newer. |

## Permission Denial

The app should degrade gracefully when a permission is denied. Some discovery details may be missing, but the app should avoid crashing or showing misleading results.

## Privacy Notes

Permissions are used for local network discovery. WiFiHomeView does not send local scan results to a cloud service.
