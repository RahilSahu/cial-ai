# Contributing to Cial.ai

Thanks for your interest in improving Cial.ai. This is a small, deliberately lean Kotlin Android project, and contributions that keep it that way are very welcome.

## Ground rules

- **One module, few dependencies.** The app uses AndroidX core/appcompat, OkHttp and Kotlin coroutines. Please do not add a new runtime dependency without opening an issue to discuss it first.
- **Privacy is a feature.** Anything that reads user text, audio or settings beyond what PRIVACY.md describes will not be merged. If a change touches the accessibility service, the speech flow or the AI call, explain the data flow in your pull request.
- **Honest claims.** README and in-app text must only promise what the code actually does. If a capability needs an API key or a device feature, the text must say so.

## Getting set up

You need JDK 17 and the Android SDK (platform 35).

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:assembleDebug       # build the debug APK
```

## Making a change

1. Fork the repo and create a branch for your change.
2. Keep the change focused. Separate unrelated cleanups into their own pull requests.
3. Put pure logic (prompts, text cleanup, validation) in classes with no Android dependencies, and cover it with JVM unit tests in `app/src/test`.
4. Run the unit tests and the debug build before opening the pull request. CI runs both on every push and pull request.
5. In the pull request description, say what changed, how you tested it, and any effect on privacy or permissions.

## Style

- Kotlin, official code style (enforced loosely; match the surrounding code).
- User-facing strings live in `strings.xml`, never hard-coded in layouts or Kotlin.
- Comments explain why, not what, and stay short.

## Reporting bugs

Open an issue with your device model, Android version, what you expected, and what happened. For speech problems, note which input language you used and whether the issue also happens in other dictation apps (that usually means the device speech service, not Cial.ai).
