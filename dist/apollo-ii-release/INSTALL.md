# Install — Apollo II v1.0.0-beta.1

## Package
- File: `Apollo-II-v1.0.0-beta.1.apk`
- Application ID: `com.apolloauto.player`
- versionCode: `100`
- versionName: `1.0.0-beta.1`

## Steps
1. Verify checksum: `shasum -a 256 -c SHA256SUMS.txt`
2. Install on **Owner** Android user (not Work profile).
3. If you previously installed `com.apolloauto.player.debug`, uninstall it first (different signing + suffix).
4. Open Android Auto → enable Apollo in Customize launcher.
5. For sideloads: AA Developer settings → Unknown sources.

```bash
adb install --user 0 -r Apollo-II-v1.0.0-beta.1.apk
```
