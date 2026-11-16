# Privacy and Accessibility Disclosure: Cial.ai

Cial.ai is built around a simple rule: your words belong to you. This document states plainly what the app does, what it does not do, and where your data goes.

## What the Accessibility Service is used for

Cial.ai uses the Android Accessibility Service for three things, and nothing else:

1. **Detecting text field focus.** The service listens for focus and window events so it knows when you tap into an editable text field in another app. This is what makes the microphone bubble appear at the right moment.
2. **Showing the bubble and dictation panel.** These are drawn as accessibility overlay windows, which is why Cial.ai does not need the separate "draw over other apps" permission.
3. **Inserting approved text.** When you tap Insert, the service writes the text you reviewed into the focused field using the standard Android set-text action, appending it after any text already there.

## What the Accessibility Service is NOT used for

- It does **not** read, log or store the contents of your text fields, messages or notifications.
- It does **not** monitor what you type, which apps you use, or how long you use them.
- It does **not** record audio in the background. The microphone runs only after you tap the bubble and only while the dictation panel is open.
- It does **not** work around Android privacy controls, change system settings, or hide what it is doing.

## Audio and speech recognition

Dictation uses the speech recognition service built into your device (on most phones this is provided by Google). Your audio goes to that on-device system component, under that provider's own privacy policy. Cial.ai itself has no server and receives only the resulting transcript text from the recognizer.

## The AI polish step (optional, your key)

If you add an API key in Settings, your dictated transcript is sent over HTTPS to the AI provider **you** selected (OpenRouter, OpenAI or Google Gemini), using **your** key and **your** chosen model, for one purpose: rewriting the text. The response comes back to your device and is shown to you before anything is inserted.

- If you do not add a key, transcripts never leave the device through Cial.ai.
- Cial.ai has no backend, no analytics and no accounts. Nothing is sent to the developer.
- Your API key is stored only in the app's private storage on your device. Uninstalling the app removes it.

## Data collection summary

| Data | Collected by Cial.ai? | Where it goes |
| --- | --- | --- |
| Audio | No (handled by the device speech service) | Device speech service only |
| Transcripts | No storage, no sharing | Shown to you; sent to your chosen AI provider only if you added a key |
| API key | Stored on device only | Sent only to your chosen provider as an auth header |
| Usage or analytics | None | Nowhere |

## Your control

You can turn the assistant off at any time in Android Settings, under Accessibility, Installed apps, Cial.ai. Removing your API key in the app (clear the field) stops all network calls from Cial.ai. Uninstalling the app deletes its settings, including the key.

Questions about this disclosure: open an issue in this repository.
