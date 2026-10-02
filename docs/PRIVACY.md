# Privacy & Data Disclosure — DevCode Editor (KONKRED Dev)

> **STATUS: DRAFT — requires owner/legal review and approval before publication (audit task T-013, finding F-LEGAL-002).**
> Do not link this document from a store listing until approved.

## 1. What the app stores on your device

DevCode Editor keeps your work **locally on your device** in a private Room (SQLite) database (`devcode_editor_db`):

| Data | Purpose | Leaves the device? |
|---|---|---|
| Virtual project files (code you type/create in the editor) | Core editor functionality | Only if you use AI pairing (see §2) |
| SSH connection profiles you create (name, host, port, username) | SSH panel (SIMULATED — see §4) | No. No connection is ever made |
| Extension marketplace entries | UI demo data | No |
| Preferences (theme, key bindings) via SharedPreferences | Settings persistence | No |

The app requires no account and collects no analytics. There is no advertising SDK.

## 2. AI pair programming (the only network data flow)

When — and only when — you explicitly tap **Send** in the Gemini AI panel, **Fix My Code**, or run the terminal `gemini` command, the app sends to Google's Gemini API (`generativelanguage.googleapis.com`, HTTPS, key via `x-goog-api-key` header):

- the text of your prompt, and
- **the full content of the currently open file**, embedded as context (`CodeEditorViewModel.askGeminiPairProgrammer`), plus a system instruction naming the file.

Google processes that request under the [Gemini API Terms of Service](https://ai.google.dev/gemini-api/terms) and its data-use policies for API consumers. **Do not open files containing secrets or personal data in the active editor when using AI features.** The API key is supplied by whoever built your copy of the app (`.env` → BuildConfig); on a distributed build, treat that key as potentially extractable (see README "Release build (signing)").

No other feature of the app transmits your code anywhere.

## 3. Android backups

The app currently ships with `android:allowBackup="true"` and stock (unconfigured) data-extraction rules, which means Android may include the local database (your files, SSH profiles) in device backups. Until the maintainers finalize a backup policy (audit finding F-SEC-005), be aware that your locally stored code can be copied into your Google account backup. To avoid this, disable backup for this app in Android system settings.

## 4. Simulated features

The **Terminal, SSH, Git, Extensions, and Cloud Sync** panels are **simulations**. They execute no shell, open no SSH socket, run no git binary, install nothing, and upload nothing — the "sync" progress and "connection established" messages are local UI demos (labeled `[SIMULATED]` in-app). No data leaves your device through these panels.

## 5. Data deletion

- Delete individual files inside the app (Explorer → trash icon).
- Delete **all** app data at any time via Android Settings → Apps → DevCode Editor → Storage → Clear data. This irreversibly removes the local database.
- The app holds no server-side copies, so there is nothing further to request deletion of — except for Gemini API request logs, which are governed by Google's retention policies (§2).

## 6. Children

The app has no age gate and no account system. Because AI requests send open-file content to a third party, it is not directed at children under 13 (COPPA) or equivalent regimes.

## 7. Contact & changes

Maintainer: `reARbitRA` (GitHub). This disclosure changes whenever data flows change; check the repository `docs/PRIVACY.md` history.
