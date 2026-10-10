# Rollback — v1.0.0-beta.1

## Source
```bash
git checkout feature/apollo-v5-3-auto-shaders   # or 143a2ef / 0b8dac9
# or historical: apollo-pre-v5-1 → 3003d61
```

## APK
Uninstall release package and reinstall prior debug/release:
```bash
adb uninstall --user 0 com.apolloauto.player
adb uninstall --user 0 com.apolloauto.player.debug
adb install --user 0 ApolloAuto-v5.3.0-cyber-debug.apk
```

Prior GitHub release: https://github.com/H34THEN/ApolloAuto/releases/tag/v5.3.0
