# WiFiHomeView

WiFiHomeView is an Android app that shows which devices are connected to your home Wi-Fi network.

It is built for regular home users who want a clearer view of their local network without ads, accounts, tracking, or cloud sync.

## Download

The latest pre-compiled version of the application can be found on our [GitHub Releases](https://github.com/openbyt91-dev/wifihomeview/releases) page. Download the `.apk` file to your Android device to install it.

## Features

- Discover devices connected to the same Wi-Fi network.
- Show IP addresses.
- Show hostnames when available.
- Show MAC and vendor details when available from local network data.
- Rename devices with custom names stored on the device.
- Mark newly seen devices.
- Refresh scan results.
- Run optional background checks for newly seen devices.
- Store known devices locally.

## Screenshots

Screenshots are not included yet. Before publishing screenshots, remove real SSIDs, device names, MAC addresses, router names, and any other private network details.

Suggested screenshots:

- Device list
- Scan progress
- Device details
- Rename dialog
- Empty or permission-denied state

## Privacy

WiFiHomeView is designed around local-only network visibility.

- No ads
- No tracking
- No account required
- No cloud sync
- No analytics SDK by default
- Scan results stay on the device

See [PRIVACY.md](PRIVACY.md) for details.

## Permissions

WiFiHomeView requests Android permissions needed for local network discovery, Wi-Fi state, multicast discovery, notifications, and Android-version-specific Wi-Fi context access.

See [docs/permissions.md](docs/permissions.md) for a plain-language permission explanation.

## Limitations

- WiFiHomeView only scans the current local network.
- Some devices hide hostnames or vendor details.
- Device visibility varies by router, Android version, firewall settings, and network isolation settings.
- WiFiHomeView is not a router admin tool.
- WiFiHomeView cannot remove devices from your network; use your router settings for that.

## Responsible Use

Use this app only on networks you own or are authorized to inspect.

## Build

Requirements:

- Android Studio Ladybug or newer
- JDK 17
- Android SDK with compile SDK 35
- A physical Android device for realistic Wi-Fi discovery testing

Clone and build:

```bash
git clone https://github.com/openbyt91-dev/wifihomeview.git
cd wifihomeview
./gradlew assembleDebug
```

On Windows:

```bat
git clone https://github.com/openbyt91-dev/wifihomeview.git
cd wifihomeview
gradlew.bat assembleDebug
```

Run tests:

```bash
./gradlew testDebugUnitTest
```

## Project Structure

```text
app/                  Android application
docs/                 Project documentation
gradle/               Gradle wrapper and version catalog files
```

## Documentation

- [Privacy](PRIVACY.md)
- [Security](SECURITY.md)
- [Contributing](CONTRIBUTING.md)
- [Permissions](docs/permissions.md)
- [Architecture](docs/architecture.md)
- [Open-source release plan](docs/wifihomeview-opensource-release-plan.md)

## License

WiFiHomeView is released under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

## Disclaimer

WiFiHomeView is intended strictly for personal home network visibility and diagnostic purposes on networks you own or have explicit permission to audit. The authors assume no liability for any unauthorized use, network disruption, or data collection. The software is provided "as is" under the GPLv3 License.
