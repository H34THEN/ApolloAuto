# Apollo V5.3 Rollback

## Source

```bash
# Leave V5.3 branch without merging
git checkout fix/apollo-v5-2-visualizer-reactivity   # or master
# Hard discard of local V5.3 only if intentional:
# git branch -D feature/apollo-v5-3-auto-shaders
```

Baseline tip before V5.3 work: `2d64eb0` (v5.2.0-cyber).

## Installed APK

```bash
adb uninstall --user 0 com.apolloauto.player.debug
adb install --user 0 ApolloAuto-v5.2.0-cyber-debug.apk   # last known good
```

Or download from https://github.com/H34THEN/ApolloAuto/releases/tag/v5.2.0

## Notes

- Do not force-reset shared branches.
- V5.3 adds modes only; prefs auto-enable newlyAdded enum values via ThemeStore.
