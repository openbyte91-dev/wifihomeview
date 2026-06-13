# Security Policy

## Supported Versions

Security fixes are currently provided for the latest public version of WiFiHomeView.

## Reporting a Vulnerability

Please report security issues by opening a private security advisory on GitHub, if available for the repository. If private advisories are not available yet, open a GitHub issue with minimal detail and ask for a private contact path.

Do not include sensitive network data, real network names, MAC addresses, private IP inventories, credentials, or detailed proof-of-concept material in a public issue.

## In Scope

- Accidental transmission of local scan data outside the device
- Unsafe logging of device names, MAC addresses, SSIDs, or scan results
- Unnecessary exported Android components
- Permission misuse
- Insecure file sharing or export behavior
- Dependency vulnerabilities that affect the app

## Out of Scope

- Reports about networks you do not own or lack permission to inspect
- Social engineering
- Denial-of-service testing against third-party devices
- Issues requiring a rooted or otherwise compromised device unless the impact is clearly broader

## Response Expectations

This is a small open-source project. Maintainers will try to acknowledge valid reports within 7 days and provide a fix or mitigation plan when practical.
