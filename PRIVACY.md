# Privacy Policy

WiFiHomeView is designed to work locally on your device and your current Wi-Fi network.

## Summary

- No ads.
- No user tracking.
- No account required.
- No cloud sync.
- No analytics SDK is included by default.
- Device discovery happens on the local network.

## Data Processed Locally

WiFiHomeView may process local network information such as:

- IP address
- Hostname, when available
- MAC address, when available from local network data
- Device vendor, when available from the bundled local OUI database
- SSID or network name, when available and permitted by Android
- User-provided custom device names

This data is used to show devices connected to the same Wi-Fi network and to remember known devices locally.

## Data Leaving the Device

WiFiHomeView does not send scan results, device names, IP addresses, MAC addresses, SSIDs, or custom names to a server.

The app may make HTTP requests to IP addresses on your local network during device identification. These requests are intended to inspect devices on the same network, not to contact a cloud service.

## Storage

WiFiHomeView stores known device information locally using Android app storage. Android cloud backup is disabled for the app so scan history and custom device names are not backed up by WiFiHomeView.

## Third-Party Services

WiFiHomeView does not include advertising, analytics, or crash-reporting services by default.

## Permissions

See [docs/permissions.md](docs/permissions.md) for a plain-language explanation of Android permissions used by the app.

## Responsible Use

Use WiFiHomeView only on networks you own or are authorized to inspect.
