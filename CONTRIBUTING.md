# Contributing

Thanks for helping improve WiFiHomeView.

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio Ladybug or newer.
3. Use a physical Android device for real Wi-Fi discovery testing.
4. Run the unit tests before submitting changes.

```bash
./gradlew testDebugUnitTest
```

On Windows, use:

```bat
gradlew.bat testDebugUnitTest
```

## Development Guidelines

- Keep device discovery local to the user's current network.
- Avoid fear-based language when describing unfamiliar devices.
- Explain permission changes clearly.
- Do not add analytics, ads, or remote services without discussion and documentation.
- Keep UI copy understandable for non-technical home users.

## Pull Requests

Please include:

- A short description of the change.
- Any permissions, privacy, or network-behavior impact.
- Test coverage or manual verification notes.
- Screenshots for UI changes, with private device names and network names removed.

## Good First Areas

- Device naming improvements
- Vendor lookup improvements
- Scan progress UI
- Accessibility
- Localization
- Router compatibility
