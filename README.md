# Cial.ai

**Chat in any language.**

Cial.ai is an Android voice assistant that turns your speech into clean, ready to send text. Tap into any text field in any app, and a small microphone bubble appears. Speak in Hindi, Tamil, Telugu, Malayalam, Kannada or English. Cial.ai transcribes your voice on the device, optionally rewrites it into polished text with an AI provider using your own API key, and inserts it into the field with one tap. No more fighting with English typing.

## How it works

1. **Tap a text field** in WhatsApp, Gmail, LinkedIn, or any other app. The Cial.ai bubble appears.
2. **Tap the bubble and speak** in your language. Watch the live transcript, then tap Done.
3. **Review and insert.** Check the polished text, switch tone or output if you like, edit if needed, and tap Insert. The text lands in the field, ready to send.

## Install

Cial.ai is not on the Play Store yet. Install it from the APK attached to the [latest release](https://github.com/RahilSahu/cial-ai/releases):

1. Download `cial-ai-v1.0.0.apk` on your Android phone (Android 8.0 or newer).
2. Open the file and allow installs from your browser or file manager when Android asks.
3. Open Cial.ai and follow the three setup steps in the app.

Note: the release APK is a debug-signed sideload build. It is meant for testing and personal use, not for distribution.

## Enable the assistant (Accessibility)

Android requires you to switch accessibility services on yourself:

1. Open Cial.ai and tap **Open Accessibility Settings** in Step 1.
2. Go to **Installed apps** (or "Installed services") and pick **Cial.ai**.
3. Turn it on and confirm the Android permission dialog.
4. Back in Cial.ai, allow the microphone in Step 2.

The bubble only appears while a text field is focused. It never appears inside Cial.ai itself or on your home screen.

## Bring your own AI key (optional)

Without a key, Cial.ai is a plain dictation tool: it inserts your words exactly as transcribed. With your own API key, it rewrites your dictation into clean, ready to send text before you insert it.

Supported providers (any OpenAI-compatible chat completions endpoint works the same way):

| Provider | Default model |
| --- | --- |
| OpenRouter | `openai/gpt-4o-mini` |
| OpenAI | `gpt-4o-mini` |
| Google Gemini | `gemini-2.0-flash` |

The provider, key and model are all editable in Step 3 of the app, and there is a **Test connection** button so you can verify your key before relying on it. The key is stored only in the app's private storage on your device and is sent only to the provider you picked, over HTTPS. Polish quality depends on the model you choose.

## Languages and output

Speak in: **Hindi, Tamil, Telugu, Malayalam, Kannada, English** (Indian locale variants).

Write in three output modes, each with Neutral, Casual or Formal tone:

- **English**: polished, natural, professional English. Not a word for word translation.
- **Hindi**: proper Hindi in Devanagari script.
- **Hinglish**: Hindi in Roman script, casual chat style.

Quick-switch chips in the review panel re-run the polish instantly with a different mode or tone.

## Accessibility disclosure (summary)

Cial.ai uses the Android Accessibility Service for exactly three things: detecting when a text field is focused (so the bubble can appear), showing the bubble and dictation panel as an accessibility overlay, and inserting text you reviewed and approved into the focused field. It does not read your conversations, does not record in the background, and does not collect, store or share your text or audio. The full disclosure is in [PRIVACY.md](PRIVACY.md), and the same text is shown inside the app.

## Honest limits

- **Sideload build**: the APK is debug-signed and is not distributed through the Play Store.
- **Speech recognition** uses the speech services built into your device (usually Google's). It needs those services present and, on many devices, a network connection. Recognition quality and language support vary by device.
- **Polish needs your own API key** and a network connection. Without a key, or if the provider call fails, you always get your raw transcript and can still insert it.
- **Insertion** uses the standard Android set-text action. A few apps use custom text fields where that action fails; in that case Cial.ai copies the text to your clipboard so you can paste it.
- The bubble follows text field focus. Some apps draw custom keyboards or fields that Android does not expose to accessibility services; the bubble may not appear there.

## Build from source

Requirements: JDK 17 and the Android SDK (platform 35).

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:assembleDebug       # debug APK in app/build/outputs/apk/debug/
```

The stack is deliberately lean: Kotlin, a single app module, AndroidX core and appcompat, OkHttp for the AI call, and Kotlin coroutines. Speech, overlay and insertion are all built on stock Android APIs.

## License

MIT, see [LICENSE](LICENSE). Contributions are welcome, see [CONTRIBUTING.md](CONTRIBUTING.md). For security questions, see [SECURITY.md](SECURITY.md).
